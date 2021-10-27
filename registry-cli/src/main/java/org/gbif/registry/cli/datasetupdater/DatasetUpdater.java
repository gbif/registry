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
package org.gbif.registry.cli.datasetupdater;

import org.gbif.api.model.registry.Dataset;
import org.gbif.registry.cli.common.spring.SpringContextBuilder;
import org.gbif.registry.cli.common.stubs.DataCiteMetadataBuilderServiceStub;
import org.gbif.registry.cli.common.stubs.DatasetDoiDataCiteHandlingServiceStub;
import org.gbif.registry.cli.common.stubs.DoiIssuingServiceStub;
import org.gbif.registry.cli.common.stubs.EditorAuthorizationServiceStub;
import org.gbif.registry.cli.common.stubs.EventManagerStub;
import org.gbif.registry.cli.common.stubs.SearchServiceStub;
import org.gbif.registry.service.RegistryDatasetServiceImpl;
import org.gbif.registry.service.WithMyBatis;
import org.gbif.registry.ws.resources.DatasetResource;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.exceptions.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * A utility that will update either a single dataset or a list of datasets by reinterpreting their
 * preferred metadata document stored in the registry.
 */
public class DatasetUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetUpdater.class);

  private ApplicationContext context;
  private int updateCounter;
  private DatasetResource datasetResource;

  public static DatasetUpdater build(DatasetUpdaterConfiguration cfg) {
    return new DatasetUpdater(cfg);
  }

  private DatasetUpdater(DatasetUpdaterConfiguration cfg) {
    LOG.info(
        "Connecting to registry {}.{} as user {}",
        cfg.db.serverName,
        cfg.db.databaseName,
        cfg.db.user);
    this.context = prepareContext(cfg);
    this.datasetResource = context.getBean(DatasetResource.class);
  }

  private AnnotationConfigApplicationContext prepareContext(DatasetUpdaterConfiguration cfg) {
    return SpringContextBuilder.create()
        .withDbConfiguration(cfg.db)
        .withComponents(
            SearchServiceStub.class,
            DatasetDoiDataCiteHandlingServiceStub.class,
            DoiIssuingServiceStub.class,
            DataCiteMetadataBuilderServiceStub.class,
            EditorAuthorizationServiceStub.class,
            EventManagerStub.class,
            RegistryDatasetServiceImpl.class,
            DatasetResource.class,
            WithMyBatis.class)
        .build();
  }

  /**
   * Iterates through list of keys of datasets, updating each one from its preferred metadata
   * document.
   *
   * @param keys list of keys of datasets to update
   */
  public void update(List<UUID> keys) {
    for (UUID key : keys) {
      update(key);
    }
  }

  /**
   * Update dataset from its preferred metadata document. Deleted or locked datasets are not
   * updated.
   *
   * @param key key of dataset to update
   */
  public void update(UUID key) {
    Dataset dataset = datasetResource.get(key);
    if (dataset == null) {
      LOG.error("Dataset [key={}] not existing!", key);
    } else if (dataset.getDeleted() != null) {
      LOG.error("Dataset [key={}] has been deleted!", key);
    } else if (dataset.isLockedForAutoUpdate()) {
      LOG.error("Dataset [key={}] has been locked!", key);
    } else {
      try {
        datasetResource.updateFromPreferredMetadata(key, "dataset-updater cli");
        LOG.info("Updated dataset [key={}]!", key);
        updateCounter++;
      } catch (PersistenceException e) {
        LOG.error("Persistence exception occurred trying to update dataset [key={}]: {}", key, e);
      }
    }
  }

  /** @return the number of datasets updated */
  public int getUpdateCounter() {
    return updateCounter;
  }

  public DatasetResource getDatasetResource() {
    return datasetResource;
  }

  public ApplicationContext getContext() {
    return context;
  }
}
