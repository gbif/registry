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
package org.gbif.registry.cli.common.stubs;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.Dataset;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.RelationType;
import org.gbif.registry.doi.DataCiteMetadataBuilderService;
import org.gbif.registry.domain.ws.DerivedDataset;

import javax.annotation.Nullable;

public class DataCiteMetadataBuilderServiceStub implements DataCiteMetadataBuilderService {
  @Override
  public DataCiteMetadata buildMetadata(Download download, GbifUser user) {
    return null;
  }

  @Override
  public DataCiteMetadata buildMetadata(Dataset dataset) {
    return null;
  }

  @Override
  public DataCiteMetadata buildMetadata(DerivedDataset derivedDataset) {
    return null;
  }

  @Override
  public DataCiteMetadata buildMetadata(
      Dataset dataset, @Nullable DOI related, @Nullable RelationType relationType) {
    return null;
  }
}
