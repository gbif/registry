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

import org.gbif.registry.persistence.mapper.OccurrenceDownloadMapper;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Utility service to update download statistics. */
@Slf4j
@Service
@AllArgsConstructor
@Endpoint(id = "downloadStatistics")
public class UpdateDownloadStatsService {

  @Autowired private OccurrenceDownloadMapper occurrenceDownloadMapper;

  @WriteOperation
  public void updateDownloadStatisticsEndpoint() {
    updateStats(LocalDateTime.now());
  }

  /**
   * Converts a LocalDateTime to Date using the systems' time zone and applies the adjuster
   * parameter.
   */
  private Date toDate(LocalDateTime localDateTime, TemporalAdjuster adjuster) {
    return Date.from(localDateTime.with(adjuster).atZone(ZoneId.systemDefault()).toInstant());
  }

  @Scheduled(cron = "${downloads.statistics.cron:0 0 9 1 * *}")
  @Transactional
  public void updateDownloadStatsTask() {
    // we get the stats from the previous month because the current month is not completed yet
    LocalDateTime previousMonthDate = LocalDateTime.now().minusMonths(1);
    updateStats(previousMonthDate);
  }

  private void updateStats(LocalDateTime date) {
    Date fromDate = toDate(date, TemporalAdjusters.firstDayOfMonth());
    Date toDate = toDate(date, TemporalAdjusters.firstDayOfNextMonth());

    log.info("Updating downloads stats for [{},{}]", fromDate, toDate);
    occurrenceDownloadMapper.updateDownloadStats(fromDate, toDate);
    occurrenceDownloadMapper.updateDownloadUserStats(fromDate, toDate);
    occurrenceDownloadMapper.updateDownloadSourceStats(fromDate, toDate);
    log.info("Downloads stats update done for [{},{}]", fromDate, toDate);
  }
}
