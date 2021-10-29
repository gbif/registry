/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.metasync;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.registry.metasync.api.ErrorCode;
import org.gbif.registry.metasync.api.MetadataException;
import org.gbif.registry.metasync.api.MetadataProtocolHandler;
import org.gbif.registry.metasync.api.MetadataSynchroniser;
import org.gbif.registry.metasync.api.SyncResult;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A concrete implementation of the {@link MetadataSynchroniser} interface. It delegates the actual
 * interaction with Installations and Endpoints to {@link MetadataProtocolHandler}s which have to be
 * registered on this class using {@link #registerProtocolHandler(MetadataProtocolHandler)}.
 */
public class MetadataSynchroniserImpl implements MetadataSynchroniser {

  private static final Logger LOG = LoggerFactory.getLogger(MetadataSynchroniserImpl.class);
  private static final int PAGING_LIMIT = 10000;
  private final InstallationService installationService;
  private final List<MetadataProtocolHandler> protocolHandlers = new ArrayList<>();

  public MetadataSynchroniserImpl(InstallationService installationService) {
    this.installationService = installationService;
  }

  @Override
  public SyncResult synchroniseInstallation(UUID key) {
    checkNotNull(key, "key can't be null");

    Installation installation = validateInstallation(key);
    List<Dataset> hostedDatasets = getHostedDatasets(key);

    for (MetadataProtocolHandler protocolHandler : protocolHandlers) {
      if (protocolHandler.canHandle(installation)) {
        return doSynchroniseInstallation(installation, hostedDatasets, protocolHandler);
      }
    }

    throw new IllegalArgumentException(
        "Installation of type [" + installation.getType() + "] not supported");
  }

  @Override
  public Long getDatasetCount(Dataset dataset, Endpoint endpoint) throws MetadataException {
    checkNotNull(dataset, "dataset can't be null");
    checkNotNull(endpoint, "endpoint can't be null");

    Installation installation = installationService.get(dataset.getInstallationKey());

    for (MetadataProtocolHandler protocolHandler : protocolHandlers) {
      if (protocolHandler.canHandle(installation)) {
        return protocolHandler.getDatasetCount(dataset, endpoint);
      }
    }

    return null;
  }

  @Override
  public List<SyncResult> synchroniseAllInstallations() {
    return synchroniseAllInstallations(1);
  }

  @Override
  public List<SyncResult> synchroniseAllInstallations(int parallel) {
    checkArgument(parallel > 0, "parallel has to be greater than 0");
    ExecutorService executor = Executors.newFixedThreadPool(parallel);
    CompletionService<SyncResult> completionService =
        new ExecutorCompletionService<SyncResult>(executor);

    // Submit all sync jobs to the Completion service
    PagingResponse<Installation> results;
    PagingRequest page = new PagingRequest();
    int count = 0;
    do {
      results = installationService.list(page);

      // skip installations that don't serve any datasets!
      Iterator<Installation> iter = results.getResults().iterator();
      while (iter.hasNext()) {
        Installation i = iter.next();
        if (!i.isDisabled() && i.getDeleted() != null) {
          PagingResponse<Dataset> datasets =
              installationService.getHostedDatasets(i.getKey(), new PagingRequest(0, 1));
          if (datasets.getResults().isEmpty()) {
            LOG.warn("Excluding installation [key={}] because it serves 0 datasets!", i.getKey());
            iter.remove();
          }
        } else {
          LOG.warn("Excluding disabled/deleted installation [key={}]!", i.getKey());
          iter.remove();
        }
      }

      for (final Installation installation : results.getResults()) {
        completionService.submit(
            new Callable<SyncResult>() {

              @Override
              public SyncResult call() throws Exception {
                try {
                  return synchroniseInstallation(installation.getKey());
                } catch (Exception e) {
                  return new SyncResult(
                      installation, new MetadataException(e, ErrorCode.OTHER_ERROR));
                }
              }
            });
        count++;
      }
      page.nextPage();
    } while (!results.isEndOfRecords());

    // Wait for all to finish and collect results
    List<SyncResult> syncResults = Lists.newArrayList();
    for (int i = 0; i < count; i++) {
      try {
        Future<SyncResult> future = completionService.take();
        syncResults.add(future.get());
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        Throwables.propagateIfPossible(e.getCause());
      }
    }

    executor.shutdown();
    while (!executor.isTerminated()) {
      LOG.info("Waiting for synchronisations to finish");
      try {
        executor.awaitTermination(1, TimeUnit.MINUTES);
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      }
    }

    return syncResults;
  }

  public void registerProtocolHandler(MetadataProtocolHandler handler) {
    protocolHandlers.add(handler);
  }

  /**
   * This method actually runs the metadata synchronisation by calling out to the protocol handler
   * and then validating and processing its result.
   */
  private SyncResult doSynchroniseInstallation(
      Installation installation,
      List<Dataset> hostedDatasets,
      MetadataProtocolHandler protocolHandler) {
    LOG.info(
        "Syncing Installation [{}] of type [{}]", installation.getKey(), installation.getType());
    try {
      return protocolHandler.syncInstallation(installation, hostedDatasets);
    } catch (MetadataException e) {
      return new SyncResult(installation, e);
    }
  }

  /**
   * Does some checks whether we can synchronise this Installation or not. They are not exhaustive
   * as some things can only be determined by the protocol handlers.
   */
  private Installation validateInstallation(UUID key) {
    Installation installation = installationService.get(key);

    if (installation == null) {
      throw new IllegalArgumentException("Installation with key [" + key + "] does not exist");
    }

    if (installation.getDeleted() != null) {
      throw new IllegalArgumentException("Installation with key [" + key + "] is deleted");
    }

    if (installation.getEndpoints() == null || installation.getEndpoints().isEmpty()) {
      throw new IllegalArgumentException(
          "Installation with key [" + key + "]" + " has no endpoints");
    }
    return installation;
  }

  /**
   * Gets all hosted datasets for an Installation.
   *
   * @param key of the Installation
   * @return list of Datasets for this Installation, might be empty but never null
   */
  private List<Dataset> getHostedDatasets(UUID key) {
    PagingRequest page = new PagingRequest(0, PAGING_LIMIT);
    PagingResponse<Dataset> results = null;
    List<Dataset> hostedDatasets = Lists.newArrayList();
    do {
      try {
        results = installationService.getHostedDatasets(key, page);
        hostedDatasets.addAll(results.getResults());
        page.nextPage();
      } catch (Exception e) {
        LOG.error(
            "Error getting hosted datasets for installation {}, offset {} limit {}",
            key,
            page.getOffset(),
            page.getLimit());
      }
    } while (results != null && !results.isEndOfRecords());
    return hostedDatasets;
  }
}
