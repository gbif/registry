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
package org.gbif.registry.ws.resources.usermanagement;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.registry.ConfirmationKeyParameter;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.domain.ws.AuthenticationDataParameters;
import org.gbif.registry.domain.ws.UserCreation;
import org.gbif.registry.persistence.mapper.UserMapper;
import org.gbif.registry.persistence.mapper.surety.ChallengeCodeMapper;
import org.gbif.utils.file.properties.PropertiesUtil;
import org.gbif.ws.security.Md5EncodeService;
import org.gbif.ws.security.SigningService;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.gbif.registry.utils.Users.prepareUserCreation;
import static org.gbif.registry.ws.fixtures.TestConstants.TEST_ADMIN;
import static org.gbif.registry.ws.fixtures.TestConstants.TEST_PASSWORD;
import static org.gbif.ws.util.SecurityConstants.HEADER_CONTENT_MD5;
import static org.gbif.ws.util.SecurityConstants.HEADER_GBIF_USER;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@SpringBootTest(
    classes = {RegistryIntegrationTestsConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class UserManagementTestSteps {

  private ResultActions result;
  private UserCreation user;
  private Properties secrets;
  private UUID challengeCode;
  private Map<String, String> credentials = new HashMap<>();

  @Value("${appkeys.file}")
  private String appkeysFile;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private UserMapper userMapper;

  @Autowired private ChallengeCodeMapper challengeCodeMapper;

  @Autowired private SigningService signingService;

  @Autowired private Md5EncodeService md5EncodeService;

  private MockMvc mvc;

  @Autowired private WebApplicationContext context;

  @Autowired private DataSource ds;

  private Connection connection;

  @Before("@UserManagement")
  public void setUp() throws Exception {
    connection = ds.getConnection();
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/usermanagement/user_management_cleanup.sql"));
    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/usermanagement/user_management_prepare.sql"));

    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

    secrets = PropertiesUtil.loadProperties(appkeysFile);
  }

  @After(value = "@UserCRUD", order = 1)
  public void cleanAfterUserCrud() {
    Objects.requireNonNull(connection, "Connection must not be null");
    ScriptUtils.executeSqlScript(
        connection,
        new ClassPathResource("/scripts/usermanagement/user_management_crud_cleanup.sql"));
  }

  @After(value = "@UserManagement", order = 0)
  public void tearDown() throws Exception {
    Objects.requireNonNull(connection, "Connection must not be null");
    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/usermanagement/user_management_cleanup.sql"));

    connection.close();
  }

  @Given("user {string}")
  public void prepareUser(String username) {
    // data prepared by scripts
  }

  @When("create new/existing user {string} with password {string} by APP role {string}")
  public void createNewUser(String username, String password, String appRole) throws Exception {
    user = prepareUserCreation(username, password);

    String userJsonString = objectMapper.writeValueAsString(user);

    // perform user creation and check response
    String contentMd5 = md5EncodeService.encode(userJsonString);
    String gbifAuthorization =
        getGbifAuthorization(
            POST,
            "/admin/user",
            APPLICATION_JSON,
            contentMd5,
            appRole,
            appRole,
            secrets.getProperty(appRole));
    result =
        mvc.perform(
            post("/admin/user")
                .content(userJsonString)
                .contentType(APPLICATION_JSON)
                .header(HEADER_CONTENT_MD5, contentMd5)
                .header(HEADER_GBIF_USER, appRole)
                .header(AUTHORIZATION, gbifAuthorization));
  }

  @When("delete user {string}")
  public void deleteUser(String username) throws Exception {
    GbifUser gbifUser = userMapper.get(username);

    result =
        mvc.perform(
            delete("/admin/user/{userKey}", gbifUser.getKey())
                .with(httpBasic(TEST_ADMIN, TEST_PASSWORD)));
  }

  @Then("response status should be {int}")
  public void assertResponseCode(int status) throws Exception {
    result.andExpect(status().is(status));
  }

  @Then("user {string} reflects the original one")
  public void assertCreatedUser(String username) throws Exception {
    // TODO: 25/10/2019 assert the whole result
    result
        .andExpect(jsonPath("$.username").value(username))
        .andExpect(jsonPath("$.email").value("user_14@gbif.org"))
        .andExpect(jsonPath("$.constraintViolation").doesNotExist())
        .andExpect(jsonPath("$.error").doesNotExist());
  }

  @When("login with user {string} and password {string}")
  public void loginWithNewUser(String username, String password) throws Exception {
    // test we can't login (challengeCode not confirmed)
    result = mvc.perform(get("/user/login").with(httpBasic(username, password)));
  }

  @When("confirm user's challenge code with APP role {string}")
  public void confirmChallengeCode(String appRole) throws Exception {
    GbifUser newUser = userMapper.get(user.getUserName());
    UUID challengeCodeString =
        challengeCodeMapper.getChallengeCode(userMapper.getChallengeCodeKey(newUser.getKey()));
    ConfirmationKeyParameter confirmation = new ConfirmationKeyParameter(challengeCodeString);
    String confirmationJsonString = objectMapper.writeValueAsString(confirmation);

    // perform request and check response
    // confirmation user and current one must be the same
    String contentMd5 = md5EncodeService.encode(confirmationJsonString);
    String gbifAuthorization =
        getGbifAuthorization(
            POST,
            "/admin/user/confirm",
            APPLICATION_JSON,
            contentMd5,
            "user_14",
            appRole,
            secrets.getProperty(appRole));
    result =
        mvc.perform(
            post("/admin/user/confirm")
                .content(confirmationJsonString)
                .contentType(APPLICATION_JSON)
                .header(HEADER_CONTENT_MD5, contentMd5)
                .header(HEADER_GBIF_USER, "user_14")
                .header(AUTHORIZATION, gbifAuthorization));
  }

  @Then("response of confirmation is valid")
  public void assertConfirmationResult() throws Exception {
    result
        .andExpect(jsonPath("$.userName").value("user_14"))
        .andExpect(jsonPath("$.email").value("user_14@gbif.org"));
  }

  @When("get user by {string}")
  public void getUserByUsername(String username) throws Exception {
    result =
        mvc.perform(
            get("/admin/user/{username}", username).with(httpBasic(TEST_ADMIN, TEST_PASSWORD)));
  }

  @Then("returned user {string} is valid")
  public void checkReturnedUser(String username) throws Exception {
    result
        .andExpect(jsonPath("$.userName").value(username))
        .andExpect(jsonPath("$.error").doesNotExist());
  }

  @When("get user by system settings {string} by admin")
  public void getUserBySystemSettingsByAdmin(String param) throws Exception {
    result =
        mvc.perform(
            get("/admin/user/find")
                .param("my.settings.key", param)
                .with(httpBasic(TEST_ADMIN, TEST_PASSWORD)));
  }

  @When("get user by system settings {string} by APP role {string}")
  public void getUserBySystemSettingsByApp(String param, String appRole) throws Exception {
    // with APP role
    String gbifAuthorization =
        getGbifAuthorization(
            GET, "/admin/user/find", appRole, appRole, secrets.getProperty(appRole));
    result =
        mvc.perform(
            get("/admin/user/find")
                .param("my.settings.key", param)
                .header(HEADER_GBIF_USER, appRole)
                .header(AUTHORIZATION, gbifAuthorization));
  }

  @When("reset password for user {string} by APP role {string}")
  public void resetPassword(String username, String appRole) throws Exception {
    String gbifAuthorization =
        getGbifAuthorization(
            POST, "/admin/user/resetPassword", username, appRole, secrets.getProperty(appRole));
    result =
        mvc.perform(
                post("/admin/user/resetPassword")
                    .header(HEADER_GBIF_USER, username)
                    .header(AUTHORIZATION, gbifAuthorization))
            .andExpect(status().isNoContent());
  }

  @Then("challenge code for user {string} is present now")
  public void checkChallengeCodeAfterPasswordReset(String username) {
    GbifUser newUser = userMapper.get(username);
    challengeCode =
        challengeCodeMapper.getChallengeCode(userMapper.getChallengeCodeKey(newUser.getKey()));
    assertThat(challengeCode, notNullValue());
  }

  @When(
      "update password to {string} for user {string} by APP role {string} with {word} challenge code")
  public void updateUserPassword(String newPassword, String username, String appRole, String type)
      throws Exception {
    AuthenticationDataParameters request = new AuthenticationDataParameters();
    request.setPassword(newPassword);
    request.setChallengeCode(type.equals("valid") ? challengeCode : UUID.randomUUID());
    String content = objectMapper.writeValueAsString(request);
    String contentMd5 = md5EncodeService.encode(content);
    String gbifAuthorization =
        getGbifAuthorization(
            POST,
            "/admin/user/updatePassword",
            APPLICATION_JSON,
            contentMd5,
            username,
            appRole,
            secrets.getProperty(appRole));

    result =
        mvc.perform(
            post("/admin/user/updatePassword")
                .contentType(APPLICATION_JSON)
                .content(content)
                .header(HEADER_CONTENT_MD5, contentMd5)
                .header(HEADER_GBIF_USER, username)
                .header(AUTHORIZATION, gbifAuthorization));
  }

  @Then("challenge code is valid for user {string} by APP role {string}")
  public void checkChallengeCodeValidity(String username, String appRole) throws Exception {
    String gbifAuthorization =
        getGbifAuthorization(
            GET,
            "/admin/user/confirmationKeyValid",
            username,
            appRole,
            secrets.getProperty(appRole));
    mvc.perform(
            get("/admin/user/confirmationKeyValid")
                .param("confirmationKey", challengeCode.toString())
                .header(HEADER_GBIF_USER, username)
                .header(AUTHORIZATION, gbifAuthorization))
        .andExpect(status().isNoContent());
  }

  @When("update user {string} with new {word} {string} by APP role {string}")
  public void updateUser(String username, String property, String newValue, String appRole)
      throws Exception {
    GbifUser user = userMapper.get(username);
    BeanUtils.setProperty(user, property, newValue);
    String userJsonString = objectMapper.writeValueAsString(user);

    // update user with a new name
    String contentMd5 = md5EncodeService.encode(userJsonString);
    String gbifAuthorization =
        getGbifAuthorization(
            PUT,
            "/admin/user/" + username,
            APPLICATION_JSON,
            contentMd5,
            appRole,
            appRole,
            secrets.getProperty(appRole));
    result =
        mvc.perform(
            put("/admin/user/{username}", username)
                .content(userJsonString)
                .contentType(APPLICATION_JSON)
                .header(HEADER_CONTENT_MD5, contentMd5)
                .header(HEADER_GBIF_USER, appRole)
                .header(AUTHORIZATION, gbifAuthorization));
  }

  @When("update user {string} with new {word} {string} by admin {string}")
  public void updateUserByAdmin(String username, String property, String newValue, String performer)
      throws Exception {
    GbifUser user = userMapper.get(username);
    BeanUtils.setProperty(user, property, newValue);
    String userJsonString = objectMapper.writeValueAsString(user);

    result =
        mvc.perform(
            put("/admin/user/{username}", username)
                .with(httpBasic(performer, credentials.get(performer)))
                .content(userJsonString)
                .contentType(APPLICATION_JSON));
  }

  @Then("{word} of user {string} was updated with new value {string}")
  public void checkFieldWasUpdated(String property, String username, String newValue)
      throws Exception {
    GbifUser gbifUser = userMapper.get(username);
    assertThat(BeanUtils.getProperty(gbifUser, property), is(newValue));
  }

  @Then("response should be {string}")
  public void checkUpdateResponse(String errorCode) throws Exception {
    result.andExpect(jsonPath("$.error").value(errorCode));
  }

  @Given("user which is {word} with credentials {string} and {string}")
  public void putUserToStorage(String role, String username, String password) {
    credentials.put(username, password);
  }

  @When("{string} adds a right {string} to the user {string}")
  public void addRightToUser(String performer, String right, String username) throws Exception {
    result =
        mvc.perform(
            post("/admin/user/{username}/editorRight", username)
                .content(right)
                .contentType(TEXT_PLAIN_VALUE)
                .with(httpBasic(performer, credentials.get(performer))));
  }

  @When("{string} gets user {string} rights")
  public void getUserRights(String performer, String username) throws Exception {
    result =
        mvc.perform(
            get("/admin/user/{username}/editorRight", username)
                .with(httpBasic(performer, credentials.get(performer))));
  }

  @Then("response is {string}")
  public void checkGetUserRightsResponse(String right) throws Exception {
    String content = right.isEmpty() ? right : "\"" + right + "\"";
    result.andExpect(content().string("[" + content + "]"));
  }

  @When("{string} deletes user {string} right {string}")
  public void deleteUserRights(String performer, String username, String right) throws Exception {
    result =
        mvc.perform(
            delete("/admin/user/{username}/editorRight/{key}", username, right)
                .with(httpBasic(performer, credentials.get(performer))));
  }

  @When("perform list roles")
  public void listRoles() throws Exception {
    result = mvc.perform(get("/admin/user/roles"));
  }

  @When("perform search user with query {string}")
  public void searchUser(String query) throws Exception {
    result =
        mvc.perform(
            get("/admin/user/search")
                .param("q", query)
                .param("limit", "20")
                .param("offset", "0")
                .with(httpBasic(TEST_ADMIN, TEST_PASSWORD)));
  }

  @Then("create user response contains error information {string}")
  public void checkChangePasswordErrorResponse(String errorInformation) throws Exception {
    result.andExpect(jsonPath("$.error").value(errorInformation));
  }

  @Then("roles response should be")
  public void assertRolesResponse(List<String> expectedRoles) throws Exception {
    result.andExpect(jsonPath("length()").value(expectedRoles.size()));
    for (int i = 0; i < expectedRoles.size(); i++) {
      result.andExpect(jsonPath(String.format("[%d]", i)).value(expectedRoles.get(i)));
    }
  }

  @Then("search users response are")
  public void assertSearchUsers(List<GbifUser> expectedUsers) throws Exception {
    result.andExpect(jsonPath("$.count").value(expectedUsers.size()));
    for (int i = 0; i < expectedUsers.size(); i++) {
      result
          .andExpect(
              jsonPath(String.format("$.results[%d].key", i)).value(expectedUsers.get(i).getKey()))
          .andExpect(
              jsonPath(String.format("$.results[%d].userName", i))
                  .value(expectedUsers.get(i).getUserName()))
          .andExpect(
              jsonPath(String.format("$.results[%d].firstName", i))
                  .value(expectedUsers.get(i).getFirstName()))
          .andExpect(
              jsonPath(String.format("$.results[%d].lastName", i))
                  .value(expectedUsers.get(i).getLastName()))
          .andExpect(
              jsonPath(String.format("$.results[%d].email", i))
                  .value(expectedUsers.get(i).getEmail()));
    }
  }

  private String getGbifAuthorization(
      RequestMethod method,
      String requestUrl,
      MediaType contentType,
      String contentMd5,
      String user,
      String authUser,
      String secretKey) {
    String strContentType = contentType == null ? null : contentType.toString();
    String stringToSign =
        buildStringToSign(method.name(), requestUrl, strContentType, contentMd5, user);
    String sign = signingService.buildSignature(stringToSign, secretKey);
    return "GBIF " + authUser + ":" + sign;
  }

  private String getGbifAuthorization(
      RequestMethod method, String requestUrl, String user, String authUser, String secretKey) {
    return getGbifAuthorization(method, requestUrl, null, null, user, authUser, secretKey);
  }

  private String buildStringToSign(
      String method, String requestUrl, String contentType, String contentMd5, String user) {
    StringBuilder sb = new StringBuilder();

    sb.append(method).append('\n');

    if (requestUrl != null) {
      sb.append(requestUrl).append('\n');
    }

    if (contentType != null) {
      sb.append(contentType).append('\n');
    }

    if (contentMd5 != null) {
      sb.append(contentMd5).append('\n');
    }

    if (user != null) {
      sb.append(user);
    }

    return sb.toString();
  }
}
