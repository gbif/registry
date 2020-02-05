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
package org.gbif.registry.pipelines;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.crawler.DatasetProcessStatus;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.registry.Dataset;
import org.gbif.registry.domain.pipelines.IngestionProcess;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.DatasetProcessStatusMapper;
import org.gbif.registry.persistence.mapper.pipelines.PipelineProcessMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class DefaultIngestionHistoryService implements IngestionHistoryService {

  private final DatasetProcessStatusMapper datasetProcessStatusMapper;
  private final PipelineProcessMapper pipelineProcessMapper;
  private final DatasetMapper datasetMapper;

  public DefaultIngestionHistoryService(
      DatasetProcessStatusMapper datasetProcessStatusMapper,
      PipelineProcessMapper pipelineProcessMapper,
      DatasetMapper datasetMapper) {
    this.datasetProcessStatusMapper = datasetProcessStatusMapper;
    this.pipelineProcessMapper = pipelineProcessMapper;
    this.datasetMapper = datasetMapper;
  }

  @Override
  public PagingResponse<IngestionProcess> ingestionHistory(Pageable pageable) {
    List<IngestionProcess> ingestions =
        datasetProcessStatusMapper.list(pageable).stream()
            .map(this::toIngestionProcess)
            .collect(Collectors.toList());

    long count = datasetProcessStatusMapper.count();

    return new PagingResponse<>(pageable, count, ingestions);
  }

  @Override
  public PagingResponse<IngestionProcess> ingestionHistory(UUID datasetKey, Pageable pageable) {
    List<IngestionProcess> ingestions =
        datasetProcessStatusMapper.listByDataset(datasetKey, pageable).stream()
            .map(this::toIngestionProcess)
            .collect(Collectors.toList());

    long count = datasetProcessStatusMapper.countByDataset(datasetKey);

    return new PagingResponse<>(pageable, count, ingestions);
  }

  @Override
  public IngestionProcess getIngestionProcess(UUID datasetKey, int attempt) {
    return Optional.ofNullable(datasetProcessStatusMapper.get(datasetKey, attempt))
        .map(this::toIngestionProcess)
        .orElse(null);
  }

  private IngestionProcess toIngestionProcess(DatasetProcessStatus datasetProcessStatus) {
    UUID datasetKey = datasetProcessStatus.getDatasetKey();
    int attempt = datasetProcessStatus.getCrawlJob().getAttempt();

    IngestionProcess ingestionProcess =
        new IngestionProcess()
            .setDatasetKey(datasetKey)
            .setAttempt(attempt)
            .setCrawlInfo(datasetProcessStatus);

    Dataset dataset = datasetMapper.get(datasetKey);
    if (dataset != null) {
      ingestionProcess.setDatasetTitle(dataset.getTitle());
    }

    // add pipeline info
    PipelineProcess pipelineProcess =
        pipelineProcessMapper.getByDatasetAndAttempt(datasetKey, attempt);
    // the process may not exist if that attempt never reached pipelines (e.g.: it was aborted
    // before)
    if (pipelineProcess != null) {
      ingestionProcess.setPipelineExecutions(pipelineProcess.getExecutions());
    }

    return ingestionProcess;
  }
}
