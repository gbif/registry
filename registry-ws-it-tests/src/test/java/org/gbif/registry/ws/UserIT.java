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
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.domain.ws.AuthenticationDataParameters;
import org.gbif.registry.domain.ws.UserCreation;
import org.gbif.registry.identity.model.ExtendedLoggedUser;
import org.gbif.registry.identity.mybatis.IdentitySuretyTestHelper;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.ws.fixtures.UserTestFixture;
import org.gbif.ws.security.RequestDataToSign;
import org.gbif.ws.security.SigningService;

import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;

import static org.gbif.registry.ws.fixtures.UserTestFixture.APP_KEY;
import static org.gbif.registry.ws.fixtures.UserTestFixture.USERNAME;
import static org.gbif.ws.util.SecurityConstants.GBIF_SCHEME;
import static org.gbif.ws.util.SecurityConstants.HEADER_GBIF_USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests related to the user (identity) module representing actions a user can initiate
 * by himself.
 *
 * <p>Due to the fact that login and changePassword are not directly available in the Java ws
 * client, most of the tests use a direct HTTP client.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = RegistryIntegrationTestsConfiguration.class)
@ContextConfiguration(initializers = {UserIT.ContextInitializer.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class UserIT {

  private SigningService signingService;
  private UserTestFixture userTestFixture;

  @Autowired private MockMvc mvc;

  @Autowired
  @Qualifier("registryObjectMapper")
  private ObjectMapper objectMapper;

  @RegisterExtension
  static PreparedDbExtension database =
      EmbeddedPostgresExtension.preparedDatabase(
          LiquibasePreparer.forClasspathLocation("liquibase/master.xml"));

  @RegisterExtension
  public final DatabaseInitializer databaseRule =
      new DatabaseInitializer(database.getTestDatabase());

  static class ContextInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      TestPropertyValues.of(dbTestPropertyPairs())
          .applyTo(configurableApplicationContext.getEnvironment());
      withSearchEnabled(false, configurableApplicationContext.getEnvironment());
    }

    protected static void withSearchEnabled(
        boolean enabled, ConfigurableEnvironment configurableEnvironment) {
      TestPropertyValues.of("searchEnabled=" + enabled).applyTo(configurableEnvironment);
    }

    protected String[] dbTestPropertyPairs() {
      return new String[] {
        "registry.datasource.url=jdbc:postgresql://localhost:"
            + database.getConnectionInfo().getPort()
            + "/"
            + database.getConnectionInfo().getDbName(),
        "registry.datasource.username=" + database.getConnectionInfo().getUser(),
        "registry.datasource.password="
      };
    }
  }

  @Autowired
  public UserIT(
      SigningService signingService,
      IdentityService identityService,
      IdentitySuretyTestHelper identitySuretyTestHelper) {
    this.signingService = signingService;
    this.userTestFixture = new UserTestFixture(identityService, identitySuretyTestHelper);
  }

  @Test
  public void testLoginNoCredentials() throws Exception {
    // GET login
    mvc.perform(get("/user/login")).andExpect(status().isUnauthorized());

    // POST login
    mvc.perform(post("/user/login")).andExpect(status().isUnauthorized());
  }

  @Test
  public void testLoginGet() throws Exception {
    GbifUser user = userTestFixture.prepareUser();

    ResultActions actions =
        mvc.perform(get("/user/login").with(httpBasic("user_12", "password")))
            .andExpect(status().isOk());

    // check jwt token
    String contentAsString = actions.andReturn().getResponse().getContentAsString();
    ExtendedLoggedUser loggedUser =
        objectMapper.readValue(contentAsString, ExtendedLoggedUser.class);

    assertUserLogged(user, loggedUser);
    assertNotNull(loggedUser.getToken());

    // try to login using the email instead of the username
    mvc.perform(get("/user/login").with(httpBasic("user_12@gbif.org", "password")))
        .andExpect(status().isOk());
  }

  @Test
  public void testLoginPost() throws Exception {
    GbifUser user = userTestFixture.prepareUser();

    ResultActions actions =
        mvc.perform(post("/user/login").with(httpBasic("user_12", "password")))
            .andExpect(status().isCreated());

    // check jwt token
    String contentAsString = actions.andReturn().getResponse().getContentAsString();
    ExtendedLoggedUser loggedUser =
        objectMapper.readValue(contentAsString, ExtendedLoggedUser.class);

    assertUserLogged(user, loggedUser);
    assertNotNull(loggedUser.getToken());

    // try to login using the email instead of the username
    mvc.perform(post("/user/login").with(httpBasic("user_12@gbif.org", "password")))
        .andExpect(status().isCreated());
  }

  @Test
  public void testChangePassword() throws Exception {
    userTestFixture.prepareUser();

    final String newPassword = "123456";
    AuthenticationDataParameters params = new AuthenticationDataParameters();
    params.setPassword(newPassword);
    mvc.perform(
            put("/user/changePassword")
                .content(objectMapper.writeValueAsString(params))
                .contentType(MediaType.APPLICATION_JSON)
                .with(httpBasic("user_12", "password")))
        .andExpect(status().isNoContent());

    // try to login using the previous password
    mvc.perform(get("/user/login").with(httpBasic("user_12", "password")))
        .andExpect(status().isUnauthorized());

    // try with the new password
    mvc.perform(get("/user/login").with(httpBasic("user_12", "123456"))).andExpect(status().isOk());
  }

  /**
   * The login endpoint only accepts HTTP Basic request. Application that uses appkeys are trusted.
   */
  @Test
  public void testLoginWithAppKeys() throws Exception {
    userTestFixture.prepareUser();

    RequestDataToSign requestDataToSign = new RequestDataToSign();
    requestDataToSign.setMethod(RequestMethod.GET.name());
    requestDataToSign.setUser(USERNAME);
    requestDataToSign.setUrl("/user/login");

    String signature = signingService.buildSignature(requestDataToSign, APP_KEY);

    mvc.perform(
            get("/user/login")
                .header(HttpHeaders.AUTHORIZATION, GBIF_SCHEME + " " + APP_KEY + ":" + signature)
                .header(HEADER_GBIF_USER, USERNAME))
        .andExpect(status().isForbidden());
  }

  @Test
  public void testWhoAmI() throws Exception {
    // create test user
    UserCreation userCreation = UserTestFixture.generateUser(USERNAME);
    GbifUser user = userTestFixture.prepareUser(userCreation);

    ResultActions actions =
        mvc.perform(post("/user/whoami").with(httpBasic("user_12", "password")))
            .andExpect(status().isCreated());

    String contentAsString = actions.andReturn().getResponse().getContentAsString();
    ExtendedLoggedUser loggedUser =
        objectMapper.readValue(contentAsString, ExtendedLoggedUser.class);

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
