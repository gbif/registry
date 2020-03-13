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
package org.gbif.registry.ws.resources.pipelines;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.registry.domain.pipelines.IngestionProcess;
import org.gbif.registry.pipelines.IngestionHistoryService;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "ingestion/history", produces = MediaType.APPLICATION_JSON_VALUE)
public class IngestionHistoryResource {

  private final IngestionHistoryService ingestionHistoryService;

  public IngestionHistoryResource(IngestionHistoryService ingestionHistoryService) {
    this.ingestionHistoryService = ingestionHistoryService;
  }

  @GetMapping
  public PagingResponse<IngestionProcess> history(Pageable pageable) {
    return ingestionHistoryService.ingestionHistory(pageable);
  }

  @GetMapping("{datasetKey}")
  public PagingResponse<IngestionProcess> history(
      @PathVariable("datasetKey") UUID datasetKey, Pageable pageable) {
    return ingestionHistoryService.ingestionHistory(datasetKey, pageable);
  }

  @GetMapping("{datasetKey}/{attempt}")
  public IngestionProcess getIngestion(
      @PathVariable("datasetKey") UUID datasetKey, @PathVariable("attempt") int attempt) {
    return ingestionHistoryService.getIngestionProcess(datasetKey, attempt);
  }
}
