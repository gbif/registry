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
package org.gbif.registry.ws.it.persistence.mapper;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.search.Facet;
import org.gbif.api.model.occurrence.DownloadStatistics;
import org.gbif.api.model.occurrence.DownloadType;
import org.gbif.api.model.registry.Dataset;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.persistence.mapper.DownloadStatisticsMapper;
import org.gbif.registry.search.test.ElasticsearchTestContainerConfiguration;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Integration tests for {@link DownloadStatisticsMapper}. */
class DownloadStatisticsMapperIT extends BaseItTest {

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule =
      new TestCaseDatabaseInitializer(
          "download_statistics", "download_user_statistics", "download_source_statistics");

  private final DownloadStatisticsMapper mapper;
  private final TestDataFactory testDataFactory;
  private final DataSource dataSource;

  private UUID datasetKey;
  private String publishingCountry;

  private static final Date YEAR_MONTH_2023_06 = dateAtStartOfMonth(2023, 6);
  private static final Date YEAR_MONTH_2023_07 = dateAtStartOfMonth(2023, 7);
  private static final Date FROM_2023 = dateAtStartOfMonth(2023, 1);
  private static final Date TO_2024 = dateAtStartOfMonth(2024, 1);

  @Autowired
  public DownloadStatisticsMapperIT(
      DownloadStatisticsMapper mapper,
      TestDataFactory testDataFactory,
      DataSource dataSource,
      SimplePrincipalProvider principalProvider,
      ElasticsearchTestContainerConfiguration elasticsearchTestContainer) {
    super(principalProvider, elasticsearchTestContainer);
    this.mapper = mapper;
    this.testDataFactory = testDataFactory;
    this.dataSource = dataSource;
  }

  @BeforeEach
  public void setUp() throws Exception {
    Dataset dataset = testDataFactory.newPersistedDataset(new DOI("10.15468/dl.stats-test"));
    datasetKey = dataset.getKey();
    publishingCountry = "DK";
    insertDownloadStatsData();
  }

  private void insertDownloadStatsData() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);

      // download_statistics: year_month, publishing_organization_country, dataset_key, total_records, number_downloads, type
      try (PreparedStatement ps =
          conn.prepareStatement(
              "INSERT INTO download_statistics (year_month, publishing_organization_country, dataset_key, total_records, number_downloads, type) VALUES (?, ?, ?::uuid, ?, ?, ?::download_type)")) {
        ps.setTimestamp(1, new Timestamp(YEAR_MONTH_2023_06.getTime()));
        ps.setString(2, publishingCountry);
        ps.setObject(3, datasetKey);
        ps.setLong(4, 100L);
        ps.setInt(5, 5);
        ps.setString(6, DownloadType.OCCURRENCE.name());
        ps.executeUpdate();

        ps.setTimestamp(1, new Timestamp(YEAR_MONTH_2023_07.getTime()));
        ps.setLong(4, 200L);
        ps.setInt(5, 10);
        ps.executeUpdate();
      }

      // download_user_statistics: year_month, user_country, total_records, number_downloads, type
      try (PreparedStatement ps =
          conn.prepareStatement(
              "INSERT INTO download_user_statistics (year_month, user_country, total_records, number_downloads, type) VALUES (?, ?, ?, ?, ?::download_type)")) {
        ps.setTimestamp(1, new Timestamp(YEAR_MONTH_2023_06.getTime()));
        ps.setString(2, "DK");
        ps.setLong(3, 50L);
        ps.setInt(4, 3);
        ps.setString(5, DownloadType.OCCURRENCE.name());
        ps.executeUpdate();
      }

      // download_source_statistics: year_month, source, total_records, number_downloads, type
      try (PreparedStatement ps =
          conn.prepareStatement(
              "INSERT INTO download_source_statistics (year_month, source, total_records, number_downloads, type) VALUES (?, ?, ?, ?, ?::download_type)")) {
        ps.setTimestamp(1, new Timestamp(YEAR_MONTH_2023_06.getTime()));
        ps.setString(2, "API");
        ps.setLong(3, 75L);
        ps.setInt(4, 4);
        ps.setString(5, DownloadType.OCCURRENCE.name());
        ps.executeUpdate();
      }

      conn.commit();
    }
  }

  private static Date dateAtStartOfMonth(int year, int month) {
    return Date.from(
        YearMonth.of(year, month).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant());
  }

  @Test
  void testGetDownloadsByUserCountry() {
    List<Facet.Count> result =
        mapper.getDownloadsByUserCountry(FROM_2023, TO_2024, "DK", DownloadType.OCCURRENCE);
    assertNotNull(result);
    assertFalse(result.isEmpty());
    Facet.Count count = result.get(0);
    assertEquals("2023-06", count.getName());
    assertEquals(3L, count.getCount());
  }

  @Test
  void testGetDownloadsByUserCountryFilteredByDateRange() {
    List<Facet.Count> result =
        mapper.getDownloadsByUserCountry(YEAR_MONTH_2023_07, TO_2024, null, DownloadType.OCCURRENCE);
    assertNotNull(result);
    assertEquals(0, result.size());
  }

  @Test
  void testGetDownloadsBySource() {
    List<Facet.Count> result =
        mapper.getDownloadsBySource(FROM_2023, TO_2024, "API", DownloadType.OCCURRENCE);
    assertNotNull(result);
    assertFalse(result.isEmpty());
    Facet.Count count = result.get(0);
    assertEquals("2023-06", count.getName());
    assertEquals(4L, count.getCount());
  }

  @Test
  void testGetDownloadedRecordsByDataset() {
    List<Facet.Count> result =
        mapper.getDownloadedRecordsByDataset(
            FROM_2023, TO_2024, publishingCountry, datasetKey, null, DownloadType.OCCURRENCE);
    assertNotNull(result);
    assertFalse(result.isEmpty());
    Facet.Count count = result.get(0);
    assertEquals("2023-07", count.getName());
    assertEquals(200L, count.getCount());
  }

  @Test
  void testGetDownloadsByDataset() {
    List<Facet.Count> result =
        mapper.getDownloadsByDataset(
            FROM_2023, TO_2024, publishingCountry, datasetKey, null, DownloadType.OCCURRENCE);
    assertNotNull(result);
    assertFalse(result.isEmpty());
    Facet.Count count = result.get(0);
    assertEquals("2023-07", count.getName());
    assertEquals(10L, count.getCount());
  }

  @Test
  void testGetDownloadStatistics() {
    Pageable page = new PagingRequest(0, 10);
    List<DownloadStatistics> result =
        mapper.getDownloadStatistics(
            FROM_2023, TO_2024, publishingCountry, datasetKey, null, page, DownloadType.OCCURRENCE);
    assertNotNull(result);
    assertFalse(result.isEmpty());
    DownloadStatistics stat = result.get(0);
    assertEquals(datasetKey, stat.getDatasetKey());
    assertNotNull(stat.getYearMonth());
    assertTrue(stat.getNumberDownloads() >= 5);
    assertTrue(stat.getTotalRecords() >= 100);
  }

  @Test
  void testCountDownloadStatistics() {
    long count =
        mapper.countDownloadStatistics(
            FROM_2023, TO_2024, publishingCountry, datasetKey, null, DownloadType.OCCURRENCE);
    assertTrue(count >= 2);
  }

  @Test
  void testGetDownloadStatisticsFilteredByDateRange() {
    Pageable page = new PagingRequest(0, 10);
    List<DownloadStatistics> result =
        mapper.getDownloadStatistics(
            YEAR_MONTH_2023_07, TO_2024, null, null, null, page, DownloadType.OCCURRENCE);
    assertNotNull(result);
    assertFalse(result.isEmpty());
    DownloadStatistics stat = result.get(0);
    assertEquals(datasetKey, stat.getDatasetKey());
    assertEquals(10L, stat.getNumberDownloads());
    assertEquals(200L, stat.getTotalRecords());
  }
}
