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
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@SpringBootTest(classes = {TestEmailConfiguration.class, RegistryIntegrationTestsConfiguration.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class UserManagementTestSteps {

  public static final String IT_APP_KEY = "gbif.app.it";
  public static final String IT_APP_KEY2 = "gbif.app.it2";

  private String secret;
  private String secret2;
  private ResultActions result;
  private UserCreation user;

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

    Properties props = PropertiesUtil.loadProperties(appkeysFile);
    secret = props.getProperty(IT_APP_KEY);
    secret2 = props.getProperty(IT_APP_KEY2);
  }

  @When("create a new user")
  public void createNewUser() throws Exception {
    user = prepareUserCreation();

    String userJsonString = objectMapper.writeValueAsString(user);

    // perform user creation and check response
    String contentMd5 = md5EncodeService.encode(userJsonString);
    String gbifAuthorization = getGbifAuthorization(POST, "/admin/user", APPLICATION_JSON, contentMd5, IT_APP_KEY, IT_APP_KEY, secret);
    result = mvc
      .perform(
        post("/admin/user")
          .content(userJsonString)
          .contentType(APPLICATION_JSON)
          .header(HEADER_CONTENT_MD5, contentMd5)
          .header(HEADER_GBIF_USER, IT_APP_KEY)
          .header(AUTHORIZATION, gbifAuthorization));
  }

  @Then("response status should be {int}")
  public void assertResponseCode(int status) throws Exception {
    result
      .andExpect(status().is(status));
  }

  @Then("created user reflects the original one")
  public void assertCreatedUser() throws Exception {
    // TODO: 25/10/2019 assert the whole result
    result
      .andExpect(jsonPath("$.username").value("user_14"))
      .andExpect(jsonPath("$.email").value("user_14@gbif.org"))
      .andExpect(jsonPath("$.constraintViolation").doesNotExist())
      .andExpect(jsonPath("$.error").doesNotExist());
  }

  @When("login with new user")
  public void loginWithNewUser() throws Exception {
    // test we can't login (challengeCode not confirmed)
    result = mvc
      .perform(
        get("/user/login")
          .with(httpBasic("user_14", "welcome")));
  }

  @When("confirm user's challenge code")
  public void confirmChallengeCode() throws Exception {
    final GbifUser newUser = userMapper.get(user.getUserName());
    final UUID challengeCodeString = challengeCodeMapper.getChallengeCode(userMapper.getChallengeCodeKey(newUser.getKey()));
    final ConfirmationKeyParameter confirmation = new ConfirmationKeyParameter(challengeCodeString);
    final String confirmationJsonString = objectMapper.writeValueAsString(confirmation);

    // perform request and check response
    // confirmation user and current one must be the same
    String contentMd5 = md5EncodeService.encode(confirmationJsonString);
    String gbifAuthorization = getGbifAuthorization(POST, "/admin/user/confirm", APPLICATION_JSON, contentMd5, "user_14", IT_APP_KEY, secret);
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

  private String getGbifAuthorization(RequestMethod method, String requestUrl, MediaType contentType, String contentMd5, String user, String authUser, String secretKey) {
    String strContentType = contentType == null ? null : contentType.toString();
    final String stringToSign = buildStringToSign(method.name(), requestUrl, strContentType, contentMd5, user);
    final String sign = signingService.buildSignature(stringToSign, secretKey);
    return "GBIF " + authUser + ":" + sign;
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
