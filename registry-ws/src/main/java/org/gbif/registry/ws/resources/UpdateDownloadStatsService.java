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

import org.gbif.api.model.occurrence.Download;
import org.gbif.registry.persistence.mapper.OccurrenceDownloadMapper;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility service to update download statistics.
 */
@Slf4j
@Service
@AllArgsConstructor
public class UpdateDownloadStatsService {

  @Autowired
  private OccurrenceDownloadMapper occurrenceDownloadMapper;

  /** Converts a LocalDateTime to Date using the systems' time zone and applies the adjuster parameter.*/
  private Date toDate(LocalDateTime localDateTime, TemporalAdjuster adjuster) {
    return Date.from(localDateTime.with(adjuster).atZone(ZoneId.systemDefault()).toInstant());
  }

  /** Converts a Date to LocalDateTime using the systems' time zone.*/
  private LocalDateTime toLocalDateTime(Date date) {
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
  }

  /** Updates the download stats asynchronously from a single occurrence download.*/
  public void updateDownloadStatsAsync(Download download) {
    CompletableFuture.runAsync(() -> {
      try {
        updateDownloadStats(download);
      } catch (Exception ex) {
        log.error("Error updating download {} statistics", download.getKey(), ex);
      }
    });
  }

  /** Updates the download stats from a single occurrence download.*/
  public void updateDownloadStats(Download download) {
    if (Download.Status.SUCCEEDED == download.getStatus() || Download.Status.FILE_ERASED == download.getStatus()) {

      LocalDateTime createdDate = toLocalDateTime(Optional.ofNullable(download.getCreated()).orElse(new Date()));

      Date fromDate = toDate(createdDate, TemporalAdjusters.firstDayOfMonth());
      Date toDate = toDate(createdDate, TemporalAdjusters.firstDayOfNextMonth());

      updateDownloadStats(fromDate, toDate);
    }
  }


  /** Update download statistics for a range of dates.*/
  @Transactional
  public void updateDownloadStats(Date fromDate, Date toDate) {
    occurrenceDownloadMapper.updateDownloadStats(fromDate, toDate);
    occurrenceDownloadMapper.updateDownloadUserStats(fromDate, toDate);
    occurrenceDownloadMapper.updateDownloadSourceStats(fromDate, toDate);
  }
}
