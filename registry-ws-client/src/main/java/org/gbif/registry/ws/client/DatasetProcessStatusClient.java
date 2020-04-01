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
package org.gbif.registry.ws.client;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.crawler.DatasetProcessStatus;
import org.gbif.api.service.registry.DatasetProcessStatusService;

import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient("DatasetProcessStatusClient")
public interface DatasetProcessStatusClient extends DatasetProcessStatusService {

  @Override
  default void createDatasetProcessStatus(@NotNull DatasetProcessStatus datasetProcessStatus) {
    throw new IllegalStateException("Dataset process status create not supported");
  }

  @Override
  default void updateDatasetProcessStatus(@NotNull DatasetProcessStatus datasetProcessStatus) {
    throw new IllegalStateException("Dataset process status update not supported");
  }

  @RequestMapping(
      method = RequestMethod.GET,
      value = "dataset/{datasetKey}/process/{attempt}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Nullable
  @ResponseBody
  @Override
  DatasetProcessStatus getDatasetProcessStatus(
      @PathVariable("datasetKey") UUID datasetKey, @PathVariable("attempt") int attempt);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "dataset/process",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<DatasetProcessStatus> listDatasetProcessStatus(@SpringQueryMap Pageable page);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "dataset/process/aborted",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<DatasetProcessStatus> listAbortedDatasetProcesses(@SpringQueryMap Pageable page);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "dataset/{datasetKey}/process",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<DatasetProcessStatus> listDatasetProcessStatus(
      @PathVariable("datasetKey") UUID datasetKey, @SpringQueryMap Pageable page);
}
