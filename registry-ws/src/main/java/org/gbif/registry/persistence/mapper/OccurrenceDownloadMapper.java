/*
 * Copyright 2013 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.search.Facet;
import org.gbif.api.model.occurrence.Download;

import java.util.*;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

/**
 * Mapper that perform operations on occurrence downloads.
 */
public interface OccurrenceDownloadMapper {

  Download get(@Param("key") String key);

  Download getByDOI(@Param("doi") DOI doi);

  void update(Download entity);

  void create(Download entity);

  List<Download> list(@Nullable @Param("page") Pageable page);

  int count();

  List<Download> listByStatus(@Nullable @Param("page") Pageable page, @Param("status") Set<Download.Status> status);

  int countByStatus(@Param("status") Set<Download.Status> status);

  List<Download> listByUser(@Param("creator") String creator, @Nullable @Param("page") Pageable page, @Param("status") Set<Download.Status> status);

  int countByUser(@Param("creator") String creator, @Param("status") Set<Download.Status> status);


  List<Facet.Count> getMonthlyStats(@Nullable @Param("fromDate") Date fromDate,
                                    @Nullable @Param("toDate") Date toDate,
                                    @Nullable @Param("userCountry") String userCountry,
                                    @Nullable @Param("publishingCountry") String publishingCountry,
                                    @Nullable @Param("datasetKey") UUID datasetKey);

  List<Facet.Count> getDownloadedRecordsMonthlyStats(@Nullable @Param("fromDate") Date fromDate,
                                                     @Nullable @Param("toDate") Date toDate,
                                                     @Nullable @Param("userCountry") String userCountry,
                                                     @Nullable @Param("publishingCountry") String publishingCountry,
                                                     @Nullable @Param("datasetKey") UUID datasetKey);

}
