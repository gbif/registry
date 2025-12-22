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
package org.gbif.registry.ws.client;
import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.crawler.DatasetProcessStatus;
import org.gbif.api.service.registry.DatasetProcessStatusService;

import java.util.UUID;

public interface DatasetProcessStatusClient extends DatasetProcessStatusService {

  @RequestLine("POST /dataset/{datasetKey}/process")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json"
  })
  void createDatasetProcessStatus(
    @Param("datasetKey") UUID datasetKey,
    DatasetProcessStatus datasetProcessStatus
  );

  default void createDatasetProcessStatus(DatasetProcessStatus datasetProcessStatus) {
    createDatasetProcessStatus(
      datasetProcessStatus.getDatasetKey(),
      datasetProcessStatus
    );
  }

  @RequestLine("PUT /dataset/{datasetKey}/process/{attempt}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json"
  })
  void updateDatasetProcessStatus(
    @Param("datasetKey") UUID datasetKey,
    @Param("attempt") int attempt,
    DatasetProcessStatus datasetProcessStatus
  );

  default void updateDatasetProcessStatus(DatasetProcessStatus datasetProcessStatus) {
    updateDatasetProcessStatus(
      datasetProcessStatus.getDatasetKey(),
      datasetProcessStatus.getCrawlJob().getAttempt(),
      datasetProcessStatus
    );
  }

  @RequestLine("GET /dataset/{datasetKey}/process/{attempt}")
  @Headers("Accept: application/json")
  DatasetProcessStatus getDatasetProcessStatus(
    @Param("datasetKey") UUID datasetKey,
    @Param("attempt") int attempt
  );

  @RequestLine("GET /dataset/process")
  @Headers("Accept: application/json")
  PagingResponse<DatasetProcessStatus> listDatasetProcessStatus(
    @QueryMap Pageable page
  );

  @RequestLine("GET /dataset/process/aborted")
  @Headers("Accept: application/json")
  PagingResponse<DatasetProcessStatus> listAbortedDatasetProcesses(
    @QueryMap Pageable page
  );

  @RequestLine("GET /dataset/{datasetKey}/process")
  @Headers("Accept: application/json")
  PagingResponse<DatasetProcessStatus> listDatasetProcessStatus(
    @Param("datasetKey") UUID datasetKey,
    @QueryMap Pageable page
  );
}
