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
package org.gbif.registry.cli.common;

import org.gbif.datacite.rest.client.DataCiteClient;
import org.gbif.datacite.rest.client.configuration.ClientConfiguration;
import org.gbif.datacite.rest.client.retrofit.DataCiteRetrofitSyncClient;
import org.gbif.doi.service.datacite.RestJsonApiDataCiteService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CommonBuilder is used to build objects from the configuration objects */
public class CommonBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(DataCiteConfiguration.class);

  private CommonBuilder() {}

  public static RestJsonApiDataCiteService createRestJsonApiDataCiteService(
      ClientConfiguration cfg) {
    return new RestJsonApiDataCiteService(new DataCiteRetrofitSyncClient(cfg));
  }

  public static RestJsonApiDataCiteService createRestJsonApiDataCiteService(
      DataCiteConfiguration cfg) {
    LOG.debug("Creating RestJsonApiDataCite service");

    ClientConfiguration clientConfiguration =
        ClientConfiguration.builder()
            .withBaseApiUrl(cfg.api.toString())
            .withUser(cfg.username)
            .withPassword(cfg.password)
            .build();

    DataCiteClient dataCiteClient = new DataCiteRetrofitSyncClient(clientConfiguration);

    return new RestJsonApiDataCiteService(dataCiteClient);
  }
}
