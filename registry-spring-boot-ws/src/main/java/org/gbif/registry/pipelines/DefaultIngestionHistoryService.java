package org.gbif.registry.pipelines;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.crawler.DatasetProcessStatus;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.registry.domain.pipelines.IngestionProcess;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.DatasetProcessStatusMapper;
import org.gbif.registry.persistence.mapper.pipelines.PipelineProcessMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
    // the process may not exist if that attempt never reached pipelines (e.g.: it was aborted before)
    if (pipelineProcess != null) {
      ingestionProcess.setPipelineExecutions(pipelineProcess.getExecutions());
    }

    return ingestionProcess;
  }
}
