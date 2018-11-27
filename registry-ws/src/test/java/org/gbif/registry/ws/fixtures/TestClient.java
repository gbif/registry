package org.gbif.registry.ws.fixtures;

import org.gbif.registry.ws.util.JerseyBaseClient;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;

import static org.gbif.registry.ws.fixtures.UserTestFixture.USER_RESOURCE_PATH;

/**
 * Generates and offer utilities related to authenticated client in the context of testing.
 *
 */
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
    clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    return Client.create(clientConfig);
  }

  /**
   * Build a {@link Client} using default settings and Basic authentication
   *
   * @param username
   * @param password
   *
   * @return new {@link Client} instance
   */
  public static Client buildAuthenticatedClient(String username, String password) {
    Client client = buildPublicClient();
    client.addFilter(new HTTPBasicAuthFilter(username, password));
    return client;
  }

  /**
   * This client will only work against a Grizzly server started from {@link org.gbif.registry.grizzly.RegistryServer}.
   * @return
   */
  public static Client buildAuthenticatedAdmin() {
    Client client = buildPublicClient();
    client.addFilter(new HTTPBasicAuthFilter(TestConstants.TEST_ADMIN, TestConstants.TEST_ADMIN));
    return client;
  }

  /**
   * Call the login endpoint using a HTTP Basic authentication.
   * @param username
   * @param password
   * @return
   */
  public ClientResponse login(String username, String password) {
    return generateAuthenticatedClient(username, password,
            USER_RESOURCE_PATH).get(wr -> wr.path("login"));
  }

  /**
   * Call the login endpoint using a HTTP Basic authentication.
   * @param username
   * @param password
   * @return
   */
  public ClientResponse loginPost(String username, String password) {
    return generateAuthenticatedClient(username, password,
                                       USER_RESOURCE_PATH).post(wr -> wr.path("login"), null);
  }

  /**
   * Generate a new {@link JerseyBaseClient} for another resource than the one defined by this test.
   *
   * @param userName
   * @param password
   * @param resourcePath
   * @return
   */
  public JerseyBaseClient generateAuthenticatedClient(String userName, String password, String resourcePath) {
    Client client = buildPublicClient();
    client.addFilter(new HTTPBasicAuthFilter(userName, password));
    return new JerseyBaseClient(client, wsBaseUrl, resourcePath);
  }
}
