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
package org.gbif.registry.ws.it;

import org.gbif.api.model.common.GbifUser;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.domain.ws.AuthenticationDataParameters;
import org.gbif.registry.domain.ws.UserCreation;
import org.gbif.registry.identity.model.ExtendedLoggedUser;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.it.fixtures.RequestTestFixture;
import org.gbif.registry.ws.it.fixtures.UserTestFixture;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Collections;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import static org.gbif.registry.ws.it.fixtures.UserTestFixture.EMAIL;
import static org.gbif.registry.ws.it.fixtures.UserTestFixture.PASSWORD;
import static org.gbif.registry.ws.it.fixtures.UserTestFixture.USERNAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests related to the user (identity) module representing actions a user can initiate
 * by himself.
 *
 * <p>Due to the fact that login and changePassword are not directly available in the Java ws
 * client, most of the tests use a direct HTTP client.
 */
public class UserIT extends BaseItTest {

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = TestCaseDatabaseInitializer.builder()
    .dataSource(database.getTestDatabase())
    .tables(Collections.singletonList("public.user"))
    .build();

  private final UserTestFixture userTestFixture;
  private final RequestTestFixture requestTestFixture;

  @Autowired
  public UserIT(
      UserTestFixture userTestFixture,
      RequestTestFixture requestTestFixture,
      @Nullable SimplePrincipalProvider simplePrincipalProvider,
      EsManageServer esServer) {
    super(simplePrincipalProvider, esServer);
    this.userTestFixture = userTestFixture;
    this.requestTestFixture = requestTestFixture;
  }

  @Test
  public void testLoginNoCredentials() throws Exception {
    // GET login
    requestTestFixture.getRequest("/user/login").andExpect(status().isUnauthorized());

    // POST login
    requestTestFixture.postRequest("/user/login").andExpect(status().isUnauthorized());
  }

  @Test
  public void testLoginGet() throws Exception {
    GbifUser user = userTestFixture.prepareUser();

    ResultActions actions =
        requestTestFixture.getRequest(USERNAME, PASSWORD, "/user/login").andExpect(status().isOk());

    // check jwt token
    ExtendedLoggedUser loggedUser =
        requestTestFixture.extractJsonResponse(actions, ExtendedLoggedUser.class);

    assertUserLogged(user, loggedUser);
    assertNotNull(loggedUser.getToken());

    // try to login using the email instead of the username
    requestTestFixture.getRequest(EMAIL, PASSWORD, "/user/login").andExpect(status().isOk());
  }

  @Test
  public void testLoginPost() throws Exception {
    GbifUser user = userTestFixture.prepareUser();

    ResultActions actions =
        requestTestFixture
            .postRequest(USERNAME, PASSWORD, "/user/login")
            .andExpect(status().isCreated());

    // check jwt token
    ExtendedLoggedUser loggedUser =
        requestTestFixture.extractJsonResponse(actions, ExtendedLoggedUser.class);

    assertUserLogged(user, loggedUser);
    assertNotNull(loggedUser.getToken());

    // try to login using the email instead of the username
    requestTestFixture.postRequest(EMAIL, PASSWORD, "/user/login").andExpect(status().isCreated());
  }

  @Test
  public void testChangePassword() throws Exception {
    userTestFixture.prepareUser();

    final String newPassword = "123456";
    AuthenticationDataParameters params = new AuthenticationDataParameters();
    params.setPassword(newPassword);

    requestTestFixture
        .putRequest(USERNAME, PASSWORD, params, "/user/changePassword")
        .andExpect(status().isNoContent());

    // try to login using the previous password
    requestTestFixture
        .getRequest(USERNAME, PASSWORD, "/user/login")
        .andExpect(status().isUnauthorized());

    // try with the new password
    requestTestFixture.getRequest(USERNAME, newPassword, "/user/login").andExpect(status().isOk());
  }

  /**
   * The login endpoint only accepts HTTP Basic request. Application that uses appkeys are trusted.
   */
  @Test
  public void testLoginWithAppKeys() throws Exception {
    userTestFixture.prepareUser();

    requestTestFixture.getSignedRequest(USERNAME, "/user/login").andExpect(status().isForbidden());
  }

  @Test
  public void testWhoAmI() throws Exception {
    // create test user
    UserCreation userCreation = UserTestFixture.generateUser(USERNAME);
    GbifUser user = userTestFixture.prepareUser(userCreation);

    ResultActions actions =
        requestTestFixture
            .postRequest(USERNAME, PASSWORD, "/user/whoami")
            .andExpect(status().isCreated());

    ExtendedLoggedUser loggedUser =
        requestTestFixture.extractJsonResponse(actions, ExtendedLoggedUser.class);

    assertUserLogged(user, loggedUser);
  }

  private void assertUserLogged(GbifUser user, ExtendedLoggedUser loggedUser) {
    assertEquals(user.getUserName(), loggedUser.getUserName());
    assertEquals(user.getEmail(), loggedUser.getEmail());
    assertEquals(user.getFirstName(), loggedUser.getFirstName());
    assertIterableEquals(
        user.getRoles().stream().map(Enum::toString).collect(Collectors.toSet()),
        loggedUser.getRoles());
  }
}
