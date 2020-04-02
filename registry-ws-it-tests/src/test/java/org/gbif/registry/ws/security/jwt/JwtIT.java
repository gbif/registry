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
package org.gbif.registry.ws.security.jwt;

import org.gbif.api.model.collections.Person;
import org.gbif.registry.security.jwt.JwtConfiguration;
import org.gbif.registry.ws.jwt.JwtUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.assertj.core.util.Strings;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.web.server.LocalServerPort;

import com.google.common.hash.Hashing;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;

import static org.gbif.registry.ws.util.GrscicollUtils.GRSCICOLL_PATH;
import static org.gbif.ws.util.SecurityConstants.HEADER_TOKEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JwtIT {

  @LocalServerPort int localServerPort;

  @RegisterExtension
  static PreparedDbExtension database =
      EmbeddedPostgresExtension.preparedDatabase(
          LiquibasePreparer.forClasspathLocation("liquibase/master.xml"));

  @RegisterExtension
  // TODO: null
  public final JwtDatabaseInitializer databaseRule =
      new JwtDatabaseInitializer(database.getTestDatabase(), null);

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final Function<String, String> BASIC_AUTH_HEADER =
      username ->
          "Basic "
              + java.util.Base64.getEncoder()
                  .encodeToString(String.format("%s:%s", username, username).getBytes());

  private Client client;
  private JwtConfiguration jwtConfiguration;

  public JwtIT(JwtConfiguration jwtConfiguration) {
    this.jwtConfiguration = jwtConfiguration;
  }

  @Before
  public void setup() {
    // jersey client
    ClientConfig clientConfig = new DefaultClientConfig();
    clientConfig.getFeatures().put("com.sun.jersey.api.json.POJOMappingFeature", Boolean.TRUE);
    client = Client.create(clientConfig);
  }

  @Test
  public void validTokenTest() throws IOException {
    String token = login(JwtDatabaseInitializer.GRSCICOLL_ADMIN);

    WebResource personResource = getPersonResource();

    ClientResponse personResponse =
        personResource
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .type(MediaType.APPLICATION_JSON)
            .post(ClientResponse.class, createPerson());

    assertEquals(Response.Status.CREATED.getStatusCode(), personResponse.getStatus());
    assertNotNull(personResponse.getHeaders().get(HEADER_TOKEN));
    assertNotEquals(token, personResponse.getHeaders().get(HEADER_TOKEN));
  }

  @Test
  public void invalidHeaderTest() throws IOException {
    String token = login(JwtDatabaseInitializer.ADMIN_USER);

    ClientResponse personResponse =
        getPersonResource()
            .header(HttpHeaders.AUTHORIZATION, "beare " + token)
            .type(MediaType.APPLICATION_JSON)
            .post(ClientResponse.class, createPerson());

    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), personResponse.getStatus());
  }

  @Test
  public void invalidTokenTest() {
    JwtConfiguration config = new JwtConfiguration();
    config.setSigningKey(generateTestSigningKey("fake"));
    String token = JwtUtils.generateJwt(JwtDatabaseInitializer.ADMIN_USER, config);

    ClientResponse personResponse =
        getPersonResource()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .type(MediaType.APPLICATION_JSON)
            .post(ClientResponse.class, createPerson());

    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), personResponse.getStatus());
    assertNull(personResponse.getHeaders().get(HEADER_TOKEN));
  }

  @Test
  public void insufficientRolesTest() throws IOException {
    String token = login(JwtDatabaseInitializer.TEST_USER);

    ClientResponse personResponse =
        getPersonResource()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .type(MediaType.APPLICATION_JSON)
            .post(ClientResponse.class, createPerson());

    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), personResponse.getStatus());
  }

  @Test
  public void fakeUserTest() {
    String token = JwtUtils.generateJwt("fake", jwtConfiguration);

    ClientResponse personResponse =
        getPersonResource()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .type(MediaType.APPLICATION_JSON)
            .post(ClientResponse.class, createPerson());

    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), personResponse.getStatus());
  }

  @Test
  public void noJwtAndNoBasicAuthTest() {
    ClientResponse personResponse =
        getPersonResource()
            .type(MediaType.APPLICATION_JSON)
            .post(ClientResponse.class, createPerson());

    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), personResponse.getStatus());
  }

  @Test
  public void noJwtWithBasicAuthTest() {
    ClientResponse personResponse =
        getPersonResource()
            .header(
                HttpHeaders.AUTHORIZATION,
                BASIC_AUTH_HEADER.apply(JwtDatabaseInitializer.GRSCICOLL_ADMIN))
            .type(MediaType.APPLICATION_JSON)
            .post(ClientResponse.class, createPerson());

    assertEquals(Response.Status.CREATED.getStatusCode(), personResponse.getStatus());
  }

  /** Logs in a user and returns the JWT token. */
  private String login(String user) throws IOException {
    WebResource webResourceLogin =
        client.resource("http://localhost:" + localServerPort + "/user/login");

    ClientResponse loginResponse =
        webResourceLogin
            .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER.apply(user))
            .accept(MediaType.APPLICATION_JSON)
            .post(ClientResponse.class);

    assertEquals(Response.Status.CREATED.getStatusCode(), loginResponse.getStatus());

    String body = loginResponse.getEntity(String.class);
    String token = OBJECT_MAPPER.readTree(body).get(HEADER_TOKEN).asText();
    assertTrue(!Strings.isNullOrEmpty(token));

    return token;
  }

  private WebResource getPersonResource() {
    return client.resource(
        "http://localhost:" + localServerPort + "/" + GRSCICOLL_PATH + "/person");
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
