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

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.registry.domain.ws.DerivedDataset;
import org.gbif.registry.domain.ws.DerivedDatasetUsage;

import java.util.List;

public interface RegistryDerivedDatasetService {

  String getCitationText(DOI derivedDatasetDoi);

  DerivedDataset create(
      DerivedDataset derivedDataset, List<DerivedDatasetUsage> derivedDatasetUsages);

  void update(DerivedDataset derivedDataset);

  DerivedDataset get(DOI derivedDatasetDoi);

  PagingResponse<DerivedDataset> getDerivedDataset(String datasetKeyOrDoi, Pageable page);

  PagingResponse<DerivedDatasetUsage> getRelatedDatasets(DOI derivedDatasetDoi, Pageable pageable);

  List<DerivedDatasetUsage> listRelatedDatasets(DOI derivedDatasetDoi);

  PagingResponse<DerivedDataset> listByUser(String user, Pageable page);

  /**
   * Finds all derived datasets that contain any of the specified deprecated categories.
   * This method is used during vocabulary synchronization to identify datasets that need
   * to be updated when categories are deprecated.
   *
   * @param deprecatedCategories the set of deprecated category names to search for
   * @return list of derived datasets that contain at least one of the deprecated categories
   */
  List<DerivedDataset> findDatasetsWithDeprecatedCategories(Set<String> deprecatedCategories);
}
