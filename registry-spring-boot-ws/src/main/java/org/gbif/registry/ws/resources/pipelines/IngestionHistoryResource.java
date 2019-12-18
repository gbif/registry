package org.gbif.registry.ws.resources.pipelines;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.registry.pipelines.IngestionHistoryService;
import org.gbif.registry.pipelines.model.IngestionProcess;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(value = "ingestion/history",
  produces = MediaType.APPLICATION_JSON_VALUE)
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
