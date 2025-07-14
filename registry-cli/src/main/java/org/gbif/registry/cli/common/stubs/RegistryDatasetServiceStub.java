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
package org.gbif.registry.cli.common.stubs;

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Metadata;
import org.gbif.api.vocabulary.MetadataType;
import org.gbif.registry.domain.ws.DerivedDatasetUsage;
import org.gbif.registry.service.RegistryDatasetService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Service;

/**
 * Stub implementation of RegistryDatasetService for CLI modules.
 * Provides minimal functionality needed by DatasetCategoryService.
 */
@Service
public class RegistryDatasetServiceStub implements RegistryDatasetService {

  @Override
  public Dataset get(UUID key) {
    return null;
  }

  @Override
  public PagingResponse<Dataset> augmentWithMetadata(PagingResponse<Dataset> resp) {
    return null;
  }

  @Override
  @Nullable
  public Dataset getPreferredMetadataDataset(UUID key) {
    return null;
  }

  @Override
  public List<Metadata> listMetadata(UUID datasetKey, @Nullable MetadataType type) {
    return null;
  }

  @Override
  public byte[] getMetadataDocument(int metadataKey) {
    return new byte[0];
  }

  @Override
  public List<DerivedDatasetUsage> ensureDerivedDatasetDatasetUsagesValid(Map<String, Long> data) {
    return null;
  }

  @Override
  public void createDwcaData(@NotNull UUID datasetKey, @NotNull @Valid Dataset.DwcA dwcA) {
    // No-op for CLI
  }

  @Override
  public void updateDwcaData(@NotNull UUID datasetKey, @NotNull @Valid Dataset.DwcA dwcA) {
    // No-op for CLI
  }

  @Override
  public List<Dataset> findDatasetsWithDeprecatedCategories(Set<String> deprecatedCategories) {
    // For CLI, we'll return empty list since we don't want to process datasets
    // The real implementation would query the database
    return List.of();
  }
} 