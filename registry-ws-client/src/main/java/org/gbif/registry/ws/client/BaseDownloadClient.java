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
package org.gbif.registry.ws.client;

import org.gbif.api.annotation.PartialDate;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.CountryOccurrenceDownloadUsage;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.model.registry.OrganizationOccurrenceDownloadUsage;
import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.api.vocabulary.Country;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.gbif.api.vocabulary.CountryUsageSortField;
import org.gbif.api.vocabulary.DatasetUsageSortField;
import org.gbif.api.vocabulary.OrganizationUsageSortField;
import org.gbif.api.vocabulary.SortOrder;

import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

public interface BaseDownloadClient extends OccurrenceDownloadService {

  @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  void create(@RequestBody Download download);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  Download get(@PathVariable("key") String key);

  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Download> list(
      @SpringQueryMap Pageable pageable,
      @RequestParam(value = "status", required = false) Set<Download.Status> status,
      @RequestParam(value = "source", required = false) String source);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "user/{user}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Download> listByUser(
      @PathVariable("user") String user,
      @SpringQueryMap Pageable pageable,
      @RequestParam(value = "status", required = false) Set<Download.Status> status);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "internal/eraseAfter",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Download> listByEraseAfter(
      @SpringQueryMap Pageable page,
      @RequestParam(value = "eraseAfter", required = false) String eraseAfterAsString,
      @RequestParam(value = "size", required = false) Long size,
      @RequestParam(value = "erasureNotification", required = false)
          String erasureNotificationAsString);

  @RequestMapping(method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  void update(@RequestBody Download download);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/datasets",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<DatasetOccurrenceDownloadUsage> listDatasetUsages(
      @PathVariable("key") String key, @SpringQueryMap Pageable page);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/datasets",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<DatasetOccurrenceDownloadUsage> listDatasetUsages(
      @PathVariable("key") String key,
      @RequestParam(value = "datasetTitle", required = false) String datasetTitle,
      @RequestParam(value = "sortBy", required = false) DatasetUsageSortField sortBy,
      @RequestParam(value = "sortOrder", required = false) SortOrder sortOrder,
      @SpringQueryMap Pageable page);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/organizations",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<OrganizationOccurrenceDownloadUsage> listOrganizationUsages(
      @PathVariable("key") String key,
      @RequestParam(value = "organizationTitle", required = false) String organizationTitle,
      @RequestParam(value = "sortBy", required = false) OrganizationUsageSortField sortBy,
      @RequestParam(value = "sortOrder", required = false) SortOrder sortOrder,
      @SpringQueryMap Pageable page);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/countries",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<CountryOccurrenceDownloadUsage> listCountryUsages(
      @PathVariable("key") String key,
      @RequestParam(value = "sortBy", required = false) CountryUsageSortField sortBy,
      @RequestParam(value = "sortOrder", required = false) SortOrder sortOrder,
      @SpringQueryMap Pageable page);

  @RequestMapping(method = RequestMethod.GET, value = "{key}/citation")
  @Override
  String getCitation(@PathVariable("key") String keyOrDoi);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "statistics/downloadsByUserCountry",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  Map<Integer, Map<Integer, Long>> getDownloadsByUserCountry(
      @PartialDate("fromDate") Date fromDate,
      @PartialDate("toDate") Date toDate,
      @RequestParam(value = "userCountry", required = false) Country userCountry);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "statistics/downloadsBySource",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  Map<Integer, Map<Integer, Long>> getDownloadsBySource(
      @PartialDate("fromDate") Date fromDate,
      @PartialDate("toDate") Date toDate,
      @RequestParam(value = "source", required = false) String source);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "statistics/downloadedRecordsByDataset",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  Map<Integer, Map<Integer, Long>> getDownloadedRecordsByDataset(
      @PartialDate("fromDate") Date fromDate,
      @PartialDate("toDate") Date toDate,
      @RequestParam(value = "publishingCountry", required = false) Country publishingCountry,
      @RequestParam(value = "datasetKey", required = false) UUID datasetKey,
      @RequestParam(value = "publishingOrgKey", required = false) UUID publishingOrgKey);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "statistics/downloadsByDataset",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  Map<Integer, Map<Integer, Long>> getDownloadsByDataset(
      @PartialDate("fromDate") Date fromDate,
      @PartialDate("toDate") Date toDate,
      @RequestParam(value = "publishingCountry", required = false) Country publishingCountry,
      @RequestParam(value = "datasetKey", required = false) UUID datasetKey,
      @RequestParam(value = "publishingOrgKey", required = false) UUID publishingOrgKey);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "{key}/datasets",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  void createUsages(@PathVariable("key") String key, @RequestBody Map<UUID, Long> datasetCitations);
}
