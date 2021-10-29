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
package org.gbif.registry.metasync.resulthandler;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.registry.metasync.api.SyncResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple utility class that prints some information about each synchronisation result. Does not
 * generate aggregate statistics at the moment.
 */
public final class DebugHandler {

  private static final Logger LOG = LoggerFactory.getLogger(DebugHandler.class);

  public static void processResults(Iterable<SyncResult> results) {
    for (SyncResult result : results) {
      processResult(result);
    }
  }

  public static void processResult(SyncResult result) {
    if (result == null) {
      LOG.warn("Null SyncResult!");
    } else if (result.exception == null) {
      LOG.info(
          "Installation [{}] synced successfully. [{}] added, [{}] deleted, [{}] updated",
          result.installation.getKey(),
          result.addedDatasets.size(),
          result.deletedDatasets.size(),
          result.existingDatasets.size());

      // Added datasets
      for (Dataset dataset : result.addedDatasets) {
        dataset.setPublishingOrganizationKey(result.installation.getOrganizationKey());
        dataset.setInstallationKey(result.installation.getKey());
        dataset.setType(DatasetType.OCCURRENCE);
        LOG.info("Dataset {} «{}» is to be added", dataset.getKey(), dataset.getTitle());
      }

      // Deleted datasets
      for (Dataset dataset : result.deletedDatasets) {
        if (dataset.isLockedForAutoUpdate()) {
          LOG.info(
              "Dataset {} «{}» is locked, but would otherwise be deleted",
              dataset.getKey(),
              dataset.getTitle());
        } else {
          LOG.info("Dataset {} «{}» is to be deleted", dataset.getKey(), dataset.getTitle());
        }
      }

      // Updated datasets
      for (Dataset existingDataset : result.existingDatasets.keySet()) {
        if (RegistryUpdater.skipDatasetUpdate(existingDataset)) {
          LOG.info(
              "Dataset {} «{}» is locked, but would otherwise be updated",
              existingDataset.getKey(),
              existingDataset.getTitle());
        } else {
          LOG.info(
              "Dataset {} «{}» is to be updated",
              existingDataset.getKey(),
              existingDataset.getTitle());
        }
      }

    } else {
      LOG.info(
          "Installation [{}] failed sync. Reason: [{}]",
          result.installation.getKey(),
          result.exception.getErrorCode());
      if (result.exception.getMessage() != null) {
        LOG.info("Message: [{}]", result.exception.getMessage());
      }
      if (result.exception.getCause() != null) {
        LOG.info(
            "Cause: [{}], [{}]",
            result.exception.getCause().getClass().toString(),
            result.exception.getCause().getMessage());
      }
    }
  }

  private DebugHandler() {
    throw new UnsupportedOperationException("Can't initialize class");
  }
}
