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
import org.gbif.ws.client.ClientFactory;

public class RegistryWsClientFactory {

  private static final ClientFactory.ConnectionPoolConfig CONNECTION_POOL_CONFIG =
      ClientFactory.ConnectionPoolConfig.builder()
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
    ClientFactory clientFactory = new ClientFactory(REGISTRY_API_BASE_URL);
    if (datasetServiceReadOnly == null) {
      datasetServiceReadOnly =
          clientFactory.newInstance(DatasetClient.class, CONNECTION_POOL_CONFIG);
    }
    return datasetServiceReadOnly;
  }

  /** @return DatasetService with authentication */
  public static synchronized DatasetService datasetService() {
    ClientFactory clientFactory = new ClientFactory(USERNAME, PASSWORD, REGISTRY_API_BASE_URL);
    if (datasetService == null) {
      datasetService = clientFactory.newInstance(DatasetClient.class, CONNECTION_POOL_CONFIG);
    }
    return datasetService;
  }

  /** @return InstallationService with authentication */
  public static synchronized InstallationService installationService() {
    ClientFactory clientFactory = new ClientFactory(USERNAME, PASSWORD, REGISTRY_API_BASE_URL);
    if (installationService == null) {
      installationService =
          clientFactory.newInstance(InstallationClient.class, CONNECTION_POOL_CONFIG);
    }
    return installationService;
  }
}
