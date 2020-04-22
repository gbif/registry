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
package org.gbif.registry.ws.it.security.jwt;

import org.gbif.api.model.collections.Person;
import org.gbif.registry.identity.model.ExtendedLoggedUser;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.security.jwt.JwtConfiguration;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.registry.ws.it.fixtures.RequestTestFixture;
import org.gbif.registry.ws.jwt.JwtUtils;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

import static org.gbif.registry.ws.it.security.jwt.JwtDatabaseInitializer.ADMIN_USER;
import static org.gbif.registry.ws.it.security.jwt.JwtDatabaseInitializer.GRSCICOLL_ADMIN;
import static org.gbif.registry.ws.it.security.jwt.JwtDatabaseInitializer.TEST_USER;
import static org.gbif.ws.util.SecurityConstants.HEADER_TOKEN;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class JwtIT extends BaseItTest {

  private static final String PATH = "/grscicoll/person";

  private JwtConfiguration jwtConfiguration;
  private final RequestTestFixture requestTestFixture;

  @RegisterExtension public JwtDatabaseInitializer databaseRule;

  @Autowired
  public JwtIT(
      JwtConfiguration jwtConfiguration,
      RequestTestFixture requestTestFixture,
      IdentityService identityService,
      SimplePrincipalProvider principalProvider) {
    super(principalProvider);
    this.jwtConfiguration = jwtConfiguration;
    this.requestTestFixture = requestTestFixture;
    databaseRule = new JwtDatabaseInitializer(identityService);
  }

  @Test
  public void validTokenTest() throws Exception {
    String token = login(GRSCICOLL_ADMIN);

    // otherwise the service may issue the same token because of the same time (seconds)
    Thread.sleep(1000);

    ResultActions actions =
        requestTestFixture.postRequest(token, createPerson(), PATH).andExpect(status().isCreated());

    String newToken = requestTestFixture.getHeader(actions, HEADER_TOKEN);

    assertNotNull(newToken);
    assertNotEquals(token, newToken);
  }

  @Test
  public void invalidHeaderTest() throws Exception {
    String token = login(ADMIN_USER);
    HttpHeaders headers = new HttpHeaders();
    headers.add("beare ", token);

    requestTestFixture.postRequest(headers, createPerson(), PATH).andExpect(status().isForbidden());
  }

  @Test
  public void invalidTokenTest() throws Exception {
    JwtConfiguration config = new JwtConfiguration();
    config.setSigningKey("fake");
    String token = JwtUtils.generateJwt(ADMIN_USER, config);

    ResultActions actions =
        requestTestFixture
            .postRequest(token, createPerson(), PATH)
            .andExpect(status().isForbidden());

    String newToken = requestTestFixture.getHeader(actions, HEADER_TOKEN);

    assertNull(newToken);
  }

  @Test
  public void insufficientRolesTest() throws Exception {
    String token = login(TEST_USER);

    requestTestFixture.postRequest(token, createPerson(), PATH).andExpect(status().isForbidden());
  }

  @Test
  public void fakeUserTest() throws Exception {
    String token = JwtUtils.generateJwt("fake", jwtConfiguration);

    requestTestFixture.postRequest(token, createPerson(), PATH).andExpect(status().isForbidden());
  }

  @Test
  public void noJwtAndNoBasicAuthTest() throws Exception {
    requestTestFixture.postRequest(createPerson(), PATH).andExpect(status().isForbidden());
  }

  @Test
  public void noJwtWithBasicAuthTest() throws Exception {
    requestTestFixture
        .postRequest(GRSCICOLL_ADMIN, GRSCICOLL_ADMIN, createPerson(), PATH)
        .andExpect(status().isCreated());
  }

  /** Logs in a user and returns the JWT token. */
  private String login(String user) throws Exception {
    ResultActions actions =
        requestTestFixture.postRequest(user, user, "/user/login").andExpect(status().isCreated());

    ExtendedLoggedUser response =
        requestTestFixture.extractJsonResponse(actions, ExtendedLoggedUser.class);

    assertNotNull(response.getToken());
    assertFalse(response.getToken().isEmpty());

    return response.getToken();
  }

  private Person createPerson() {
    Person newPerson = new Person();
    newPerson.setFirstName("first name");
    newPerson.setCreatedBy("Test");
    newPerson.setModifiedBy("Test");
    return newPerson;
  }
}
