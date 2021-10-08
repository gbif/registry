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
package org.gbif.config;

import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.registry.ws.client.DatasetClient;
import org.gbif.registry.ws.client.InstallationClient;
import org.gbif.ws.client.ClientBuilder;
import org.gbif.ws.json.JacksonJsonObjectMapperProvider;

public class RegistryWsClientFactory {

  private static final ClientBuilder.ConnectionPoolConfig CONNECTION_POOL_CONFIG =
      ClientBuilder.ConnectionPoolConfig.builder()
          .timeout(10000)
          .maxConnections(100)
          .maxPerRoute(100)
          .build();

  // GBIF account, with correct role and permissions set
  private static final String USERNAME = "ws_client_demo";
  private static final String PASSWORD = "Demo123";
  private static final String REGISTRY_API_BASE_URL = "http://api.gbif-uat.org/v1/";

  private static DatasetService datasetService;
  private static DatasetService datasetServiceReadOnly;
  private static InstallationService installationService;

  /** @return read-only DatasetService */
  public static synchronized DatasetService datasetServiceReadOnly() {
    ClientBuilder clientBuilder = new ClientBuilder();
    clientBuilder.withObjectMapper(JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport());
    clientBuilder
        .withUrl(REGISTRY_API_BASE_URL)
        .withConnectionPoolConfig(CONNECTION_POOL_CONFIG);

    if (datasetServiceReadOnly == null) {
      datasetServiceReadOnly = clientBuilder.build(DatasetClient.class);
    }
    return datasetServiceReadOnly;
  }

  /** @return DatasetService with authentication */
  public static synchronized DatasetService datasetService() {
    ClientBuilder clientBuilder = new ClientBuilder();
    clientBuilder.withObjectMapper(JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport());
    clientBuilder
        .withUrl(REGISTRY_API_BASE_URL)
        .withConnectionPoolConfig(CONNECTION_POOL_CONFIG)
        .withCredentials(USERNAME, PASSWORD);

    if (datasetService == null) {
      datasetService = clientBuilder.build(DatasetClient.class);
    }
    return datasetService;
  }

  /** @return InstallationService with authentication */
  public static synchronized InstallationService installationService() {
    ClientBuilder clientBuilder = new ClientBuilder();
    clientBuilder.withObjectMapper(JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport());
    clientBuilder
        .withUrl(REGISTRY_API_BASE_URL)
        .withConnectionPoolConfig(CONNECTION_POOL_CONFIG)
        .withCredentials(USERNAME, PASSWORD);

    if (installationService == null) {
      installationService = clientBuilder.build(InstallationClient.class);
    }
    return installationService;
  }
}
