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

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper that perform operations on dataset usages in occurrence downloads.
 */
public interface DatasetOccurrenceDownloadMapper {

  List<DatasetOccurrenceDownloadUsage> listByDataset(@Param("datasetKey") UUID datasetKey,
    @Nullable @Param("page") Pageable page);

  int countByDataset(@Param("datasetKey") UUID datasetKey);

  List<DatasetOccurrenceDownloadUsage> listByDownload(@Param("downloadKey") String downloadKey,
                                                     @Nullable @Param("page") Pageable page);
  
  void createUsages(@Param("downloadKey") String downloadKey, @Param("citationMap") Map<UUID,Long> downloadDataset);
}
