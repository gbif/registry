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
package org.gbif.registry.ws.export;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.occurrence.DownloadStatistics;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.model.registry.search.DatasetSearchResult;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetSubtype;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.License;

import java.io.StringWriter;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CsvWriterTest {

  /**
   * Functional interface to test a single line.
   */
  private interface AssertElement<T> {
    void assertElement(T element, String[] line);
  }

  /**
   * Tests a report against a source list fo elements.
   */
  private <T> void assertExport(List<T> source,
                                StringWriter writer,
                                CsvWriter csvWriter,
                                AssertElement<T> assertElement) {
    String export = writer.toString();
    String[] lines = export.split("\\n");

    //Number of lines is header + list.size
    assertEquals(source.size() + 1, lines.length);

    //Each line has csvWriter.getHeader().length - 1 commands
    assertEquals((source.size() + 1) * (csvWriter.getFields().length - 1),
                 export.chars().filter(ch -> ch == csvWriter.getPreference().getDelimiter()).count());
    IntStream.range(0, source.size())
      .forEach(idx -> assertElement.assertElement(source.get(idx),
                                                  lines[idx + 1].split(csvWriter.getPreference().getDelimiter().toString())));
  }

  /**
   * Test one DownloadStatistic against its expected exported data.
   */
  private void assertDownloadStatistics(DownloadStatistics downloadStatistics, String line[]) {
    assertEquals(downloadStatistics.getDatasetKey().toString(), line[0]);
    assertEquals(downloadStatistics.getTotalRecords(), Integer.parseInt(line[1]));
    assertEquals(downloadStatistics.getNumberDownloads(), Integer.parseInt(line[2]));
    assertEquals(downloadStatistics.getYear(), Integer.parseInt(line[3]));
    assertEquals(downloadStatistics.getMonth(), Integer.parseInt(line[4]));
  }

  @Test
  public void downloadStatisticsTest() {

    //Test data
    List<DownloadStatistics> stats = Arrays.asList(
      new DownloadStatistics(UUID.randomUUID(), 10L, 10L, LocalDate.of(2020,1,1)),
      new DownloadStatistics(UUID.randomUUID(), 10L, 10L, LocalDate.of(2021, 2,1)));

    StringWriter writer = new StringWriter();

    CsvWriter<DownloadStatistics> csvWriter = CsvWriter.downloadStatisticsCsvWriter(stats, ExportFormat.TSV);
    csvWriter.export(writer);

    //Assert elements
    assertExport(stats, writer, csvWriter, this::assertDownloadStatistics);
  }

  /**
   * Generates a DatasetSearchResult, the consecutive parameters is used as postfix for titles,
   * projectIdentifier, occurrence and name usages counts.
   */
  private DatasetSearchResult newDatasetSearchResult(int consecutive){
    DatasetSearchResult datasetSearchResult = new DatasetSearchResult();
    datasetSearchResult.setKey(UUID.randomUUID());
    datasetSearchResult.setTitle("DatasetTitle" + consecutive);
    datasetSearchResult.setDoi(new DOI("10.21373/6m9yw" + consecutive));
    datasetSearchResult.setLicense(License.CC_BY_4_0);
    datasetSearchResult.setType(DatasetType.OCCURRENCE);
    datasetSearchResult.setSubtype(DatasetSubtype.DERIVED_FROM_OCCURRENCE);
    datasetSearchResult.setHostingOrganizationKey(UUID.randomUUID());
    datasetSearchResult.setHostingOrganizationTitle("HostingOrganizationTitle" + consecutive);
    datasetSearchResult.setHostingCountry(Country.DENMARK);
    datasetSearchResult.setPublishingOrganizationKey(UUID.randomUUID());
    datasetSearchResult.setPublishingOrganizationTitle("PublishingOrganizationTitle" + consecutive);
    datasetSearchResult.setPublishingCountry(Country.COSTA_RICA);
    datasetSearchResult.setEndorsingNodeKey(UUID.randomUUID());
    datasetSearchResult.setNetworkKeys(Arrays.asList(UUID.randomUUID(), UUID.randomUUID()));
    datasetSearchResult.setProjectIdentifier("project" + consecutive);
    datasetSearchResult.setRecordCount(consecutive);
    datasetSearchResult.setNameUsagesCount(consecutive);
    return datasetSearchResult;
  }


  /**
   * Test one DatasetSearchResult against its expected exported data.
   */
  private void assertDatasetSearchResult(DatasetSearchResult datasetSearchResult, String[] line) {
    assertEquals(datasetSearchResult.getKey().toString(), line[0]);
    assertEquals(datasetSearchResult.getTitle(), line[1]);
    assertEquals(datasetSearchResult.getDoi().toString(), line[2]);
    assertEquals(datasetSearchResult.getLicense().name(), line[3]);
    assertEquals(datasetSearchResult.getType().name(), line[4]);
    assertEquals(datasetSearchResult.getSubtype().name(), line[5]);
    assertEquals(datasetSearchResult.getHostingOrganizationKey().toString(), line[6]);
    assertEquals(datasetSearchResult.getHostingOrganizationTitle(), line[7]);
    assertEquals(datasetSearchResult.getHostingCountry().getIso2LetterCode(), line[8]);
    assertEquals(datasetSearchResult.getPublishingOrganizationKey().toString(), line[9]);
    assertEquals(datasetSearchResult.getPublishingOrganizationTitle(), line[10]);
    assertEquals(datasetSearchResult.getPublishingCountry().getIso2LetterCode(), line[11]);
    assertEquals(datasetSearchResult.getEndorsingNodeKey().toString(), line[12]);
    assertTrue(datasetSearchResult.getNetworkKeys()
                 .containsAll(Arrays.stream(line[13].split(CsvWriter.ARRAY_DELIMITER))
                                .map(UUID::fromString)
                                .collect(Collectors.toList())));
    assertEquals(datasetSearchResult.getProjectIdentifier(), line[14]);
    assertEquals(datasetSearchResult.getRecordCount(), Integer.parseInt(line[15]));
    //Last characters has carriage return \r
    assertEquals(datasetSearchResult.getNameUsagesCount(), Integer.parseInt(line[16].replace("\r","")));
  }

  @Test
  public void datasetSearchTest() {

    //Test data
    DatasetSearchResult datasetSearchResult1 = newDatasetSearchResult(1);
    DatasetSearchResult datasetSearchResult2 = newDatasetSearchResult(2);

    List<DatasetSearchResult> datasets = Arrays.asList(datasetSearchResult1, datasetSearchResult2);

    StringWriter writer = new StringWriter();

    CsvWriter<DatasetSearchResult> csvWriter = CsvWriter.datasetSearchResultCsvWriter(datasets, ExportFormat.CSV);
    csvWriter.export(writer);

    assertExport(datasets, writer, csvWriter, this::assertDatasetSearchResult);
  }

  /**
   * Test one DatasetOccurrenceDownloadUsage against its expected exported data.
   */
  private void assertDatasetOccurrenceDownloadUsage(DatasetOccurrenceDownloadUsage downloadUsage, String[] line) {
    assertEquals(downloadUsage.getDatasetDOI().toString(), line[0]);
    assertEquals(downloadUsage.getDatasetKey().toString(), line[1]);
    assertEquals(downloadUsage.getDatasetTitle(), line[2]);
    assertEquals(downloadUsage.getDatasetCitation(), line[3]);
    assertEquals(downloadUsage.getNumberRecords(), Long.parseLong(line[4].replace("\r","")));
  }

  /**
   * Generates test instances of DatasetOccurrenceDownloadUsage.
   */
  private static DatasetOccurrenceDownloadUsage newDatasetOccurrenceDownloadUsageTest(int consecutive) {
    DatasetOccurrenceDownloadUsage downloadUsage = new DatasetOccurrenceDownloadUsage();

    downloadUsage.setDatasetKey(UUID.randomUUID());
    downloadUsage.setDatasetDOI(new DOI("10.21373/6m9yw" + consecutive));
    downloadUsage.setDatasetTitle("UsageTitle" + consecutive);
    downloadUsage.setDatasetCitation("Citation" + consecutive);
    downloadUsage.setNumberRecords(consecutive);

    return downloadUsage;
  }

  @Test
  public void datasetOccurrenceDownloadUsageTest() {

    //Test data
    List<DatasetOccurrenceDownloadUsage> downloadUsages = Arrays.asList(newDatasetOccurrenceDownloadUsageTest(1),
                                                                        newDatasetOccurrenceDownloadUsageTest(2));

    StringWriter writer = new StringWriter();

    CsvWriter<DatasetOccurrenceDownloadUsage> csvWriter = CsvWriter.datasetOccurrenceDownloadUsageCsvWriter(downloadUsages, ExportFormat.CSV);
    csvWriter.export(writer);

    assertExport(downloadUsages, writer, csvWriter, this::assertDatasetOccurrenceDownloadUsage);
  }
}
