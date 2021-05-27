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
package org.gbif.registry.ws.resources;

import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.service.registry.DatasetOccurrenceDownloadUsageService;
import org.gbif.api.util.iterables.Iterables;
import org.gbif.registry.persistence.mapper.DatasetOccurrenceDownloadMapper;
import org.gbif.registry.ws.export.CsvWriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.gbif.registry.security.util.DownloadSecurityUtils.clearSensitiveData;

/** Occurrence download resource/web service. */
@Validated
@RestController
@RequestMapping(value = "occurrence/download/dataset", produces = MediaType.APPLICATION_JSON_VALUE)
public class DatasetOccurrenceDownloadUsageResource
    implements DatasetOccurrenceDownloadUsageService {

  //Page size to iterate over download stats export service
  private static final int EXPORT_LIMIT = 5_000;

  //Export header prefix
  private static final String FILE_HEADER_PRE = "attachment; filename=datasets_download_usage.";


  private final DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper;

  public DatasetOccurrenceDownloadUsageResource(
      DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper) {
    this.datasetOccurrenceDownloadMapper = datasetOccurrenceDownloadMapper;
  }

  @GetMapping("{datasetKey}")
  @Override
  public PagingResponse<DatasetOccurrenceDownloadUsage> listByDataset(
      @PathVariable UUID datasetKey, Pageable page) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    List<DatasetOccurrenceDownloadUsage> usages =
        datasetOccurrenceDownloadMapper.listByDataset(datasetKey, page);
    clearSensitiveData(authentication, usages);
    return new PagingResponse<>(
        page, (long) datasetOccurrenceDownloadMapper.countByDataset(datasetKey), usages);
  }

  @GetMapping("{datasetKey}/export")
  public void exportListByDataset(
    HttpServletResponse response,
    @PathVariable UUID datasetKey,
    @RequestParam(value = "format", defaultValue = "TSV") ExportFormat format) throws IOException {

    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, FILE_HEADER_PRE + format.name().toLowerCase());

    try (Writer writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream()))) {
      CsvWriter.datasetOccurrenceDownloadUsageCsvWriter(Iterables.datasetOccurrenceDownloadUsages(this,
                                                                                                  datasetKey,
                                                                                                  EXPORT_LIMIT),
                                                          format)
        .export(writer);
    }
  }
}
