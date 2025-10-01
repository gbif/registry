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

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.occurrence.DownloadType;
import org.springframework.stereotype.Repository;

/** Mapper that perform operations on occurrence downloads. */
public interface DownloadMapper {

  Download get(@Param("key") String key);

  Download getWithCounts(@Param("key") String key);

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

  List<Download> listByUserLightweight(
      @Param("creator") String creator,
      @Nullable @Param("page") Pageable page,
      @Param("status") Set<Download.Status> status,
      @Nullable @Param("type") DownloadType type,
      @Nullable @Param("from") LocalDateTime from);

  List<Download> listByUser(
      @Param("creator") String creator,
      @Nullable @Param("page") Pageable page,
      @Param("status") Set<Download.Status> status,
      @Nullable @Param("type") DownloadType type,
      @Nullable @Param("from") LocalDateTime from);

  int countByUser(
      @Param("creator") String creator,
      @Param("status") Set<Download.Status> status,
      @Nullable @Param("type") DownloadType type,
      @Nullable @Param("from") LocalDateTime from);

  List<Download> listByEraseAfter(
      @Nullable @Param("page") Pageable page,
      @Param("eraseAfter") Date eraseAfter,
      @Param("size") Long size,
      @Param("erasureNotification") Date erasureNotification);

  int countByEraseAfter(
      @Param("eraseAfter") Date eraseAfter,
      @Param("size") Long size,
      @Param("erasureNotification") Date erasureNotification);
}
