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

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.occurrence.DownloadType;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.service.registry.DatasetOccurrenceDownloadUsageService;
import org.gbif.registry.persistence.mapper.DatasetOccurrenceDownloadMapper;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import static org.gbif.registry.security.util.DownloadSecurityUtils.clearSensitiveData;

/** Base download resource/web service. */
public abstract class DatasetDownloadUsageResourceBase
    implements DatasetOccurrenceDownloadUsageService {

  private final DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper;

  private final DownloadType downloadType;

  public DatasetDownloadUsageResourceBase(
      DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper, DownloadType downloadType) {
    this.datasetOccurrenceDownloadMapper = datasetOccurrenceDownloadMapper;
    this.downloadType = downloadType;
  }

  @GetMapping("{datasetKey}")
  @Override
  public PagingResponse<DatasetOccurrenceDownloadUsage> listByDataset(
      @PathVariable UUID datasetKey, Pageable page) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    List<DatasetOccurrenceDownloadUsage> usages =
        datasetOccurrenceDownloadMapper.listByDataset(datasetKey, downloadType, page);
    clearSensitiveData(authentication, usages);
    return new PagingResponse<>(
        page,
        (long) datasetOccurrenceDownloadMapper.countByDataset(datasetKey, downloadType),
        usages);
  }
}
