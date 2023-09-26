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

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.occurrence.DownloadType;
import org.gbif.api.model.registry.CountryOccurrenceDownloadUsage;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.model.registry.OrganizationOccurrenceDownloadUsage;
import org.gbif.api.vocabulary.CountryUsageSortField;
import org.gbif.api.vocabulary.DatasetUsageSortField;
import org.gbif.api.vocabulary.OrganizationUsageSortField;
import org.gbif.api.vocabulary.SortOrder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/** Mapper that perform operations on dataset usages in occurrence downloads. */
@Repository
public interface DatasetOccurrenceDownloadMapper {

  List<DatasetOccurrenceDownloadUsage> listByDataset(
      @Param("datasetKey") UUID datasetKey,
      @Param("type") DownloadType type,
      @Nullable @Param("page") Pageable page);

  List<DatasetOccurrenceDownloadUsage> listByDatasetWithoutDownload(
    @Param("datasetKey") UUID datasetKey,
    @Param("type") DownloadType type,
    @Nullable @Param("page") Pageable page);

  int countByDataset(@Param("datasetKey") UUID datasetKey, @Param("type") DownloadType type);

  /**
   * Note that the Download objects within the DatasetOccurrenceDownloadUsage are not retrieved, to
   * avoid massive repetition, and high memory use for complex queries.
   */
  List<DatasetOccurrenceDownloadUsage> listByDownload(
      @Param("downloadKey") String downloadKey,
      @Param("datasetTitle") String datasetTitle,
      @Param("sortBy") DatasetUsageSortField sortBy,
      @Param("sortOrder") SortOrder sortOrder,
      @Nullable @Param("page") Pageable page);

  void createOrUpdateUsages(
      @Param("downloadKey") String downloadKey,
      @Param("citationMap") Map<UUID, Long> downloadDataset);

  List<OrganizationOccurrenceDownloadUsage> listOrganizationsByDownload(
      @Param("downloadKey") String downloadKey,
      @Param("organizationTitle") String organizationTitle,
      @Param("sortBy") OrganizationUsageSortField sortBy,
      @Param("sortOrder") SortOrder sortOrder,
      @Nullable @Param("page") Pageable page);

  List<CountryOccurrenceDownloadUsage> listCountriesByDownload(
      @Param("downloadKey") String downloadKey,
      @Param("sortBy") CountryUsageSortField sortBy,
      @Param("sortOrder") SortOrder sortOrder,
      @Nullable @Param("page") Pageable page);

  int countOrganizationsByDownload(
      @Param("downloadKey") String downloadKey,
      @Param("organizationTitle") String organizationTitle);

  int countCountriesByDownload(@Param("downloadKey") String downloadKey);
}
