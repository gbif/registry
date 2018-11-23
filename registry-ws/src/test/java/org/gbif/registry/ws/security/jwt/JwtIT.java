package org.gbif.registry.ws.security.jwt;

import org.gbif.api.model.collections.Person;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.database.LiquibaseModules;
import org.gbif.registry.grizzly.RegistryServer;
import org.gbif.registry.grizzly.RegistryServerWithIdentity;
import org.gbif.registry.guice.RegistryTestModules;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.hash.Hashing;
import com.google.inject.Injector;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import org.assertj.core.util.Strings;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JwtIT {

  @ClassRule
  public static final LiquibaseInitializer liquibaseRule = new LiquibaseInitializer(LiquibaseModules.database());

  @ClassRule
  public static final RegistryServerWithIdentity registryServer = RegistryServerWithIdentity.INSTANCE;

  @Rule
  public final JwtDatabaseInitializer databaseRule = new JwtDatabaseInitializer(LiquibaseModules.database());

  private static final Function<String, String> BASIC_AUTH_HEADER = username -> "Basic " + java.util.Base64.getEncoder()
    .encodeToString(String.format("%s:%s", username, username).getBytes());

  private Client client;
  private JwtConfiguration jwtConfiguration;

  @Before
  public void setup() {
    // jersey client
    ClientConfig clientConfig = new DefaultClientConfig();
    clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    client = Client.create(clientConfig);

    // jwt config
    Injector inj = RegistryTestModules.webservice();
    jwtConfiguration = inj.getInstance(JwtConfiguration.class);
  }

  @Test
  public void validTokenTest() {
    String token = login(JwtDatabaseInitializer.ADMIN_USER);

    WebResource personResource = getPersonResource();

    ClientResponse personResponse = personResource.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
      .type(MediaType.APPLICATION_JSON)
      .post(ClientResponse.class, createPerson());

    assertEquals(Response.Status.CREATED.getStatusCode(), personResponse.getStatus());

    personResponse = personResource.header(HttpHeaders.AUTHORIZATION, "bearer" + token)
      .type(MediaType.APPLICATION_JSON)
      .post(ClientResponse.class, createPerson());

    assertEquals(Response.Status.CREATED.getStatusCode(), personResponse.getStatus());
  }

  @Test
  public void invalidHeaderTest() {
    String token = login(JwtDatabaseInitializer.ADMIN_USER);

    ClientResponse personResponse = getPersonResource().header(HttpHeaders.AUTHORIZATION, "beare " + token)
      .type(MediaType.APPLICATION_JSON)
      .post(ClientResponse.class, createPerson());

    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), personResponse.getStatus());
  }

  @Test
  public void invalidTokenTest() {
    JwtConfiguration config = JwtConfiguration.newBuilder().signingKey(generateTestSigningKey("fake")).build();
    String token = JwtUtils.generateJwt(JwtDatabaseInitializer.ADMIN_USER, config);

    ClientResponse personResponse = getPersonResource().header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
      .type(MediaType.APPLICATION_JSON)
      .post(ClientResponse.class, createPerson());

    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), personResponse.getStatus());
  }

  @Test
  public void insufficientRolesTest() {
    String token = login(JwtDatabaseInitializer.TEST_USER);

    WebResource webResourcePerson = client.resource("http://localhost:" + RegistryServer.getPort() + "/grbio/person");

    ClientResponse personResponse = webResourcePerson.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
      .type(MediaType.APPLICATION_JSON)
      .post(ClientResponse.class, createPerson());

    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), personResponse.getStatus());
  }

  @Test
  public void fakeUserTest() {
    String token = JwtUtils.generateJwt("fake", jwtConfiguration);

    ClientResponse personResponse = getPersonResource().header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
      .type(MediaType.APPLICATION_JSON)
      .post(ClientResponse.class, createPerson());

    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), personResponse.getStatus());
  }

  @Test
  public void noJwtAndNoBasicAuthTest() {
    ClientResponse personResponse =
      getPersonResource().type(MediaType.APPLICATION_JSON).post(ClientResponse.class, createPerson());

    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), personResponse.getStatus());
  }

  @Test
  public void noJwtWithBasicAuthTest() {
    ClientResponse personResponse =
      getPersonResource().header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER.apply(JwtDatabaseInitializer.ADMIN_USER))
        .type(MediaType.APPLICATION_JSON)
        .post(ClientResponse.class, createPerson());

    assertEquals(Response.Status.CREATED.getStatusCode(), personResponse.getStatus());
  }

  /**
   * Logs in a user and returns the JWT token.
   */
  private String login(String user) {
    WebResource webResourceLogin = client.resource("http://localhost:" + RegistryServer.getPort() + "/user/login");

    ClientResponse loginResponse = webResourceLogin.
      header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER.apply(user)).
      get(ClientResponse.class);

    assertEquals(Response.Status.OK.getStatusCode(), loginResponse.getStatus());

    String authHeader = loginResponse.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    assertNotNull(authHeader);
    assertTrue(JwtUtils.containsBearer(authHeader));

    String token = JwtUtils.removeBearer(authHeader);
    assertTrue(!Strings.isNullOrEmpty(token));

    return token;
  }

  private WebResource getPersonResource() {
    return client.resource("http://localhost:" + RegistryServer.getPort() + "/grbio/person");
  }

  private Person createPerson() {
    Person newPerson = new Person();
    newPerson.setFirstName("first name");
    newPerson.setCreatedBy("Test");
    newPerson.setModifiedBy("Test");
    return newPerson;
  }

  private static String generateTestSigningKey(String string) {
    return Hashing.sha256().hashString(string, StandardCharsets.UTF_8).toString();
  }

}
