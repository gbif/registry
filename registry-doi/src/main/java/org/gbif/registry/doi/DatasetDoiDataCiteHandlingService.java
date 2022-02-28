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
package org.gbif.registry.doi;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Dataset;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * Business logic for Dataset DOI handling with DataCite.
 */
public interface DatasetDoiDataCiteHandlingService {

  /**
   * Called when some data related to the Dataset changed. The implementation decides the action to
   * take with the DOI service.
   *
   * @param previousDoi DOI of this dataset prior to the update.
   */
  void datasetChanged(Dataset dataset, @Nullable final DOI previousDoi);

  /**
   * Called when dataset to be deleted, tries to deactivate DOI.
   */
  void datasetDeleted(DOI doi);

  /**
   * Directly schedule the registration of a Dataset DOI.
   */
  void scheduleDatasetRegistration(DOI doi, DataCiteMetadata metadata, UUID datasetKey);

  /**
   * Directly schedule the registration of a Derived Dataset DOI.
   */
  void scheduleDerivedDatasetRegistration(
      DOI doi, DataCiteMetadata metadata, URI target, Date registrationDate);
}
