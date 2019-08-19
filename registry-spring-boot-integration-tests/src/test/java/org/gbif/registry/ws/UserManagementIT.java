package org.gbif.registry.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.registry.ConfirmationKeyParameter;
import org.gbif.registry.persistence.mapper.ChallengeCodeMapper;
import org.gbif.registry.persistence.mapper.UserMapper;
import org.gbif.registry.ws.model.UserCreation;
import org.gbif.utils.file.properties.PropertiesUtil;
import org.gbif.ws.security.Md5EncodeService;
import org.gbif.ws.security.SigningService;
import org.gbif.ws.util.SecurityConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Properties;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {TestEmailConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public class UserManagementIT {

  public static final String IT_APP_KEY = "gbif.app.it";

  private String secret;

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
  }

  /**
   * Creation flow.
   * First create a new user by an APP role user. After creation the user is not confirmed yet (inactive).
   * Try to login with this new user. It must fail with UNAUTHORIZED.
   * Confirm the user and after that try to login again. It must succeed.
   */
  @Test
  public void testCreateUser() throws Exception {
    final UserCreation user = prepareUser();

    final String userJsonString = objectMapper.writeValueAsString(user);

    // perform user creation and check response
    String contentMd5 = md5EncodeService.encode(userJsonString);
    String gbifAuthorization = getGbifAuthorization("POST", "/admin/user", "application/json", contentMd5, IT_APP_KEY, IT_APP_KEY);
    mvc
        .perform(
            post("/admin/user")
                .content(userJsonString)
                .contentType(MediaType.APPLICATION_JSON)
                .header(SecurityConstants.HEADER_CONTENT_MD5, contentMd5)
                .header(SecurityConstants.HEADER_GBIF_USER, IT_APP_KEY)
                .header(HttpHeaders.AUTHORIZATION, gbifAuthorization))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value("user_14"))
        .andExpect(jsonPath("$.email").value("user_14@gbif.org"))
        .andExpect(jsonPath("$.constraintViolation").doesNotExist())
        .andExpect(jsonPath("$.error").doesNotExist());

    // test we can't login (challengeCode not confirmed)
    mvc
        .perform(
            get("/user/login")
                .with(httpBasic("user_14", "welcome")))
        .andExpect(status().isUnauthorized())
        .andReturn();

    final GbifUser newUser = userMapper.get(user.getUserName());
    final UUID challengeCodeString = challengeCodeMapper.getChallengeCode(userMapper.getChallengeCodeKey(newUser.getKey()));
    final ConfirmationKeyParameter confirmation = new ConfirmationKeyParameter(challengeCodeString);
    final String confirmationJsonString = objectMapper.writeValueAsString(confirmation);

    // perform request and check response
    // confirmation user and current one must be the same
    contentMd5 = md5EncodeService.encode(confirmationJsonString);
    gbifAuthorization = getGbifAuthorization("POST", "/admin/user/confirm", "application/json", contentMd5, "user_14", IT_APP_KEY);
    mvc
        .perform(
            post("/admin/user/confirm")
                .content(confirmationJsonString)
                .contentType(MediaType.APPLICATION_JSON)
                .header(SecurityConstants.HEADER_CONTENT_MD5, contentMd5)
                .header(SecurityConstants.HEADER_GBIF_USER, "user_14")
                .header(HttpHeaders.AUTHORIZATION, gbifAuthorization))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userName").value("user_14"))
        .andExpect(jsonPath("$.email").value("user_14@gbif.org"));

    // test we can login now (challengeCode was confirmed)
    mvc
        .perform(
            get("/user/login")
                .with(httpBasic("user_14", "welcome")))
        .andExpect(status().isOk())
        .andReturn();
  }

  @Test
  public void getUserBySystemSettings() throws Exception {
    // with admin role
    mvc
        .perform(
            get("/admin/user/find")
                .param("my.settings.key", "100_tacos=100$")
                .with(httpBasic("justadmin", "welcome")))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.userName").value("justadmin"))
        .andExpect(jsonPath("$.error").doesNotExist());

    // with APP role
    String gbifAuthorization = getGbifAuthorization("GET", "/admin/user/find", null, null, IT_APP_KEY, IT_APP_KEY);
    mvc
        .perform(
            get("/admin/user/find")
                .param("my.settings.key", "100_tacos=100$")
                .header(SecurityConstants.HEADER_GBIF_USER, IT_APP_KEY)
                .header(HttpHeaders.AUTHORIZATION, gbifAuthorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.userName").value("justadmin"))
        .andExpect(jsonPath("$.error").doesNotExist());
  }

  @Test
  public void testResetPassword() throws Exception {
    // perform reset password
    String gbifAuthorization = getGbifAuthorization("POST", "/admin/user/resetPassword", null, null, "user_reset_password", IT_APP_KEY);
    mvc
        .perform(
            post("/admin/user/resetPassword")
                .header(SecurityConstants.HEADER_GBIF_USER, "user_reset_password")
                .header(HttpHeaders.AUTHORIZATION, gbifAuthorization))
        .andExpect(status().isNoContent());

    // check the challengeCode is present now
    final GbifUser newUser = userMapper.get("user_reset_password");
    final UUID challengeCodeString = challengeCodeMapper.getChallengeCode(userMapper.getChallengeCodeKey(newUser.getKey()));
    assertThat(challengeCodeString, notNullValue());

    // we should still be able to login with username/password
    mvc
        .perform(
            get("/user/login")
                .with(httpBasic("user_reset_password", "welcome")))
        .andExpect(status().isOk())
        .andReturn();
  }

  @Test
  public void testUpdatePassword() throws Exception {
    // perform update password with a random UUID
    String content = "{\"password\":\"newpass\",\"challengeCode\":\"" + UUID.randomUUID() + "\"}";
    String contentMd5 = md5EncodeService.encode(content);
    String gbifAuthorization = getGbifAuthorization("POST", "/admin/user/updatePassword", "application/json", contentMd5, "user_update_password", IT_APP_KEY);

    mvc
        .perform(
            post("/admin/user/updatePassword")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content)
                .header(SecurityConstants.HEADER_CONTENT_MD5, contentMd5)
                .header(SecurityConstants.HEADER_GBIF_USER, "user_update_password")
                .header(HttpHeaders.AUTHORIZATION, gbifAuthorization))
        .andExpect(status().isUnauthorized());

    // ask to reset password
    gbifAuthorization = getGbifAuthorization("POST", "/admin/user/resetPassword", null, null, "user_update_password", IT_APP_KEY);
    mvc
        .perform(
            post("/admin/user/resetPassword")
                .header(SecurityConstants.HEADER_GBIF_USER, "user_update_password")
                .header(HttpHeaders.AUTHORIZATION, gbifAuthorization))
        .andExpect(status().isNoContent());

    // check the challengeCode is present now
    final GbifUser newUser = userMapper.get("user_update_password");
    final UUID confirmationKey = challengeCodeMapper.getChallengeCode(userMapper.getChallengeCodeKey(newUser.getKey()));
    assertThat(confirmationKey, notNullValue());

    // ensure we can check if the challengeCode is valid for the user
    gbifAuthorization = getGbifAuthorization("GET", "/admin/user/confirmationKeyValid", null, null, "user_update_password", IT_APP_KEY);
    mvc
        .perform(
            get("/admin/user/confirmationKeyValid")
                .param("confirmationKey", confirmationKey.toString())
                .header(SecurityConstants.HEADER_GBIF_USER, "user_update_password")
                .header(HttpHeaders.AUTHORIZATION, gbifAuthorization))
        .andExpect(status().isNoContent());

    // change password using that code
    content = "{\"password\":\"newpass\",\"challengeCode\":\"" + confirmationKey + "\"}";
    contentMd5 = md5EncodeService.encode(content);
    gbifAuthorization = getGbifAuthorization("POST", "/admin/user/updatePassword", "application/json", contentMd5, "user_update_password", IT_APP_KEY);
    mvc
        .perform(
            post("/admin/user/updatePassword")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content)
                .header(SecurityConstants.HEADER_CONTENT_MD5, contentMd5)
                .header(SecurityConstants.HEADER_GBIF_USER, "user_update_password")
                .header(HttpHeaders.AUTHORIZATION, gbifAuthorization))
        .andExpect(status().isCreated());

    // ensure we can login with the new password
    mvc
        .perform(
            get("/user/login")
                .with(httpBasic("user_update_password", "newpass")))
        .andExpect(status().isOk())
        .andReturn();
  }

  @Test
  public void testUserEditorRights() throws Exception {
    final UUID uuid = UUID.randomUUID();

    // Admin add a right to the user
    mvc
        .perform(
            post("/admin/user/{username}/editorRight", "justuser")
                .content(uuid.toString())
                .contentType(MediaType.TEXT_PLAIN_VALUE)
                .with(httpBasic("justadmin", "welcome")))
        .andExpect(status().isCreated());

    // Admin can see user's rights
    mvc
        .perform(
            get("/admin/user/{username}/editorRight", "justuser")
                .with(httpBasic("justadmin", "welcome")))
        .andExpect(status().isOk())
        .andExpect(content().string("[\"" + uuid.toString() + "\"]"));

    // Use can see its own rights
    mvc
        .perform(
            get("/admin/user/{username}/editorRight", "justuser")
                .with(httpBasic("justuser", "welcome")))
        .andExpect(status().isOk())
        .andExpect(content().string("[\"" + uuid.toString() + "\"]"));

    // Admin delete the user's right
    mvc
        .perform(
            delete("/admin/user/{username}/editorRight/{key}", "justuser", uuid.toString())
                .with(httpBasic("justadmin", "welcome")))
        .andExpect(status().isNoContent());
  }

  private String getGbifAuthorization(String method, String requestUrl, String contentType, String contentMd5, String user, String authUser) {
    final String stringToSign = buildStringToSign(method, requestUrl, contentType, contentMd5, user);
    final String sign = signingService.buildSignature(stringToSign, secret);
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

  private UserCreation prepareUser() {
    UserCreation user = new UserCreation();
    user.setUserName("user_14");
    user.setFirstName("Tim");
    user.setLastName("Robertson");
    user.setPassword("welcome");
    user.getSettings().put("language", "en");
    user.getSettings().put("country", "dk");
    user.setEmail("user_14@gbif.org");
    return user;
  }
}
