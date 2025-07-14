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

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.registry.domain.ws.DerivedDataset;
import org.gbif.registry.domain.ws.DerivedDatasetUsage;
import org.gbif.registry.service.RegistryDerivedDatasetService;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

/**
 * Stub implementation of RegistryDerivedDatasetService for CLI modules.
 * Provides minimal functionality needed by DatasetCategoryService.
 */
@Service
public class RegistryDerivedDatasetServiceStub implements RegistryDerivedDatasetService {

  @Override
  public String getCitationText(DOI derivedDatasetDoi) {
    return null;
  }

  @Override
  public DerivedDataset create(DerivedDataset derivedDataset, List<DerivedDatasetUsage> derivedDatasetUsages) {
    return null;
  }

  @Override
  public void update(DerivedDataset derivedDataset) {
    // No-op for CLI
  }

  @Override
  public DerivedDataset get(DOI derivedDatasetDoi) {
    return null;
  }

  @Override
  public PagingResponse<DerivedDataset> getDerivedDataset(String datasetKeyOrDoi, Pageable page) {
    return null;
  }

  @Override
  public PagingResponse<DerivedDatasetUsage> getRelatedDatasets(DOI derivedDatasetDoi, Pageable pageable) {
    return null;
  }

  @Override
  public List<DerivedDatasetUsage> listRelatedDatasets(DOI derivedDatasetDoi) {
    return null;
  }

  @Override
  public PagingResponse<DerivedDataset> listByUser(String user, Pageable page) {
    return null;
  }

  @Override
  public List<DerivedDataset> findDatasetsWithDeprecatedCategories(Set<String> deprecatedCategories) {
    // For CLI, we'll return empty list since we don't want to process derived datasets
    // The real implementation would query the database
    return List.of();
  }
} 