package org.gbif.registry.ws.resources;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.service.registry.DatasetOccurrenceDownloadUsageService;
import org.gbif.registry.persistence.mapper.DatasetOccurrenceDownloadMapper;
import org.gbif.ws.server.interceptor.NullToNotFound;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static org.gbif.registry.ws.util.DownloadSecurityUtils.clearSensitiveData;

/**
 * Occurrence download resource/web service.
 */
@RestController
@RequestMapping(value = "occurrence/download/dataset",
    produces = MediaType.APPLICATION_JSON_VALUE,
    consumes = MediaType.APPLICATION_JSON_VALUE) // TODO: 29/08/2019 add produce javascript
public class DatasetOccurrenceDownloadUsageResource implements DatasetOccurrenceDownloadUsageService {

  private final DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper;

  public DatasetOccurrenceDownloadUsageResource(DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper) {
    this.datasetOccurrenceDownloadMapper = datasetOccurrenceDownloadMapper;
  }

  @GetMapping("/{datasetKey}")
  @NullToNotFound
  @Override
  public PagingResponse<DatasetOccurrenceDownloadUsage> listByDataset(@PathVariable UUID datasetKey, Pageable page) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    List<DatasetOccurrenceDownloadUsage> usages = datasetOccurrenceDownloadMapper.listByDataset(datasetKey, page);
    clearSensitiveData(authentication, usages);
    return new PagingResponse<>(page, (long) datasetOccurrenceDownloadMapper.countByDataset(datasetKey), usages);
  }
}
