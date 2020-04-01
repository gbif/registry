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
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.occurrence.Download.Status;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.api.vocabulary.Country;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient("OccurrenceDownloadClient")
public interface OccurrenceDownloadClient extends OccurrenceDownloadService {

  @Override
  default void create(@NotNull Download download) {
    throw new IllegalStateException("Occurrence download create not supported");
  }

  @RequestMapping(
    method = RequestMethod.GET,
    value = "occurrence/download/{key}",
    produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  Download get(@NotNull @PathVariable("key") String key);

  @Override
  default PagingResponse<Download> list(@Nullable Pageable pageable, @Nullable Set<Status> set) {
    throw new IllegalStateException("Occurrence download list not supported");
  }

  @Override
  default PagingResponse<Download> listByUser(
      @NotNull String s, @Nullable Pageable pageable, @Nullable Set<Status> set) {
    throw new IllegalStateException("Occurrence download list by user not supported");
  }

  @RequestMapping(
    method = RequestMethod.PUT,
    value = "occurrence/download",
    consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  void update(@RequestBody @NotNull Download download);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "occurrence/download/{key}/datasets",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<DatasetOccurrenceDownloadUsage> listDatasetUsages(
      @NotNull @PathVariable("key") String key, @SpringQueryMap Pageable page);

  @Override
  default Map<Integer, Map<Integer, Long>> getDownloadsByUserCountry(
      @Nullable Date date, @Nullable Date date1, @Nullable Country country) {
    throw new IllegalStateException("Occurrence download get by user country not supported");
  }

  @Override
  default Map<Integer, Map<Integer, Long>> getDownloadedRecordsByDataset(
      @Nullable Date date, @Nullable Date date1, @Nullable Country country, @Nullable UUID uuid) {
    throw new IllegalStateException("Occurrence download get downloaded records by dataset not supported");
  }

  @RequestMapping(
      method = RequestMethod.POST,
      value = "occurrence/download/{key}/datasets",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  void createUsages(@PathVariable("key") String key, @RequestBody Map<UUID, Long> datasetCitations);
}
