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
package org.gbif.registry.ws.resources;

import static org.gbif.registry.security.util.DownloadSecurityUtils.clearSensitiveData;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.service.registry.DatasetOccurrenceDownloadUsageService;
import org.gbif.registry.persistence.mapper.DatasetDownloadMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Base download resource/web service. */
public abstract class DatasetDownloadUsageResourceBase
    implements DatasetOccurrenceDownloadUsageService {

  private final DatasetDownloadMapper datasetDownloadMapper;

  public DatasetDownloadUsageResourceBase(DatasetDownloadMapper datasetDownloadMapper) {
    this.datasetDownloadMapper = datasetDownloadMapper;
  }

  @Operation(
      operationId = "getDatasetDownloadActivity",
      summary = "List the downloads activity of a dataset.",
      description =
          "Lists the downloads in which data from a dataset has been included. "
              + "The limit is set to a maximum of 100 results unless you set the showDownloadDetails parameter to false.")
  @Parameter(name = "datasetKey", description = "The key of the dataset.", in = ParameterIn.PATH)
  @Parameter(
      name = "showDownloadDetails",
      description =
          "Flag to indicate if we want the download details in the response. It defaults to true to keep backwards compatibility.",
      in = ParameterIn.QUERY)
  @Pageable.OffsetLimitParameters
  @ApiResponse(
      responseCode = "200",
      description = "Dataset found and download information returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{datasetKey}")
  @Override
  public PagingResponse<DatasetOccurrenceDownloadUsage> listByDataset(
      @PathVariable UUID datasetKey,
      @RequestParam(value = "showDownloadDetails", required = false, defaultValue = "true")
          Boolean showDownloadDetails,
      Pageable page) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (Boolean.TRUE.equals(showDownloadDetails) && page.getLimit() > 100) {
      page = new PagingRequest(page.getOffset(), 100);
    }

    List<DatasetOccurrenceDownloadUsage> usages = null;
    if (Boolean.FALSE.equals(showDownloadDetails)) {
      usages = datasetDownloadMapper.listByDatasetWithoutDownload(datasetKey, page);
    } else {
      usages = datasetDownloadMapper.listByDataset(datasetKey, page);
    }
    clearSensitiveData(authentication, usages);
    return new PagingResponse<>(
        page, (long) datasetDownloadMapper.countByDataset(datasetKey), usages);
  }
}
