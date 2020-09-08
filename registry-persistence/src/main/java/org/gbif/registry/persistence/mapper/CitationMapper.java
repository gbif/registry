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
package org.gbif.registry.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.Dataset;
import org.gbif.registry.domain.ws.Citation;
import org.gbif.registry.domain.ws.CitationDatasetUsage;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Repository
public interface CitationMapper {

  void create(@Param("citation") Citation citation);

  Citation get(@Param("doi") DOI doi);

  void addCitationDatasets(
      @Param("citationDoi") DOI citationDoi,
      @Param("citationDatasetUsages") List<CitationDatasetUsage> citationDatasetUsages);

  List<Citation> listByDataset(
      @Param("datasetKey") UUID datasetKey, @Nullable @Param("page") Pageable page);

  long countByDataset(@Param("datasetKey") UUID datasetKey);

  List<Dataset> listByCitation(
      @Param("citationDoi") DOI citationDoi, @Nullable @Param("page") Pageable page);

  long countByCitation(@Param("citationDoi") DOI citationDoi);

  List<Citation> listByRegistrationDate(@Param("registrationDate") Date registrationDate);
}
