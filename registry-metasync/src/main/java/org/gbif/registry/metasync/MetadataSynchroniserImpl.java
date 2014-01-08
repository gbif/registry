package org.gbif.registry.metasync;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.registry.metasync.api.ErrorCode;
import org.gbif.registry.metasync.api.MetadataException;
import org.gbif.registry.metasync.api.MetadataProtocolHandler;
import org.gbif.registry.metasync.api.MetadataSynchroniser;
import org.gbif.registry.metasync.api.SyncResult;

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

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A concrete implementation of the {@link MetadataSynchroniser} interface. It delegates the actual interaction with
 * Installations and Endpoints to {@link MetadataProtocolHandler}s which have to be registered on this class using
 * {@link #registerProtocolHandler(MetadataProtocolHandler)}.
 */
public class MetadataSynchroniserImpl implements MetadataSynchroniser {

  private static final Logger LOG = LoggerFactory.getLogger(MetadataSynchroniserImpl.class);
  private static final int PAGING_LIMIT = 10000;
  private final InstallationService installationService;
  private final List<MetadataProtocolHandler> protocolHandlers = Lists.newArrayList();

  @Inject
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

    throw new IllegalArgumentException("Installation of type [" + installation.getType() + "] not supported");
  }

  @Override
  public List<SyncResult> synchroniseAllInstallations() {
    return synchroniseAllInstallations(1);
  }

  @Override
  public List<SyncResult> synchroniseAllInstallations(int parallel) {
    checkArgument(parallel > 0, "parallel has to be greater than 0");
    ExecutorService executor = Executors.newFixedThreadPool(parallel);
    CompletionService<SyncResult> completionService = new ExecutorCompletionService<SyncResult>(executor);

    // Submit all sync jobs to the Completion service
    PagingResponse<Installation> results;
    PagingRequest page = new PagingRequest();
    int count = 0;
    do {
      results = installationService.list(page);
      // TODO:Check for null result
      for (final Installation installation : results.getResults()) {
        completionService.submit(new Callable<SyncResult>() {

          @Override
          public SyncResult call() throws Exception {
            try {
              return synchroniseInstallation(installation.getKey());
            } catch (IllegalArgumentException e) {
              return new SyncResult(installation, new MetadataException(e, ErrorCode.OTHER_ERROR));
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
   * This method actually runs the metadata synchronisation by calling out to the protocol handler and then validating
   * and processing its result.
   */
  private SyncResult doSynchroniseInstallation(
    Installation installation, List<Dataset> hostedDatasets, MetadataProtocolHandler protocolHandler
    ) {
    LOG.info("Syncing Installation [{}] of type [{}]", installation.getKey(), installation.getType());
    try {
      return protocolHandler.syncInstallation(installation, hostedDatasets);
    } catch (MetadataException e) {
      return new SyncResult(installation, e);
    }
  }

  /**
   * Does some checks whether we can synchronise this Installation or not. They are not exhaustive as some things can
   * only be determined by the protocol handlers.
   */
  private Installation validateInstallation(UUID key) {
    Installation installation = installationService.get(key);

    if (installation == null) {
      throw new IllegalArgumentException("Installation with key [" + key + "] does not exist");
    }

    if (installation.getDeleted() != null) {
      throw new IllegalArgumentException("Installation with key [" + key +"] is deleted");
    }

    if (installation.getEndpoints() == null || installation.getEndpoints().isEmpty()) {
      throw new IllegalArgumentException("Installation with key [" + key + "]" + " has no endpoints");
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
    PagingResponse<Dataset> results;
    List<Dataset> hostedDatasets = Lists.newArrayList();
    do {
      results = installationService.getHostedDatasets(key, page);
      hostedDatasets.addAll(results.getResults());
      page.nextPage();
    } while (!results.isEndOfRecords());
    return hostedDatasets;
  }

}
