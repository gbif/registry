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
package org.gbif.registry.doi.converter;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.utils.file.FileUtils;
import org.gbif.utils.file.properties.PropertiesUtil;
import org.gbif.utils.file.tabular.TabularDataFileReader;
import org.gbif.utils.file.tabular.TabularFiles;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlunit.matchers.CompareMatcher;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;

import static org.gbif.registry.doi.converter.DataCiteConverterTestCommon.getXmlMetadataFromFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomDownloadDataCiteConverterTest {

  private static final Logger LOG =
      LoggerFactory.getLogger(CustomDownloadDataCiteConverterTest.class);
  private static final Configuration FREEMARKER_CONFIG = createConfiguration();

  /** @return FreeMarker Configuration */
  public static Configuration createConfiguration() {
    Configuration configuration = new Configuration();
    configuration.setDefaultEncoding(StandardCharsets.UTF_8.toString());
    TemplateLoader tl =
        new ClassTemplateLoader(CustomDownloadDataCiteConverterTest.class, "/customdownload");
    configuration.setTemplateLoader(tl);
    return configuration;
  }

  /**
   * Creates DataCite metadata XML document and homepage markdown file for custom download by
   * reading download properties from customDownload.properties and list of used datasets from
   * usedDatasets.txt.
   */
  @Test
  public void createCustomDownload() throws Exception {
    // gather custom download properties
    Properties properties = PropertiesUtil.loadProperties("customdownload/download.properties");
    DOI doi = new DOI(properties.getProperty("downloadDoi"));
    String size = properties.getProperty("downloadSizeInBytes");
    String numberRecords = properties.getProperty("downloadNumberRecords");
    String creatorName = properties.getProperty("creatorName");
    String creatorUserId = properties.getProperty("creatorUserId");
    Calendar createdDate = Calendar.getInstance();
    createdDate.setTime(
        DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT.parse(
            properties.getProperty("createdIsoDate")));

    // populate list of used Datasets
    List<DatasetOccurrenceDownloadUsage> usedDatasets = getUsedDatasets();
    testUsedDatasets(usedDatasets);
    properties.put("downloadNumberDatasets", usedDatasets.size());

    // create DataCite metadata XML file
    DataCiteMetadata metadata =
        CustomDownloadDataCiteConverter.convert(
            doi, size, numberRecords, creatorName, creatorUserId, createdDate, usedDatasets);
    String xml = DataCiteValidator.toXml(doi, metadata);
    String expectedMetadataXml =
        getXmlMetadataFromFile("customdownload/custom-download-metadata.xml");
    assertThat(
        xml,
        CompareMatcher.isIdenticalTo(expectedMetadataXml).ignoreWhitespace().normalizeWhitespace());

    // write it to tmp directory
    File output = FileUtils.createTempDir();
    File xmlFile =
        new File(
            output,
            "custom_download-"
                + DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT.format(createdDate)
                + ".xml");
    Writer xmlFileWriter = FileUtils.startNewUtf8File(xmlFile);
    xmlFileWriter.write(xml);
    xmlFileWriter.close();

    // Create homepage markdown file and write it to tmp directory
    File homepageFile = new File(output, "readme.md");
    Writer homepageFileWriter = FileUtils.startNewUtf8File(homepageFile);
    FREEMARKER_CONFIG.getTemplate("homepage.ftl").process(properties, homepageFileWriter);
    homepageFileWriter.close();

    LOG.info("Files written to: " + output.getAbsolutePath());
  }

  /** @return list of DatasetOccurrenceDownloadUsage populated from usedDatasets.txt */
  private List<DatasetOccurrenceDownloadUsage> getUsedDatasets()
      throws IOException, ParseException {
    File csv = FileUtils.getClasspathFile("customdownload/usedDatasets.txt");
    List<DatasetOccurrenceDownloadUsage> usages = new ArrayList<>();

    try (TabularDataFileReader<List<String>> reader =
        TabularFiles.newTabularFileReader(
            Files.newBufferedReader(csv.toPath(), StandardCharsets.UTF_8), '\t', true)) {
      List<String> rec = reader.read();
      while (rec != null) {
        DatasetOccurrenceDownloadUsage usage = new DatasetOccurrenceDownloadUsage();

        // Dataset key @ column #1 (index 0)
        UUID datasetKey = UUID.fromString(rec.get(0));
        usage.setDatasetKey(datasetKey);

        // Dataset title @ column #2 (index 1)
        String datasetTitle = rec.get(1);
        usage.setDatasetTitle(datasetTitle);

        // Dataset DOI @ column #3 (index 2)
        DOI datasetDoi = new DOI(rec.get(2));
        usage.setDatasetDOI(datasetDoi);

        // Number of records @ column #4 (index 3)
        Long numberRecords = Long.valueOf(rec.get(3));
        usage.setNumberRecords(numberRecords);

        usages.add(usage);

        rec = reader.read();
      }
    }
    return usages;
  }

  /** Perform some tests to ensure used Datasets list was populated correctly. */
  private void testUsedDatasets(List<DatasetOccurrenceDownloadUsage> usedDatasets) {
    assertEquals(2, usedDatasets.size());
    assertEquals(
        UUID.fromString("01536750-8af5-430c-b0e2-077dee7f7d5f"),
        usedDatasets.get(0).getDatasetKey());
    assertEquals(
        "Registros biológicos del Humedal Santa Maria del Lago 1999-2013",
        usedDatasets.get(0).getDatasetTitle());
    assertEquals(
        new DOI("10.15468/uvzgpk").getDoiName(), usedDatasets.get(0).getDatasetDOI().getDoiName());
    assertEquals(1, usedDatasets.get(0).getNumberRecords());
  }
}
