package org.gbif.registry;

import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.database.LiquibaseModules;
import org.gbif.registry.grizzly.RegistryServerWithIdentity;
import org.gbif.registry.ws.fixtures.TestClient;
import org.gbif.registry.ws.fixtures.TestConstants;
import org.gbif.registry.ws.util.JerseyBaseClient;
import org.gbif.ws.security.GbifAuthService;

import java.net.URI;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import static org.gbif.registry.ws.fixtures.TestClient.buildPublicClient;

/**
 * This abstract class is used to test access to API endpoint using plain call (NOT using a Java ws-client).
 * If a ws-client exists for the API you are testing this class is probably not what you need.
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
    authenticatedClient = new JerseyBaseClient(
            TestClient.buildAuthenticatedClient(getUsername(), getPassword()),
            wsBaseUrl, getResourcePath());
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
    return postSignedRequest(getAuthService(), username, entity, uriBuilder);
  }

  protected ClientResponse postSignedRequest(GbifAuthService authService, String username, @Nullable Object entity, Function<UriBuilder, UriBuilder> uriBuilder) {
    ClientRequest clientRequest = generatePostClientRequest(uriBuilder);

    if (entity != null) {
      clientRequest.setEntity(entity);
    }
    //sign the request, we create users using the appKeys
    authService.signRequest(username, clientRequest);
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
  public ClientResponse getWithSignedRequest(String username, Function<UriBuilder, UriBuilder> uriBuilder,
                                             Map<String, String> queryParameters) {
    ClientRequest clientRequest = generateGetClientRequest(uriBuilder, queryParameters);
    //sign the request, we create users using the appKeys
    getAuthService().signRequest(username, clientRequest);
    return publicClient.getClient().handle(clientRequest);
  }

  public ClientResponse getWithSignedRequest(String username, Function<UriBuilder, UriBuilder> uriBuilder) {
    return getWithSignedRequest(username, uriBuilder, null);
  }

  public ClientResponse putWithSignedRequest(String username, @Nullable Object entity, Function<UriBuilder, UriBuilder> uriBuilder) {
    ClientRequest clientRequest = generatePutClientRequest(uriBuilder);
    if (entity != null) {
      clientRequest.setEntity(entity);
    }
    //sign the request, we create users using the appKeys
    getAuthService().signRequest(username, clientRequest);
    return publicClient.getClient().handle(clientRequest);
  }

  public ClientResponse deleteWithSignedRequest(String username, Function<UriBuilder, UriBuilder> uriBuilder) {
    ClientRequest clientRequest = generateDeleteClientRequest(uriBuilder);
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
    return generateClientRequest(HttpMethod.POST, uriBuilder, null);
  }

  /**
   * Generate a ClientRequest GET using a {@link Function} to get the URL to point to.
   * @param uriBuilder
   * @return
   */
  private ClientRequest generateGetClientRequest(Function<UriBuilder, UriBuilder> uriBuilder,
                                                 Map<String, String> queryParameters) {
    return generateClientRequest(HttpMethod.GET, uriBuilder, queryParameters);
  }

  private ClientRequest generatePutClientRequest(Function<UriBuilder, UriBuilder> uriBuilder) {
    return generateClientRequest(HttpMethod.PUT, uriBuilder, null);
  }

  private ClientRequest generateDeleteClientRequest(Function<UriBuilder, UriBuilder> uriBuilder) {
    return generateClientRequest(HttpMethod.DELETE, uriBuilder, null);
  }

  /**
   * Generate a ClientRequest using a {@link Function} to get the URL to point to.
   * @param httpMethod
   * @param uriBuilderFct
   * @param queryParameters optional, can be null
   * @return
   */
  private ClientRequest generateClientRequest(String httpMethod, Function<UriBuilder, UriBuilder> uriBuilderFct,
                                              Map<String, String> queryParameters) {

    UriBuilder uriBuilder = uriBuilderFct.apply(UriBuilder.fromUri(wsBaseUrl).path(getResourcePath()));
    if(queryParameters != null) {
      for( Map.Entry<String, String> entry : queryParameters.entrySet()) {
        uriBuilder = uriBuilder.queryParam(entry.getKey(), entry.getValue());
      }
    }

    URI uri = uriBuilder.build();
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

}
