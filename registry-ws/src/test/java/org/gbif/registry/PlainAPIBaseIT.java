package org.gbif.registry;

import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.database.LiquibaseModules;
import org.gbif.registry.grizzly.RegistryServerWithIdentity;
import org.gbif.registry.utils.JerseyBaseClient;
import org.gbif.ws.security.GbifAuthService;

import java.net.URI;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

/**
 * This abstract class is used to test access to API endpoint using plain call (NOT using a Java ws-client).
 * If a ws-client exists for the API you are testing this class is probably not what you need.
 *
 */
public abstract class PlainAPIBaseIT {

  // Flushes the database on each run
  @ClassRule
  public static final LiquibaseInitializer liquibaseRule = new LiquibaseInitializer(LiquibaseModules.database());

  @ClassRule
  public static final RegistryServerWithIdentity registryServer = RegistryServerWithIdentity.INSTANCE;

  @Rule
  public final DatabaseInitializer databaseRule = new DatabaseInitializer(LiquibaseModules.database());

  protected final String wsBaseUrl;
  private JerseyBaseClient publicClient;
  private JerseyBaseClient authenticatedClient;

  protected PlainAPIBaseIT() {
    wsBaseUrl = TestConstants.getRegistryServerURL(registryServer.getPort());
  }

  @Before
  public void setupBase() {
    publicClient = new JerseyBaseClient(buildPublicClient(), wsBaseUrl, getResourcePath());
    authenticatedClient = new JerseyBaseClient(buildAuthenticatedClient(), wsBaseUrl, getResourcePath());
    onSetup();
  }

  @After
  public void destroyBase() {
    publicClient.destroy();
    authenticatedClient.destroy();
  }

  protected abstract GbifAuthService getAuthService();
  protected abstract String getResourcePath();
  protected abstract String getUsername();
  protected abstract String getPassword();

  protected abstract void onSetup();

  /**
   * Build a {@link Client} using default settings and no authentication.
   *
   * @return new {@link Client} instance
   */
  private Client buildPublicClient() {
    ClientConfig clientConfig = new DefaultClientConfig();
    clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    return Client.create(clientConfig);
  }

  /**
   * Build a {@link Client} using the result of {@link #getUsername()} and {@link #getPassword()} for authentication.
   * @return {@link Client}
   */
  private Client buildAuthenticatedClient() {
    Client client = buildPublicClient();
    client.addFilter(new HTTPBasicAuthFilter(getUsername(), getPassword()));
    return client;
  }

  public ClientRequest generatePostClientRequest() {
    return generatePostClientRequest(Function.identity());
  }

  /**
   * POST a signed request (signed using {@link #getAuthService()}.
   *
   * @param username   username to use to sign the request
   * @param uriBuilder function that receives a UriBuilder already pointing to wsBaseUrl/getResourcePath()
   *
   * @return
   */
  public ClientResponse postSignedRequest(String username, Function<UriBuilder, UriBuilder> uriBuilder) {
    return postSignedRequest(username, null, uriBuilder);
  }

  /**
   * POST a signed request (signed using {@link #getAuthService()} and an entity.
   *
   * @param username   username to use to sign the request
   * @param entity
   * @param uriBuilder function that receives a UriBuilder already pointing to wsBaseUrl/getResourcePath()
   *
   * @return
   */
  public ClientResponse postSignedRequest(String username, @Nullable Object entity, Function<UriBuilder, UriBuilder> uriBuilder) {
    ClientRequest clientRequest = generatePostClientRequest(uriBuilder);

    if(entity != null) {
      clientRequest.setEntity(entity);
    }
    //sign the request, we create users using the appKeys
    getAuthService().signRequest(username, clientRequest);
    return publicClient.getClient().handle(clientRequest);
  }

  /**
   * GET using a signed request (signed using {@link #getAuthService()} and return the result.
   *
   * @param username   username to use to sign the request
   * @param uriBuilder function that receives a UriBuilder already pointing to wsBaseUrl/getResourcePath()
   *
   * @return
   */
  public ClientResponse getWithSignedRequest(String username, Function<UriBuilder, UriBuilder> uriBuilder) {
    ClientRequest clientRequest = generateGetClientRequest(uriBuilder);
    //sign the request, we create users using the appKeys
    getAuthService().signRequest(username, clientRequest);
    return publicClient.getClient().handle(clientRequest);
  }

  /**
   * Generate a ClientRequest POST using a {@link Function} to get the URL to point to.
   * @param uriBuilder
   * @return
   */
  private ClientRequest generatePostClientRequest(Function<UriBuilder, UriBuilder> uriBuilder) {
    return generateClientRequest(HttpMethod.POST, uriBuilder);
  }

  /**
   * Generate a ClientRequest GET using a {@link Function} to get the URL to point to.
   * @param uriBuilder
   * @return
   */
  private ClientRequest generateGetClientRequest(Function<UriBuilder, UriBuilder> uriBuilder) {
    return generateClientRequest(HttpMethod.GET, uriBuilder);
  }

  /**
   * Generate a ClientRequest using a {@link Function} to get the URL to point to.
   * @param httpMethod
   * @param uriBuilder
   * @return
   */
  private ClientRequest generateClientRequest(String httpMethod, Function<UriBuilder, UriBuilder> uriBuilder) {
    URI uri = uriBuilder.apply(UriBuilder.fromUri(wsBaseUrl).path(getResourcePath())).build();
    return ClientRequest.create().type(MediaType.APPLICATION_JSON_TYPE)
            .build(uri, httpMethod);
  }

  JerseyBaseClient getPublicClient() {
    return publicClient;
  }

  /**
   *
   * @return
   */
  JerseyBaseClient getAuthenticatedClient() {
    return authenticatedClient;
  }

  /**
   * Generate a new {@link JerseyBaseClient}
   * @param userName
   * @param password
   * @return
   */
  protected JerseyBaseClient generateAuthenticatedClient(String userName, String password) {
    Client client = buildPublicClient();
    client.addFilter(new HTTPBasicAuthFilter(userName, password));
    return new JerseyBaseClient(client, wsBaseUrl, getResourcePath());
  }


  public static MultivaluedMap<String, String> buildQueryParams(String key, String value){
    MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    queryParams.add(key, value);
    return queryParams;
  }

  public static MultivaluedMap<String, String> buildQueryParams(String key1, String value1, String key2, String value2){
    MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    queryParams.add(key1, value1);
    queryParams.add(key2, value2);
    return queryParams;
  }

}
