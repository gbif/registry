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
package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.registry.domain.ws.DerivedDataset;
import org.gbif.registry.domain.ws.DerivedDatasetUsage;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DerivedDatasetMapper {

  void create(@Param("derivedDataset") DerivedDataset derivedDataset);

  void update(@Param("derivedDataset") DerivedDataset derivedDataset);

  DerivedDataset get(@Param("doi") DOI doi);

  void addUsagesToDerivedDataset(
      @Param("derivedDatasetDoi") DOI derivedDatasetDoi,
      @Param("derivedDatasetUsages") List<DerivedDatasetUsage> derivedDatasetUsages);

  List<DerivedDataset> listByDataset(
      @Param("datasetKey") UUID datasetKey, @Nullable @Param("page") Pageable page);

  long countByDataset(@Param("datasetKey") UUID datasetKey);

  List<DerivedDatasetUsage> listDerivedDatasetUsages(
      @Param("derivedDatasetDoi") DOI derivedDatasetDoi, @Nullable @Param("page") Pageable page);

  long countDerivedDatasetUsages(@Param("derivedDatasetDoi") DOI derivedDatasetDoi);

  List<DerivedDataset> listByRegistrationDate(@Param("registrationDate") Date registrationDate);

  List<DerivedDataset> listByUser(
      @Param("creator") String creator, @Nullable @Param("page") Pageable page);

  long countByUser(@Param("creator") String creator);

  /**
   * Find derived datasets that have deprecated categories.
   * @param deprecatedCategories set of deprecated category names
   * @return list of derived datasets that have deprecated categories
   */
  List<DerivedDataset> findDerivedDatasetsWithDeprecatedCategories(@Param("deprecatedCategories") Set<String> deprecatedCategories);

}
