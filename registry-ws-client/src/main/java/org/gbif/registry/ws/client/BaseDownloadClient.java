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
import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
import org.gbif.api.annotation.PartialDate;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.CountryOccurrenceDownloadUsage;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.model.registry.OrganizationOccurrenceDownloadUsage;
import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.CountryUsageSortField;
import org.gbif.api.vocabulary.DatasetUsageSortField;
import org.gbif.api.vocabulary.OrganizationUsageSortField;
import org.gbif.api.vocabulary.SortOrder;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface BaseDownloadClient extends OccurrenceDownloadService {

  // ---------------------------------------------------------------------------
  // CRUD
  // ---------------------------------------------------------------------------

  @Override
  @RequestLine("POST /")
  @Headers("Content-Type: application/json")
  void create(Download download);

  @Override
  @RequestLine("GET /{key}")
  @Headers("Accept: application/json")
  Download get(@Param("key") String key);

  @Override
  @RequestLine("PUT /")
  @Headers("Content-Type: application/json")
  Download update(Download download);

  // ---------------------------------------------------------------------------
  // Listing / counting
  // ---------------------------------------------------------------------------

  @Override
  @RequestLine("GET /")
  @Headers("Accept: application/json")
  PagingResponse<Download> list(
    @QueryMap Pageable pageable,
    @Param("status") Set<Download.Status> status,
    @Param("source") String source);

  @Override
  @RequestLine("GET /count?status={status}&source={source}")
  @Headers("Accept: application/json")
  long count(
    @Param("status") Set<Download.Status> status,
    @Param("source") String source);

  @Override
  @RequestLine(
    "GET /user/{user}?status={status}&from={from}&statistics={statistics}")
  @Headers("Accept: application/json")
  PagingResponse<Download> listByUser(
    @Param("user") String user,
    @QueryMap Pageable pageable,
    @Param("status") Set<Download.Status> status,
    @Param("from") LocalDateTime from,
    @Param("statistics") Boolean statistics);

  @Override
  @RequestLine("GET /user/{user}/count?status={status}&from={from}")
  @Headers("Accept: application/json")
  long countByUser(
    @Param("user") String user,
    @Param("status") Set<Download.Status> status,
    @Param("from") LocalDateTime from);

  @Override
  @RequestLine(
    "GET /internal/eraseAfter?eraseAfter={eraseAfter}&size={size}&erasureNotification={erasureNotification}")
  @Headers("Accept: application/json")
  PagingResponse<Download> listByEraseAfter(
    @QueryMap Pageable page,
    @Param("eraseAfter") String eraseAfterAsString,
    @Param("size") Long size,
    @Param("erasureNotification") String erasureNotificationAsString);

  // ---------------------------------------------------------------------------
  // Usage breakdowns
  // ---------------------------------------------------------------------------

  @Override
  default PagingResponse<DatasetOccurrenceDownloadUsage> listDatasetUsages(
    String key, Pageable page) {
    return listDatasetUsages(key, null, null, null, page);
  }

  @Override
  @RequestLine(
    "GET /{key}/datasets?datasetTitle={datasetTitle}&sortBy={sortBy}&sortOrder={sortOrder}")
  @Headers("Accept: application/json")
  PagingResponse<DatasetOccurrenceDownloadUsage> listDatasetUsages(
    @Param("key") String key,
    @Param("datasetTitle") String datasetTitle,
    @Param("sortBy") DatasetUsageSortField sortBy,
    @Param("sortOrder") SortOrder sortOrder,
    @QueryMap Pageable page);

  @Override
  @RequestLine(
    "GET /{key}/organizations?organizationTitle={organizationTitle}&sortBy={sortBy}&sortOrder={sortOrder}")
  @Headers("Accept: application/json")
  PagingResponse<OrganizationOccurrenceDownloadUsage> listOrganizationUsages(
    @Param("key") String key,
    @Param("organizationTitle") String organizationTitle,
    @Param("sortBy") OrganizationUsageSortField sortBy,
    @Param("sortOrder") SortOrder sortOrder,
    @QueryMap Pageable page);

  @Override
  @RequestLine(
    "GET /{key}/countries?sortBy={sortBy}&sortOrder={sortOrder}")
  @Headers("Accept: application/json")
  PagingResponse<CountryOccurrenceDownloadUsage> listCountryUsages(
    @Param("key") String key,
    @Param("sortBy") CountryUsageSortField sortBy,
    @Param("sortOrder") SortOrder sortOrder,
    @QueryMap Pageable page);

  // ---------------------------------------------------------------------------
  // Citation
  // ---------------------------------------------------------------------------

  @Override
  @RequestLine("GET /{key}/citation")
  String getCitation(@Param("key") String keyOrDoi);

  // ---------------------------------------------------------------------------
  // Statistics
  // ---------------------------------------------------------------------------

  @Override
  @RequestLine(
    "GET /statistics/downloadsByUserCountry?fromDate={fromDate}&toDate={toDate}&userCountry={userCountry}")
  @Headers("Accept: application/json")
  Map<Integer, Map<Integer, Long>> getDownloadsByUserCountry(
    @Param("fromDate") @PartialDate("fromDate") Date fromDate,
    @Param("toDate") @PartialDate("toDate") Date toDate,
    @Param("userCountry") Country userCountry);

  @Override
  @RequestLine(
    "GET /statistics/downloadsBySource?fromDate={fromDate}&toDate={toDate}&source={source}")
  @Headers("Accept: application/json")
  Map<Integer, Map<Integer, Long>> getDownloadsBySource(
    @Param("fromDate") @PartialDate("fromDate") Date fromDate,
    @Param("toDate") @PartialDate("toDate") Date toDate,
    @Param("source") String source);

  @Override
  @RequestLine(
    "GET /statistics/downloadedRecordsByDataset?fromDate={fromDate}&toDate={toDate}&publishingCountry={publishingCountry}&datasetKey={datasetKey}&publishingOrgKey={publishingOrgKey}")
  @Headers("Accept: application/json")
  Map<Integer, Map<Integer, Long>> getDownloadedRecordsByDataset(
    @Param("fromDate") @PartialDate("fromDate") Date fromDate,
    @Param("toDate") @PartialDate("toDate") Date toDate,
    @Param("publishingCountry") Country publishingCountry,
    @Param("datasetKey") UUID datasetKey,
    @Param("publishingOrgKey") UUID publishingOrgKey);

  @Override
  @RequestLine(
    "GET /statistics/downloadsByDataset?fromDate={fromDate}&toDate={toDate}&publishingCountry={publishingCountry}&datasetKey={datasetKey}&publishingOrgKey={publishingOrgKey}")
  @Headers("Accept: application/json")
  Map<Integer, Map<Integer, Long>> getDownloadsByDataset(
    @Param("fromDate") @PartialDate("fromDate") Date fromDate,
    @Param("toDate") @PartialDate("toDate") Date toDate,
    @Param("publishingCountry") Country publishingCountry,
    @Param("datasetKey") UUID datasetKey,
    @Param("publishingOrgKey") UUID publishingOrgKey);

  // ---------------------------------------------------------------------------
  // Usage creation
  // ---------------------------------------------------------------------------

  @Override
  @RequestLine("POST /{key}/datasets")
  @Headers("Content-Type: application/json")
  void createUsages(
    @Param("key") String key,
    Map<UUID, Long> datasetCitations);
}
