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
package org.gbif.registry.utils;

import org.gbif.registry.oaipmh.config.OaipmhConfigurationProperties;
import org.gbif.registry.oaipmh.config.RegistryOaipmhConfiguration;

import org.dspace.xoai.dataprovider.repository.RepositoryConfiguration;
import org.dspace.xoai.model.oaipmh.DeletedRecord;
import org.dspace.xoai.model.oaipmh.Granularity;

/**
 * Simple utility class to build RepositoryConfiguration for OAI-PMH testing.
 *
 * @author cgendreau
 */
public class OaipmhTestConfiguration {

  public static final int MAX_LIST_RECORDS = 2;

  public static RepositoryConfiguration buildTestRepositoryConfiguration(
      OaipmhConfigurationProperties properties) {

    return new RepositoryConfiguration()
        .withRepositoryName("GBIF Test Registry")
        .withAdminEmail(properties.getAdminEmail())
        .withBaseUrl(properties.getBaseUrl())
        .withEarliestDate(RegistryOaipmhConfiguration.EARLIEST_DATE)
        .withMaxListIdentifiers(2)
        .withMaxListSets(2)
        .withMaxListRecords(MAX_LIST_RECORDS)
        .withGranularity(Granularity.Second)
        .withDeleteMethod(DeletedRecord.PERSISTENT)
        .withDescription("<TestDescription xmlns=\"\">Test description</TestDescription>");
  }
}
