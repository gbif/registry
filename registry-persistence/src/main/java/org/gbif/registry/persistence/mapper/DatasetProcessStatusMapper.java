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

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.crawler.DatasetProcessStatus;
import org.gbif.api.model.crawler.FinishReason;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/** Mapper that perform operations on {@link DatasetProcessStatus} instances. */
@Repository
public interface DatasetProcessStatusMapper {

  int count(@Nullable @Param("finishReasons") List<FinishReason> finishReasons);

  int countAborted();

  int countByDataset(
      @Param("datasetKey") UUID datasetKey,
      @Nullable @Param("finishReasons") List<FinishReason> finishReasons);

  void create(DatasetProcessStatus datasetProcessStatus);

  void update(DatasetProcessStatus datasetProcessStatus);

  DatasetProcessStatus get(@Param("datasetKey") UUID datasetKey, @Param("attempt") int attempt);

  List<DatasetProcessStatus> list(
      @Nullable @Param("finishReasons") List<FinishReason> finishReasons,
      @Nullable @Param("page") Pageable page);

  List<DatasetProcessStatus> listByDataset(
      @Param("datasetKey") UUID datasetKey,
      @Nullable @Param("finishReasons") List<FinishReason> finishReasons,
      @Nullable @Param("page") Pageable page);

  List<DatasetProcessStatus> listAborted(@Nullable @Param("page") Pageable page);
}
