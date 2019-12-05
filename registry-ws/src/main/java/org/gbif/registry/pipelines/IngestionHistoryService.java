package org.gbif.registry.pipelines;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.registry.pipelines.model.IngestionProcess;

import java.util.List;
import java.util.UUID;

public interface IngestionHistoryService {

  PagingResponse<IngestionProcess> ingestionHistory(Pageable pageable);

  PagingResponse<IngestionProcess> ingestionHistory(UUID datasetKey, Pageable pageable);

  IngestionProcess getIngestionProcess(UUID datasetKey, int attempt);
}
