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
package org.gbif.registry.metasync.protocols.dwca;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Metadata;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.vocabulary.InstallationType;
import org.gbif.registry.metasync.api.MetadataException;
import org.gbif.registry.metasync.api.MetadataProtocolHandler;
import org.gbif.registry.metasync.api.SyncResult;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Moved to test package since this should not be used in production, EML are synchronised using
 * another path. It can be useful to run locally to test some changes though.
 *
 * <p>IPT and HTTP synchronisation starts by iterating through all GBIF {@link Dataset} served by
 * the installation. Each GBIF {@link Dataset} is then augmented by its preferred metadata document,
 * typically an EML document.
 */
public class EmlMetadataSynchroniser implements MetadataProtocolHandler {

  private static final Logger LOG = LoggerFactory.getLogger(EmlMetadataSynchroniser.class);
  private final DatasetService datasetService;

  public EmlMetadataSynchroniser(DatasetService datasetService) {
    this.datasetService = datasetService;
  }

  @Override
  public boolean canHandle(Installation installation) {
    return installation.getType() == InstallationType.IPT_INSTALLATION
        || installation.getType() == InstallationType.HTTP_INSTALLATION;
  }

  @Nullable
  @Override
  public SyncResult syncInstallation(Installation installation, List<Dataset> datasets)
      throws MetadataException {
    checkArgument(
        installation.getType() == InstallationType.IPT_INSTALLATION
            || installation.getType() == InstallationType.HTTP_INSTALLATION,
        "Only supports IPT or HTTP Installations");

    int count = augmentDatasets(datasets);
    LOG.info(
        "Updated {} out of {} datasets hosted by installation {}",
        count,
        datasets.size(),
        installation.getKey());

    // return empty SyncResult. Datasets are updated in the database already when augmented
    List<Dataset> added = Lists.newArrayList();
    List<Dataset> deleted = Lists.newArrayList();
    Map<Dataset, Dataset> updated = Maps.newHashMap();
    return new SyncResult(updated, added, deleted, installation);
  }

  @Override
  public Long getDatasetCount(Dataset dataset, Endpoint endpoint) {
    LOG.info("Not implemented, returning null");
    return null;
  }

  /**
   * Augments the Datasets hosted by this Installation with their preferred metadata document stored
   * in the Registry. Datasets are updated in the database when augmented. </br> NOTE: code
   * commented out depends on new method in DatasetService, currently only implemented locally
   */
  private int augmentDatasets(Iterable<Dataset> datasets) {
    int count = 0;
    for (Dataset dataset : datasets) {
      List<Metadata> docs = datasetService.listMetadata(dataset.getKey(), null);
      if (!docs.isEmpty()) {
        try {
          // InputStream preferred = datasetService.getMetadataDocument(docs.get(0).getKey());
          // Metadata inserted = datasetService.insertMetadata(dataset.getKey(), preferred, true);
          // LOG.info("Updated dataset {} from preferred metadata document of type {}",
          // dataset.getKey(), inserted.getType());
          count++;
        } catch (Exception e) {
          LOG.error("Error augmenting dataset {}: {}", dataset.getKey(), e);
        }
      }
    }
    return count;
  }
}
