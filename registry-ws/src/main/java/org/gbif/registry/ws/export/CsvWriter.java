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

import java.io.Writer;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ParseEnum;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ParseLong;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.util.CsvContext;

import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;

@Data
@Builder
public class CsvWriter<T> {

  //Delimiter used for list/array of elements
  public static final String ARRAY_DELIMITER = ";";

  private final String[] header;

  private final String[] fields;

  private final CellProcessor[] processors;

  private final Iterable<T> pager;

  private final ExportFormat preference;


  private CsvPreference csvPreference() {
    if (ExportFormat.CSV == preference) {
      return CsvPreference.STANDARD_PREFERENCE;
    } else if (ExportFormat.TSV == preference) {
      return CsvPreference.TAB_PREFERENCE;
    }
    throw new IllegalArgumentException("Export format not supported " + preference);
  }

  @SneakyThrows
  public void export(Writer writer) {
    try (ICsvBeanWriter beanWriter = new CsvBeanWriter(writer, csvPreference())) {
      beanWriter.writeHeader(header);
      for (T o : pager) {
        beanWriter.write(o, fields, processors);
      }
    }
  }

  /**
   * Creates an CsvWriter/exporter of DownloadStatistics.
   */
  public static CsvWriter<DownloadStatistics> downloadStatisticsCsvWriter(Iterable<DownloadStatistics> pager,
                                                                          ExportFormat preference) {

    return CsvWriter.<DownloadStatistics>builder()
              .fields(new String[]{"datasetKey", "totalRecords", "numberDownloads", "year", "month"})
              .header(new String[]{"dataset_key", "total_records", "number_downloads", "year", "month"})
              .processors(new CellProcessor[]{new UUIDProcessor(),           //datasetKey
                                              new Optional(new ParseLong()),  //totalRecords
                                              new Optional(new ParseLong()),  //numberDowloads
                                              new Optional(new ParseInt()),  //year
                                              new Optional(new ParseInt())   //month
                                              })
              .preference(preference)
              .pager(pager)
              .build();
  }

  /**
   * Creates an CsvWriter/exporter of DatasetSearchResult.
   */
  public static CsvWriter<DatasetSearchResult> datasetSearchResultCsvWriter(Iterable<DatasetSearchResult> pager,
                                                                            ExportFormat preference) {
    return CsvWriter.<DatasetSearchResult>builder()
      .fields(new String[]{"key", "title", "doi", "license", "type", "subType", "hostingOrganizationKey", "hostingOrganizationTitle", "hostingCountry", "publishingOrganizationKey", "publishingOrganizationTitle", "publishingCountry","endorsingNodeKey", "networkKeys", "projectIdentifier", "recordCount", "nameUsagesCount"})
      .header(new String[]{"dataset_key", "title", "doi", "license", "type", "sub_type", "hosting_organization_Key", "hosting_organization_title", "hosting_country","publishing_organization_key", "publishing_organization_title", "publishing_country", "endorsing_node_key", "network_keys", "project_identifier", "occurrence_records_count", "name_usages_count"})
      //  "recordCount", "nameUsagesCount"
      .processors(new CellProcessor[]{new UUIDProcessor(),                              //key
                                      new CleanStringProcessor(),                       //title
                                      new DOIProcessor(),                               //doi
                                      new Optional(new ParseEnum(License.class)),       //license
                                      new Optional(new ParseEnum(DatasetType.class)),   //type
                                      new Optional(new ParseEnum(DatasetSubtype.class)),//subType
                                      new UUIDProcessor(),                              //hostingOrganizationKey
                                      new CleanStringProcessor(),                       //hostingOrganizationTitle
                                      new CountryProcessor(),                           //hostingCountry
                                      new UUIDProcessor(),                              //publishingOrganizationKey
                                      new CleanStringProcessor(),                       //publishingOrganizationTitle
                                      new CountryProcessor(),                           //publishingCountry
                                      new UUIDProcessor(),                              //endorsingNodeKey
                                      new ListUUIDProcessor(),                          //networkKeys
                                      new CleanStringProcessor(),                       //projectIdentifier
                                      new Optional(new ParseInt()),                     //recordCount
                                      new Optional(new ParseInt())                      //nameUsagesCount
                                      })
      .preference(preference)
      .pager(pager)
      .build();
  }

  /**
   * Creates an CsvWriter/exporter of DatasetOccurrenceDownloadUsage.
   */
  public static CsvWriter<DatasetOccurrenceDownloadUsage> datasetOccurrenceDownloadUsageCsvWriter(Iterable<DatasetOccurrenceDownloadUsage> pager,
                                                                                                  ExportFormat preference) {
    return CsvWriter.<DatasetOccurrenceDownloadUsage>builder()
      .fields(new String[]{"download",     "downloadKey",  "datasetDOI", "datasetKey",  "datasetTitle",  "datasetCitation",  "numberRecords"})
      .header(new String[]{"download_doi", "download_key", "dataset_doi", "dataset_key", "dataset_title", "dataset_citation", "number_records"})
      .processors(new CellProcessor[]{new DownloadDOIProcessor(),   //download.doi
                                      null,                         //downloadKey
                                      new DOIProcessor(),           //datasetDOI
                                      new UUIDProcessor(),          //datasetKey
                                      new CleanStringProcessor(),   //datasetTitle
                                      new CleanStringProcessor(),   //datasetCitation
                                      new Optional(new ParseLong()) //numberRecords
                                      })
      .preference(preference)
      .pager(pager)
      .build();

  }

  /**
   * Null aware UUID processor.
   */
  private static class UUIDProcessor implements CellProcessor {
    @Override
    public String execute(Object value, CsvContext csvContext) {
      return value != null ? ((UUID) value).toString() : "";
    }
  }

  /**
   * Null aware List of UUIDs processor.
   */
  private static class ListUUIDProcessor implements CellProcessor {
    @Override
    public String execute(Object value, CsvContext csvContext) {
      return value != null ?
        ((List<UUID>) value).stream().map(UUID::toString).collect(Collectors.joining(ARRAY_DELIMITER)) : "";
    }
  }

  /**
   * Null aware UUID processor.
   */
  private static class DOIProcessor implements CellProcessor {
    @Override
    public String execute(Object value, CsvContext csvContext) {
      return value != null ? value.toString() : "";
    }
  }

  /**
   * Null aware UUID processor.
   */
  private static class DownloadDOIProcessor implements CellProcessor {

    private static String getDoiValue(Download download) {
      return java.util.Optional.ofNullable(download.getDoi()).map(DOI::toString).orElse("");
    }

    @Override
    public String execute(Object value, CsvContext csvContext) {
      return value != null ? getDoiValue((Download)value) : "";
    }
  }


  /**
   * Null aware Country processor.
   */
  private static class CountryProcessor implements CellProcessor {
    @Override
    public String execute(Object value, CsvContext csvContext) {
      return value != null ? ((Country) value).getIso2LetterCode() : "";
    }
  }


  /**
   * Produces a String instance clean of delimiter.
   * If the value is null an empty string is returned.
   * Borrowed from Occurrence Downloads!!.
   */
  private static class CleanStringProcessor implements CellProcessor {

    private static final String DELIMETERS_MATCH =
      "\\t|\\n|\\r|(?:(?>\\u000D\\u000A)|[\\u000A\\u000B\\u000C\\u000D\\u0085\\u2028\\u2029\\u0000])";

    public static final Pattern DELIMETERS_MATCH_PATTERN = Pattern.compile(DELIMETERS_MATCH);

    @Override
    public String execute(Object value, CsvContext context) {
      return value != null ? DELIMETERS_MATCH_PATTERN.matcher((String) value).replaceAll(" ") : "";
    }

  }
}
