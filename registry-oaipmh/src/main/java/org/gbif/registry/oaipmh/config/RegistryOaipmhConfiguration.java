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
package org.gbif.registry.oaipmh.config;

import org.gbif.metrics.ws.client.CubeWsClient;
import org.gbif.registry.oaipmh.OaipmhItemRepository;
import org.gbif.registry.oaipmh.OaipmhSetRepository;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.service.RegistryDatasetService;
import org.gbif.ws.client.ClientBuilder;
import org.gbif.ws.json.JacksonJsonObjectMapperProvider;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.dspace.xoai.dataprovider.DataProvider;
import org.dspace.xoai.dataprovider.handlers.ErrorHandler;
import org.dspace.xoai.dataprovider.model.Context;
import org.dspace.xoai.dataprovider.model.MetadataFormat;
import org.dspace.xoai.dataprovider.repository.ItemRepository;
import org.dspace.xoai.dataprovider.repository.Repository;
import org.dspace.xoai.dataprovider.repository.RepositoryConfiguration;
import org.dspace.xoai.dataprovider.repository.SetRepository;
import org.dspace.xoai.model.oaipmh.DeletedRecord;
import org.dspace.xoai.model.oaipmh.Granularity;
import org.dspace.xoai.services.api.DateProvider;
import org.dspace.xoai.services.impl.SimpleResumptionTokenFormat;
import org.dspace.xoai.services.impl.UTCDateProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RegistryOaipmhConfiguration {

  // earliest date a dataset was created
  public static final Date EARLIEST_DATE;

  static {
    Calendar cal = Calendar.getInstance();
    cal.setTimeZone(TimeZone.getTimeZone("UTC"));
    cal.set(2007, Calendar.JANUARY, 1, 0, 0, 1);
    EARLIEST_DATE = cal.getTime();
  }

  private static final String REPO_NAME = "GBIF Registry";

  @Bean
  public Repository repository(
      ItemRepository itemRepository,
      SetRepository setRepository,
      RepositoryConfiguration repositoryConfiguration) {
    return new Repository()
        .withItemRepository(itemRepository)
        .withSetRepository(setRepository)
        .withResumptionTokenFormatter(new SimpleResumptionTokenFormat())
        .withConfiguration(repositoryConfiguration);
  }

  @Bean
  public DataProvider dataProvider(Repository repository) {
    return new DataProvider(context(), repository);
  }

  @Bean
  public DateProvider dateProvider() {
    return new UTCDateProvider();
  }

  @Bean
  public ErrorHandler errorHandler() {
    return new ErrorHandler();
  }

  private Context context() {
    return new Context()
        .withMetadataFormat(
            new MetadataFormat()
                .withPrefix("oai_dc")
                .withNamespace("http://www.openarchives.org/OAI/2.0/oai_dc/")
                .withSchemaLocation("http://www.openarchives.org/OAI/2.0/oai_dc.xsd")
                .withTransformer(xsltTransformer("dc.xslt")))
        .withMetadataFormat(
            new MetadataFormat()
                .withPrefix("eml")
                .withNamespace("eml://ecoinformatics.org/eml-2.1.1")
                .withSchemaLocation("http://rs.gbif.org/schema/eml-gbif-profile/1.0.2/eml.xsd")
                .withTransformer(xsltTransformer("eml.xslt")));
  }

  private Transformer xsltTransformer(String xsltFile) {
    TransformerFactory factory = TransformerFactory.newInstance();
    try {
      /*
       * TODO: "An object of this class [Transformer] may not be used in multiple threads running concurrently!"
       * Instead, XOAI should accept an immutable Templates object, and call .newTransformer() to get a Transformer.
       * See https://xalan.apache.org/xalan-j/usagepatterns.html#multithreading
       */
      InputStream stream =
          this.getClass()
              .getClassLoader()
              .getResourceAsStream("org/gbif/registry/oaipmh/" + xsltFile);
      return factory.newTransformer(new StreamSource(stream));
    } catch (TransformerConfigurationException e) {
      throw new RuntimeException("Unable to read XSLT transform " + xsltFile, e);
    }
  }

  @Bean
  public RepositoryConfiguration repositoryConfiguration(
      OaipmhConfigurationProperties oaipmhConfigProperties) {
    return new RepositoryConfiguration()
        .withRepositoryName(REPO_NAME)
        .withAdminEmail(oaipmhConfigProperties.getAdminEmail())
        .withBaseUrl(oaipmhConfigProperties.getBaseUrl())
        .withEarliestDate(EARLIEST_DATE)
        .withMaxListIdentifiers(1000)
        .withMaxListSets(1000)
        .withMaxListRecords(100)
        .withGranularity(Granularity.Second)
        .withDeleteMethod(DeletedRecord.PERSISTENT)
        .withDescription(
            "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n"
                + "\t<dc:title>GBIF Registry</dc:title>\n"
                + "\t<dc:creator>Global Biodiversity Information Facility Secretariat</dc:creator>\n"
                + "\t<dc:description>\n"
                + "\t\tThe GBIF Registry — the entities that make up the GBIF network.\n"
                + "\t\tThis OAI-PMH service exposes Datasets, organized into sets of country, installation and resource type.\n"
                + "\t\tFor more information, see https://www.gbif.org/developer/registry\n"
                + "\t</dc:description>\n"
                + "</oai_dc:dc>\n");
  }

  @Bean
  public ItemRepository itemRepository(
      RegistryDatasetService datasetService,
      DatasetMapper datasetMapper,
      OrganizationMapper organizationMapper,
      CubeWsClient metricsClient) {
    return new OaipmhItemRepository(
        datasetService, datasetMapper, organizationMapper, metricsClient);
  }

  @Bean
  public CubeWsClient occurrenceMetricsClient(@Value("${api.root.url}") String url) {
    ClientBuilder clientBuilder = new ClientBuilder();
    clientBuilder.withObjectMapper(JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport());
    clientBuilder.withUrl(url);
    return clientBuilder.build(CubeWsClient.class);
  }

  @Bean
  public SetRepository setRepository(DatasetMapper datasetMapper) {
    return new OaipmhSetRepository(datasetMapper);
  }
}
