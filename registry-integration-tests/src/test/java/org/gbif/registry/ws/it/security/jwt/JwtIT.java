/*
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

import org.gbif.api.model.collections.Institution;
import org.gbif.registry.identity.model.ExtendedLoggedUser;
import org.gbif.registry.search.test.EsManageServer;
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

  private static final String PATH = "/grscicoll/institution";

  private JwtConfiguration jwtConfiguration;
  private final RequestTestFixture requestTestFixture;

  @RegisterExtension
  public static JwtDatabaseInitializer databaseRule =
      new JwtDatabaseInitializer(database.getPostgresContainer());

  @Autowired
  public JwtIT(
      JwtConfiguration jwtConfiguration,
      RequestTestFixture requestTestFixture,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer) {
    super(principalProvider, esServer);
    this.jwtConfiguration = jwtConfiguration;
    this.requestTestFixture = requestTestFixture;
  }

  @Test
  public void validTokenTest() throws Exception {
    String token = login(GRSCICOLL_ADMIN);

    // otherwise the service may issue the same token because of the same time (seconds)
    Thread.sleep(1000);

    ResultActions actions =
        requestTestFixture
            .postRequest(token, createInstitution(), PATH)
            .andExpect(status().isCreated());

    String newToken = requestTestFixture.getHeader(actions, HEADER_TOKEN);

    assertNotNull(newToken);
    assertNotEquals(token, newToken);
  }

  @Test
  public void invalidHeaderTest() throws Exception {
    String token = login(ADMIN_USER);
    HttpHeaders headers = new HttpHeaders();
    headers.add("beare ", token);

    requestTestFixture
        .postRequest(headers, createInstitution(), PATH)
        .andExpect(status().isForbidden());
  }

  @Test
  public void invalidTokenTest() throws Exception {
    JwtConfiguration config = new JwtConfiguration();
    config.setSigningKey("fake");
    String token = JwtUtils.generateJwt(ADMIN_USER, config);

    ResultActions actions =
        requestTestFixture
            .postRequest(token, createInstitution(), PATH)
            .andExpect(status().isForbidden());

    String newToken = requestTestFixture.getHeader(actions, HEADER_TOKEN);

    assertNull(newToken);
  }

  @Test
  public void insufficientRolesTest() throws Exception {
    String token = login(TEST_USER);

    requestTestFixture
        .postRequest(token, createInstitution(), PATH)
        .andExpect(status().isForbidden());
  }

  @Test
  public void fakeUserTest() throws Exception {
    String token = JwtUtils.generateJwt("fake", jwtConfiguration);

    requestTestFixture
        .postRequest(token, createInstitution(), PATH)
        .andExpect(status().isForbidden());
  }

  @Test
  public void noJwtAndNoBasicAuthTest() throws Exception {
    requestTestFixture.postRequest(createInstitution(), PATH).andExpect(status().isForbidden());
  }

  @Test
  public void noJwtWithBasicAuthTest() throws Exception {
    requestTestFixture
        .postRequest(GRSCICOLL_ADMIN, GRSCICOLL_ADMIN, createInstitution(), PATH)
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

  private Institution createInstitution() {
    Institution newInstitution = new Institution();
    newInstitution.setCode("code");
    newInstitution.setName("name");
    newInstitution.setCreatedBy("Test");
    newInstitution.setModifiedBy("Test");
    return newInstitution;
  }
}
