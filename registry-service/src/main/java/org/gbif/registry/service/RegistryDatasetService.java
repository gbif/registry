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
package org.gbif.registry.service;

import java.util.Set;

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Metadata;
import org.gbif.api.vocabulary.MetadataType;
import org.gbif.registry.domain.ws.DerivedDatasetUsage;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface RegistryDatasetService {

  Dataset get(UUID key);

  PagingResponse<Dataset> augmentWithMetadata(PagingResponse<Dataset> resp);

  @Nullable
  Dataset getPreferredMetadataDataset(UUID key);

  List<Metadata> listMetadata(UUID datasetKey, @Nullable MetadataType type);

  byte[] getMetadataDocument(int metadataKey);

  List<DerivedDatasetUsage> ensureDerivedDatasetDatasetUsagesValid(Map<String, Long> data);

  /**
   * Adds DwcA metadata to a target dataset.
   *
   * @param datasetKey   key of target dataset
   * @param dwcA         dqwcA metadata to add
   *
   */
  void createDwcaData(@NotNull UUID datasetKey, @NotNull @Valid Dataset.DwcA dwcA);

  /**
   * Updates DwcA metadata to a target dataset.
   *
   * @param datasetKey   key of target dataset
   * @param dwcA         dqwcA metadata
   *
   */
  void updateDwcaData(@NotNull UUID datasetKey, @NotNull @Valid Dataset.DwcA dwcA);

  /**
   * Finds datasets containing any of the given deprecated categories.
   * Used during vocabulary synchronization to identify datasets requiring updates.
   *
   * @param deprecatedCategories set of deprecated category names
   * @return list of datasets with deprecated categories
   */
  List<Dataset> findDatasetsWithDeprecatedCategories(Set<String> deprecatedCategories);
}
