package org.gbif.registry.doi;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.utils.file.ClosableReportingIterator;
import org.gbif.utils.file.FileUtils;
import org.gbif.utils.file.csv.CSVReader;
import org.gbif.utils.file.csv.CSVReaderFactory;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import com.beust.jcommander.internal.Lists;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class CustomDownloadDataCiteConverterTest {

  private static final Logger LOG = LoggerFactory.getLogger(CustomDownloadDataCiteConverterTest.class);
  private static final Configuration FREEMARKER_CONFIG = createConfiguration();

  /**
   * @return FreeMarker Configuration
   */
  public static Configuration createConfiguration() {
    Configuration configuration = new Configuration();
    configuration.setDefaultEncoding(StandardCharsets.UTF_8.toString());
    TemplateLoader tl = new ClassTemplateLoader(CustomDownloadDataCiteConverterTest.class, "/customdownload");
    configuration.setTemplateLoader(tl);
    return configuration;
  }

  /**
   * Creates DataCite metadata XML document and homepage markdown file for custom download by reading
   * download properties from customDownload.properties and list of used datasets from usedDatasets.txt.
   */
  @Test
  public void createCustomDownload()
    throws IOException, InvalidMetadataException, ParseException, TemplateException {
    // gather custom download properties
    Properties properties = PropertiesUtil.loadProperties("customdownload/download.properties");
    DOI doi = new DOI(properties.getProperty("downloadDoi"));
    String size = properties.getProperty("downloadSizeInBytes");
    String numberRecords = properties.getProperty("downloadNumberRecords");
    String creatorName = properties.getProperty("creatorName");
    String creatorUserId = properties.getProperty("creatorUserId");
    Calendar createdDate = Calendar.getInstance();
    createdDate.setTime(DateFormatUtils.ISO_DATE_FORMAT.parse(properties.getProperty("createdIsoDate")));

    // populate list of used Datasets
    List<DatasetOccurrenceDownloadUsage> usedDatasets = getUsedDatasets();
    testUsedDatasets(usedDatasets);
    properties.put("downloadNumberDatasets", usedDatasets.size());

    // create DataCite metadata XML file
    DataCiteMetadata metadata = CustomDownloadDataCiteConverter
      .convert(doi, size, numberRecords, creatorName, creatorUserId, createdDate, usedDatasets);
    String xml = DataCiteValidator.toXml(doi, metadata);

    // write it to tmp directory
    File output = org.gbif.utils.file.FileUtils.createTempDir();
    File xmlFile = new File(output, "custom_download-" + DateFormatUtils.ISO_DATE_FORMAT.format(createdDate) + ".xml");
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

  /**
   * @return list of DatasetOccurrenceDownloadUsage populated from usedDatasets.txt
   */
  private List<DatasetOccurrenceDownloadUsage> getUsedDatasets() throws IOException {
    // load the .txt file to process
    InputStream fis = DataCiteConverterTest.class.getResourceAsStream("/customdownload/usedDatasets.txt");
    // create an iterator on the file
    CSVReader reader = CSVReaderFactory.build(fis, "UTF-8", "\t", null, 1);

    List<DatasetOccurrenceDownloadUsage> usages = Lists.newArrayList();
    ClosableReportingIterator<String[]> iter = reader.iterator();
    while (iter.hasNext()) {
      String[] record = iter.next();
      DatasetOccurrenceDownloadUsage usage = new DatasetOccurrenceDownloadUsage();

      // Dataset key @ column #1 (index 0)
      UUID datasetKey = UUID.fromString(record[0]);
      usage.setDatasetKey(datasetKey);

      // Dataset title @ column #2 (index 1)
      String datasetTitle = record[1];
      usage.setDatasetTitle(datasetTitle);

      // Dataset DOI @ column #3 (index 2)
      DOI datasetDoi = new DOI(record[2]);
      usage.setDatasetDOI(datasetDoi);

      // Number of records @ column #4 (index 3)
      Long numberRecords = Long.valueOf(record[3]);
      usage.setNumberRecords(numberRecords);

      usages.add(usage);
    }
    return usages;
  }

  /**
   * Perform some tests to ensure used Datasets list was populated correctly.
   */
  private void testUsedDatasets(List<DatasetOccurrenceDownloadUsage> usedDatasets) {
    assertEquals(2, usedDatasets.size());
    assertEquals(UUID.fromString("01536750-8af5-430c-b0e2-077dee7f7d5f"), usedDatasets.get(0).getDatasetKey());
    assertEquals("Registros biológicos del Humedal Santa Maria del Lago 1999-2013",
      usedDatasets.get(0).getDatasetTitle());
    assertEquals(new DOI("10.15468/uvzgpk").getDoiName(), usedDatasets.get(0).getDatasetDOI().getDoiName());
    assertEquals(1, usedDatasets.get(0).getNumberRecords());
  }
}
