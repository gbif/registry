/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.registry.doi.handler;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.Dataset;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.RelationType;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

/** Business logic for DOI handling with DataCite. */
public interface DataCiteDoiHandlerStrategy {

  /** Tells is a DOI is using a predefined prefix. */
  boolean isUsingMyPrefix(DOI doi);

  /** Build the DataCiteMetadata for a Download. */
  DataCiteMetadata buildMetadata(Download download, GbifUser user);

  /** Build the DataCiteMetadata for a Dataset. */
  DataCiteMetadata buildMetadata(Dataset dataset);

  /** Build the DataCiteMetadata for a Dataset. */
  DataCiteMetadata buildMetadata(DOI doi, String creatorName, String title, List<DOI> relatedDatasets);

  /** Build the DataCiteMetadata for a Dataset that includes a relation to another DOI. */
  DataCiteMetadata buildMetadata(
      Dataset dataset, @Nullable DOI related, @Nullable RelationType relationType);

  /**
   * Called when some data related to the Dataset changed. The implementation decides the action to
   * take with the DOI service.
   *
   * @param previousDoi DOI of this dataset prior to the update.
   */
  void datasetChanged(Dataset dataset, @Nullable final DOI previousDoi);

  /**
   * Called when some data in the Download changed. The implementation decides the action to take
   * with the DOI service.
   *
   * @param previousDownload download object as it appears before the change or null if the change
   *     is triggered by something else
   */
  void downloadChanged(Download download, Download previousDownload, GbifUser user);

  /** Directly schedule the registration of a Dataset DOI. */
  void scheduleDatasetRegistration(DOI doi, DataCiteMetadata metadata, UUID datasetKey);

  /** Directly schedule the registration of a Derived Dataset DOI. */
  void scheduleDerivedDatasetRegistration(DOI doi, DataCiteMetadata metadata, URI target);
}
