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
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.Dataset;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.RelationType;
import org.gbif.registry.domain.ws.DerivedDataset;
import org.gbif.registry.domain.ws.DerivedDatasetUsage;

import java.util.List;

import javax.annotation.Nullable;

public interface DataCiteMetadataBuilderService {

  /**
   * Build the DataCiteMetadata for a Download.
   */
  DataCiteMetadata buildMetadata(Download download, GbifUser user);

  /**
   * Build the DataCiteMetadata for a Dataset.
   */
  DataCiteMetadata buildMetadata(Dataset dataset);

  /**
   * Build the DataCiteMetadata for a DerivedDataset.
   */
  DataCiteMetadata buildMetadata(
      DerivedDataset derivedDataset, List<DerivedDatasetUsage> derivedDatasetUsages);

  /**
   * Build the DataCiteMetadata for a Dataset that includes a relation to another DOI.
   */
  DataCiteMetadata buildMetadata(
      Dataset dataset, @Nullable DOI related, @Nullable RelationType relationType);
}
