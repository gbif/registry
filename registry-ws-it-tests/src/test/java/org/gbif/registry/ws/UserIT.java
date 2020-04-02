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
package org.gbif.registry.ws;

import org.gbif.api.model.common.GbifUser;
import org.gbif.identity.mybatis.IdentitySuretyTestHelper;
import org.gbif.registry.domain.ws.AuthenticationDataParameters;
import org.gbif.registry.domain.ws.UserCreation;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.ws.fixtures.TestClient;
import org.gbif.registry.ws.fixtures.TestConstants;
import org.gbif.registry.ws.fixtures.UserTestFixture;
import org.gbif.ws.security.JerseyGbifAuthService;
import org.gbif.ws.util.SecurityConstants;

import java.io.IOException;
import java.util.function.Function;

import javax.ws.rs.core.Response;

import org.assertj.core.util.Strings;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import static org.gbif.registry.ws.util.AssertHttpResponse.assertResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests related to the user (identity) module representing actions a user can initiate
 * by himself.
 *
 * <p>Due to the fact that login and changePassword are not directly available in the Java ws
 * client, most of the tests use a direct HTTP client.
 */
public class UserIT extends PlainAPIBaseIT {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private TestClient testClient;

  private JerseyGbifAuthService gbifAuthService;

  private static final Function<WebResource, WebResource> LOGIN_RESOURCE_FCT =
      (wr) -> wr.path("login");

  private UserTestFixture userTestFixture;

  @LocalServerPort private int localServerPort;

  @Autowired
  public UserIT(
      IdentityService identityService,
      IdentitySuretyTestHelper identitySuretyTestHelper,
      JerseyGbifAuthService gbifAuthService) {
    testClient = new TestClient(TestConstants.getRegistryServerURL(localServerPort));
    userTestFixture = new UserTestFixture(identityService, identitySuretyTestHelper);
    this.gbifAuthService = gbifAuthService;
  }

  /**
   * Only used to make sure we can NOT use the appkey
   *
   * @return
   */
  @Override
  protected JerseyGbifAuthService getAuthService() {
    return gbifAuthService;
  }

  @Override
  protected String getResourcePath() {
    return UserTestFixture.USER_RESOURCE_PATH;
  }

  @Override
  protected String getUsername() {
    return UserTestFixture.USERNAME;
  }

  @Override
  protected String getPassword() {
    return UserTestFixture.PASSWORD;
  }

  @Override
  protected void onSetup() {
    // no-op
  }

  @Test
  public void testLoginNoCredentials() {
    // GET login
    ClientResponse cr = getPublicClient().get(LOGIN_RESOURCE_FCT);
    assertResponse(Response.Status.UNAUTHORIZED, cr);

    // POST login
    cr = getPublicClient().post(LOGIN_RESOURCE_FCT, null);
    assertResponse(Response.Status.UNAUTHORIZED, cr);
  }

  @Test
  public void testLoginGet() throws IOException {
    GbifUser user = userTestFixture.prepareUser();
    ClientResponse cr = getAuthenticatedClient().get(LOGIN_RESOURCE_FCT);
    assertResponse(Response.Status.OK, cr);

    // check jwt token
    String body = cr.getEntity(String.class);
    JsonNode node = OBJECT_MAPPER.readTree(body);
    assertUserLogged(node, user);
    assertTrue(!Strings.isNullOrEmpty(node.get(SecurityConstants.HEADER_TOKEN).asText()));

    // try to login using the email instead of the username
    cr = testClient.login(user.getEmail(), getPassword());
    assertResponse(Response.Status.OK, cr);
  }

  @Test
  public void testLoginPost() throws IOException {
    GbifUser user = userTestFixture.prepareUser();
    ClientResponse cr = getAuthenticatedClient().post(LOGIN_RESOURCE_FCT, null);
    assertResponse(Response.Status.CREATED, cr);

    // check jwt token
    String body = cr.getEntity(String.class);
    JsonNode node = OBJECT_MAPPER.readTree(body);
    assertUserLogged(node, user);
    assertTrue(!Strings.isNullOrEmpty(node.get(SecurityConstants.HEADER_TOKEN).asText()));

    // try to login using the email instead of the username
    cr = testClient.loginPost(user.getEmail(), getPassword());
    assertResponse(Response.Status.CREATED, cr);
  }

  @Test
  public void testChangePassword() {
    userTestFixture.prepareUser();

    final String newPassword = "123456";
    AuthenticationDataParameters params = new AuthenticationDataParameters();
    params.setPassword(newPassword);
    ClientResponse cr = getAuthenticatedClient().put(uri -> uri.path("changePassword"), params);
    assertResponse(Response.Status.NO_CONTENT, cr);

    // try to login using the previous password
    cr = getAuthenticatedClient().get(LOGIN_RESOURCE_FCT);
    assertResponse(Response.Status.UNAUTHORIZED, cr);

    // try with the new password
    cr = testClient.login(UserTestFixture.USERNAME, newPassword);
    assertResponse(Response.Status.OK, cr);
  }

  /**
   * The login endpoint only accepts HTTP Basic request. Application that uses appkeys are trusted.
   */
  @Test
  public void testLoginWithAppKeys() {
    GbifUser user = userTestFixture.prepareUser();
    ClientResponse cr =
        getWithSignedRequest(user.getUserName(), (uriBuilder -> uriBuilder.path("login")));
    assertResponse(Response.Status.FORBIDDEN, cr);
  }

  @Test
  public void testWhoAmI() throws IOException {
    // create test user
    UserCreation userCreation = userTestFixture.generateUser(UserTestFixture.USERNAME);
    GbifUser user = userTestFixture.prepareUser(userCreation);

    ClientResponse cr = getAuthenticatedClient().post(wr -> wr.path("whoami"), null);
    assertResponse(Response.Status.CREATED, cr);
    String body = cr.getEntity(String.class);
    assertUserLogged(OBJECT_MAPPER.readTree(body), user);
  }

  private void assertUserLogged(JsonNode node, GbifUser user) {
    assertEquals(user.getUserName(), node.get("userName").asText());
    assertEquals(user.getEmail(), node.get("email").asText());
    assertEquals(user.getFirstName(), node.get("firstName").asText());
    assertTrue(node.has("roles"));
    ArrayNode roles = (ArrayNode) node.get("roles");
    assertEquals(user.getRoles().size(), roles.size());
    assertTrue(node.has("editorRoleScopes"));
  }
}
