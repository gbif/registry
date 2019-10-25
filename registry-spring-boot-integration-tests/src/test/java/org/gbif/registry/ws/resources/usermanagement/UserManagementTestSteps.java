package org.gbif.registry.ws.resources.usermanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.registry.ConfirmationKeyParameter;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.persistence.mapper.UserMapper;
import org.gbif.registry.persistence.mapper.surety.ChallengeCodeMapper;
import org.gbif.registry.ws.TestEmailConfiguration;
import org.gbif.registry.ws.model.UserCreation;
import org.gbif.utils.file.properties.PropertiesUtil;
import org.gbif.ws.security.Md5EncodeService;
import org.gbif.ws.security.SigningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;

import java.util.Properties;
import java.util.UUID;

import static org.gbif.registry.utils.Users.prepareUserCreation;
import static org.gbif.ws.util.SecurityConstants.HEADER_CONTENT_MD5;
import static org.gbif.ws.util.SecurityConstants.HEADER_GBIF_USER;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@SpringBootTest(classes = {TestEmailConfiguration.class, RegistryIntegrationTestsConfiguration.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class UserManagementTestSteps {

  public static final String IT_APP_KEY = "gbif.app.it";
  public static final String IT_APP_KEY2 = "gbif.app.it2";

  private ResultActions result;
  private UserCreation user;
  private Properties secretProperties;

  @Value("${appkeys.file}")
  private String appkeysFile;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserMapper userMapper;

  @Autowired
  private ChallengeCodeMapper challengeCodeMapper;

  @Autowired
  private SigningService signingService;

  @Autowired
  private Md5EncodeService md5EncodeService;

  private MockMvc mvc;

  @Autowired
  private WebApplicationContext context;

  @Before
  public void setUp() throws Exception {
    mvc = MockMvcBuilders
      .webAppContextSetup(context)
      .apply(springSecurity())
      .build();

    secretProperties = PropertiesUtil.loadProperties(appkeysFile);
  }

  @When("create a new user {string} with APP role {string}")
  public void createNewUser(String username, String appRole) throws Exception {
    user = prepareUserCreation();

    String userJsonString = objectMapper.writeValueAsString(user);

    // perform user creation and check response
    String contentMd5 = md5EncodeService.encode(userJsonString);
    String gbifAuthorization = getGbifAuthorization(POST, "/admin/user", APPLICATION_JSON, contentMd5, appRole, appRole, secretProperties.getProperty(appRole));
    result = mvc
      .perform(
        post("/admin/user")
          .content(userJsonString)
          .contentType(APPLICATION_JSON)
          .header(HEADER_CONTENT_MD5, contentMd5)
          .header(HEADER_GBIF_USER, appRole)
          .header(AUTHORIZATION, gbifAuthorization));
  }

  @Then("response status should be {int}")
  public void assertResponseCode(int status) throws Exception {
    result
      .andExpect(status().is(status));
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

  @When("login with user {string}")
  public void loginWithNewUser(String username) throws Exception {
    // test we can't login (challengeCode not confirmed)
    result = mvc
      .perform(
        get("/user/login")
          .with(httpBasic(username, "welcome")));
  }

  @When("confirm user's challenge code with APP role {string}")
  public void confirmChallengeCode(String appRole) throws Exception {
    final GbifUser newUser = userMapper.get(user.getUserName());
    final UUID challengeCodeString = challengeCodeMapper.getChallengeCode(userMapper.getChallengeCodeKey(newUser.getKey()));
    final ConfirmationKeyParameter confirmation = new ConfirmationKeyParameter(challengeCodeString);
    final String confirmationJsonString = objectMapper.writeValueAsString(confirmation);

    // perform request and check response
    // confirmation user and current one must be the same
    String contentMd5 = md5EncodeService.encode(confirmationJsonString);
    String gbifAuthorization = getGbifAuthorization(POST, "/admin/user/confirm", APPLICATION_JSON, contentMd5, "user_14", appRole, secretProperties.getProperty(appRole));
    result = mvc
      .perform(
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
    result = mvc
      .perform(
        get("/admin/user/{username}", username)
          .with(httpBasic("justadmin", "welcome")));
  }

  @Then("returned user {string} is valid")
  public void checkReturnedUser(String username) throws Exception {
    result
      .andExpect(jsonPath("$.user.userName").value(username))
      .andExpect(jsonPath("$.error").doesNotExist());
  }

  @When("get user by system settings {string} by admin")
  public void getUserBySystemSettingsByAdmin(String param) throws Exception {
    result = mvc
      .perform(
        get("/admin/user/find")
          .param("my.settings.key", param)
          .with(httpBasic("justadmin", "welcome")));
  }

  @When("get user by system settings {string} by APP role {string}")
  public void getUserBySystemSettingsByApp(String param, String appRole) throws Exception {
    // with APP role
    String gbifAuthorization = getGbifAuthorization(GET, "/admin/user/find", appRole, appRole, secretProperties.getProperty(appRole));
    result = mvc
      .perform(
        get("/admin/user/find")
          .param("my.settings.key", param)
          .header(HEADER_GBIF_USER, appRole)
          .header(AUTHORIZATION, gbifAuthorization));
  }

  @When("reset password for user {string} by APP role {string}")
  public void resetPassword(String username, String appRole) throws Exception {
    String gbifAuthorization = getGbifAuthorization(POST, "/admin/user/resetPassword", username, appRole, secretProperties.getProperty(appRole));
    result = mvc
      .perform(
        post("/admin/user/resetPassword")
          .header(HEADER_GBIF_USER, username)
          .header(AUTHORIZATION, gbifAuthorization))
      .andExpect(status().isNoContent());
  }

  @Then("challenge code for user {string} is present now")
  public void checkChallengeCodeAfterPasswordReset(String username) {
    final GbifUser newUser = userMapper.get(username);
    final UUID challengeCodeString = challengeCodeMapper.getChallengeCode(userMapper.getChallengeCodeKey(newUser.getKey()));
    assertThat(challengeCodeString, notNullValue());
  }

  private String getGbifAuthorization(RequestMethod method, String requestUrl, MediaType contentType, String contentMd5, String user, String authUser, String secretKey) {
    String strContentType = contentType == null ? null : contentType.toString();
    final String stringToSign = buildStringToSign(method.name(), requestUrl, strContentType, contentMd5, user);
    final String sign = signingService.buildSignature(stringToSign, secretKey);
    return "GBIF " + authUser + ":" + sign;
  }

  private String getGbifAuthorization(RequestMethod method, String requestUrl, String user, String authUser, String secretKey) {
    return getGbifAuthorization(method, requestUrl, null, null, user, authUser, secretKey);
  }

  private String buildStringToSign(String method, String requestUrl, String contentType, String contentMd5, String user) {
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
