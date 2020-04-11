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
package org.gbif.registry.ws.fixtures;

import org.gbif.registry.ws.util.JerseyBaseClient;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

import static org.gbif.registry.ws.fixtures.UserTestFixture.USER_RESOURCE_PATH;

// TODO: 11/04/2020 remove
/** Generates and offer utilities related to authenticated client in the context of testing. */
public class TestClient {

  private final String wsBaseUrl;

  public TestClient(String wsBaseUrl) {
    this.wsBaseUrl = wsBaseUrl;
  }

  /**
   * Build a {@link Client} using default settings and no authentication.
   *
   * @return new {@link Client} instance
   */
  public static Client buildPublicClient() {
    ClientConfig clientConfig = new DefaultClientConfig();
    clientConfig.getFeatures().put("com.sun.jersey.api.json.POJOMappingFeature", Boolean.TRUE);
    return Client.create(clientConfig);
  }

  /**
   * Build a {@link Client} using default settings and Basic authentication
   *
   * @param username
   * @param password
   * @return new {@link Client} instance
   */
  public static Client buildAuthenticatedClient(String username, String password) {
    Client client = buildPublicClient();
    client.addFilter(new HTTPBasicAuthFilter(username, password));
    return client;
  }

  /**
   * This client will only work against a Grizzly server started from {@link
   * org.gbif.registry.grizzly.RegistryServer}.
   *
   * @return
   */
  public static Client buildAuthenticatedAdmin() {
    Client client = buildPublicClient();
    client.addFilter(new HTTPBasicAuthFilter(TestConstants.TEST_ADMIN, TestConstants.TEST_ADMIN));
    return client;
  }

  /**
   * Call the login endpoint using a HTTP Basic authentication.
   *
   * @param username
   * @param password
   * @return
   */
  public ClientResponse login(String username, String password) {
    return generateAuthenticatedClient(username, password, USER_RESOURCE_PATH)
        .get(wr -> wr.path("login"));
  }

  /**
   * Call the login endpoint using a HTTP Basic authentication.
   *
   * @param username
   * @param password
   * @return
   */
  public ClientResponse loginPost(String username, String password) {
    return generateAuthenticatedClient(username, password, USER_RESOURCE_PATH)
        .post(wr -> wr.path("login"), null);
  }

  /**
   * Generate a new {@link JerseyBaseClient} for another resource than the one defined by this test.
   *
   * @param userName
   * @param password
   * @param resourcePath
   * @return
   */
  public JerseyBaseClient generateAuthenticatedClient(
      String userName, String password, String resourcePath) {
    Client client = buildPublicClient();
    client.addFilter(new HTTPBasicAuthFilter(userName, password));
    return new JerseyBaseClient(client, wsBaseUrl, resourcePath);
  }
}
