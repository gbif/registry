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
package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.search.Facet;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.occurrence.DownloadStatistics;
import org.gbif.api.model.occurrence.DownloadType;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/** Mapper that perform operations on occurrence downloads. */
@Repository
public interface OccurrenceDownloadMapper {

  Download get(@Param("key") String key);

  Download getByDOI(@Param("doi") DOI doi);

  void update(Download entity);

  void create(Download entity);

  List<Download> list(
      @Nullable @Param("page") Pageable page,
      @Param("status") Set<Download.Status> status,
      @Nullable @Param("type") DownloadType type,
      @Nullable @Param("source") String source);

  int count(
      @Param("status") Set<Download.Status> status,
      @Nullable @Param("type") DownloadType type,
      @Nullable @Param("source") String source);

  void updateNotificationAddresses(
      @Param("oldCreator") String oldCreator,
      @Param("newCreator") String newCreator,
      @Param("notificationAddressesAsString") String notificationAddressesAsString);

  List<Download> listByUser(
      @Param("creator") String creator,
      @Nullable @Param("page") Pageable page,
      @Param("status") Set<Download.Status> status,
      @Nullable @Param("type") DownloadType type);

  int countByUser(@Param("creator") String creator,
                  @Param("status") Set<Download.Status> status,
                  @Nullable @Param("type") DownloadType type);

  List<Download> listByEraseAfter(
      @Nullable @Param("page") Pageable page,
      @Param("eraseAfter") Date eraseAfter,
      @Param("size") Long size,
      @Param("erasureNotification") Date erasureNotification);

  int countByEraseAfter(
      @Param("eraseAfter") Date eraseAfter,
      @Param("size") Long size,
      @Param("erasureNotification") Date erasureNotification);

  List<Facet.Count> getDownloadsByUserCountry(
      @Nullable @Param("fromDate") Date fromDate,
      @Nullable @Param("toDate") Date toDate,
      @Nullable @Param("userCountry") String userCountry,
      @Nullable @Param("type") DownloadType type);

  List<Facet.Count> getDownloadsBySource(
    @Nullable @Param("fromDate") Date fromDate,
    @Nullable @Param("toDate") Date toDate,
    @Nullable @Param("source") String source,
    @Nullable @Param("type") DownloadType type);

  List<Facet.Count> getDownloadedRecordsByDataset(
      @Nullable @Param("fromDate") Date fromDate,
      @Nullable @Param("toDate") Date toDate,
      @Nullable @Param("publishingCountry") String publishingCountry,
      @Nullable @Param("datasetKey") UUID datasetKey,
      @Nullable @Param("publishingOrgKey") UUID publishingOrgKey,
      @Nullable @Param("type") DownloadType type);

  List<Facet.Count> getDownloadsByDataset(
      @Nullable @Param("fromDate") Date fromDate,
      @Nullable @Param("toDate") Date toDate,
      @Nullable @Param("publishingCountry") String publishingCountry,
      @Nullable @Param("datasetKey") UUID datasetKey,
      @Nullable @Param("publishingOrgKey") UUID publishingOrgKey,
      @Nullable @Param("type") DownloadType type);

  List<DownloadStatistics> getDownloadStatistics(
      @Nullable @Param("fromDate") Date fromDate,
      @Nullable @Param("toDate") Date toDate,
      @Nullable @Param("publishingCountry") String publishingCountry,
      @Nullable @Param("datasetKey") UUID datasetKey,
      @Nullable @Param("publishingOrgKey") UUID publishingOrgKey,
      @Nullable @Param("page") Pageable page,
      @Nullable @Param("type") DownloadType type);

  long countDownloadStatistics(
      @Nullable @Param("fromDate") Date fromDate,
      @Nullable @Param("toDate") Date toDate,
      @Nullable @Param("publishingCountry") String publishingCountry,
      @Nullable @Param("datasetKey") UUID datasetKey,
      @Nullable @Param("publishingOrgKey") UUID publishingOrgKey,
      @Nullable @Param("type") DownloadType type);

  void updateDownloadStats(@Param("fromDate") Date fromDate, @Param("toDate") Date toDate);

  void updateDownloadUserStats(@Param("fromDate") Date fromDate, @Param("toDate") Date toDate);

  void updateDownloadSourceStats(@Param("fromDate") Date fromDate, @Param("toDate") Date toDate);
}
