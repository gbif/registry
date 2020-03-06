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
package org.gbif.registry.oaipmh;

import org.gbif.registry.oaipmh.config.OaipmhConfigurationProperties;
import org.gbif.registry.oaipmh.config.RegistryOaipmhConfiguration;

import org.dspace.xoai.dataprovider.repository.RepositoryConfiguration;
import org.dspace.xoai.model.oaipmh.DeletedRecord;
import org.dspace.xoai.model.oaipmh.Granularity;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class OaipmhTestConfiguration {

  public static final int MAX_LIST_RECORDS = 2;

  @Primary
  @Bean
  public static RepositoryConfiguration repositoryConfigurationTest(
      OaipmhConfigurationProperties configProperties) {
    return new RepositoryConfiguration()
        .withRepositoryName("GBIF Test Registry")
        .withAdminEmail(configProperties.getAdminEmail())
        .withBaseUrl(configProperties.getBaseUrl())
        .withEarliestDate(RegistryOaipmhConfiguration.EARLIEST_DATE)
        .withMaxListIdentifiers(2)
        .withMaxListSets(2)
        .withMaxListRecords(MAX_LIST_RECORDS)
        .withGranularity(Granularity.Second)
        .withDeleteMethod(DeletedRecord.PERSISTENT)
        .withDescription("<TestDescription xmlns=\"\">Test description</TestDescription>");
  }
}
