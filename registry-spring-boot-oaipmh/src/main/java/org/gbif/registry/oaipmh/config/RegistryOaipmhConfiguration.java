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

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.dspace.xoai.dataprovider.repository.RepositoryConfiguration;
import org.dspace.xoai.model.oaipmh.DeletedRecord;
import org.dspace.xoai.model.oaipmh.Granularity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RegistryOaipmhConfiguration {

  // earliest date a dataset was created
  private static final Date EARLIEST_DATE;

  static {
    Calendar cal = Calendar.getInstance();
    cal.setTimeZone(TimeZone.getTimeZone("UTC"));
    cal.set(2007, 0, 1, 0, 0, 1);
    EARLIEST_DATE = cal.getTime();
  }

  private static final String REPO_NAME = "GBIF Registry";

  private OaipmhConfigurationProperties oaipmhConfigProperties;

  public RegistryOaipmhConfiguration(OaipmhConfigurationProperties oaipmhConfigProperties) {
    this.oaipmhConfigProperties = oaipmhConfigProperties;
  }

  @Bean
  public RepositoryConfiguration repositoryConfiguration() {
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
}
