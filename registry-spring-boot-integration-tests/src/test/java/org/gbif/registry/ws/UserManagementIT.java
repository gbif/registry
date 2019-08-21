package org.gbif.registry.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.registry.ConfirmationKeyParameter;
import org.gbif.registry.persistence.mapper.UserMapper;
import org.gbif.registry.persistence.mapper.surety.ChallengeCodeMapper;
import org.gbif.registry.ws.model.UserCreation;
import org.gbif.utils.file.properties.PropertiesUtil;
import org.gbif.ws.security.Md5EncodeService;
import org.gbif.ws.security.SigningService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;

import java.util.Properties;
import java.util.UUID;

import static org.gbif.ws.util.SecurityConstants.HEADER_CONTENT_MD5;
import static org.gbif.ws.util.SecurityConstants.HEADER_GBIF_USER;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@SpringBootTest(classes = {TestEmailConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public class UserManagementIT {

  public static final String IT_APP_KEY = "gbif.app.it";
  public static final String IT_APP_KEY2 = "gbif.app.it2";

  private String secret;

  private String secret2;

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

  /**
   * Creation flow.
   * First create a new user by an APP role user. After creation the user is not confirmed yet (inactive).
   * Try to login with this new user. It must fail with UNAUTHORIZED.
   * Confirm the user and after that try to login again. It must succeed.
   */
  @Test
  public void testCreateUser() throws Exception {
    final UserCreation user = prepareUserCreation();

    final String userJsonString = objectMapper.writeValueAsString(user);

    // perform user creation and check response
    String contentMd5 = md5EncodeService.encode(userJsonString);
    String gbifAuthorization = getGbifAuthorization(POST, "/admin/user", APPLICATION_JSON, contentMd5, IT_APP_KEY, IT_APP_KEY, secret);
    mvc
        .perform(
            post("/admin/user")
                .content(userJsonString)
                .contentType(APPLICATION_JSON)
                .header(HEADER_CONTENT_MD5, contentMd5)
                .header(HEADER_GBIF_USER, IT_APP_KEY)
                .header(AUTHORIZATION, gbifAuthorization))
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
    gbifAuthorization = getGbifAuthorization(POST, "/admin/user/confirm", APPLICATION_JSON, contentMd5, "user_14", IT_APP_KEY, secret);
    mvc
        .perform(
            post("/admin/user/confirm")
                .content(confirmationJsonString)
                .contentType(APPLICATION_JSON)
                .header(HEADER_CONTENT_MD5, contentMd5)
                .header(HEADER_GBIF_USER, "user_14")
                .header(AUTHORIZATION, gbifAuthorization))
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
  public void testCreateUserNonWhiteListAppKey() throws Exception {
    final UserCreation user = prepareUserCreation();

    final String userJsonString = objectMapper.writeValueAsString(user);

    String contentMd5 = md5EncodeService.encode(userJsonString);
    String gbifAuthorization = getGbifAuthorization(POST, "/admin/user", APPLICATION_JSON, contentMd5, IT_APP_KEY2, IT_APP_KEY2, secret2);
    mvc
        .perform(
            post("/admin/user")
                .content(userJsonString)
                .contentType(APPLICATION_JSON)
                .header(HEADER_CONTENT_MD5, contentMd5)
                .header(HEADER_GBIF_USER, IT_APP_KEY2)
                .header(AUTHORIZATION, gbifAuthorization))
        .andExpect(status().isForbidden());
  }

  @Test
  public void getUserFromAdmin() throws Exception {
    mvc
        .perform(
            get("/admin/user/{username}", "justuser")
                .with(httpBasic("justadmin", "welcome")))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.userName").value("justuser"))
        .andExpect(jsonPath("$.error").doesNotExist());
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
    String gbifAuthorization = getGbifAuthorization(GET, "/admin/user/find", IT_APP_KEY, IT_APP_KEY, secret);
    mvc
        .perform(
            get("/admin/user/find")
                .param("my.settings.key", "100_tacos=100$")
                .header(HEADER_GBIF_USER, IT_APP_KEY)
                .header(AUTHORIZATION, gbifAuthorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.userName").value("justadmin"))
        .andExpect(jsonPath("$.error").doesNotExist());
  }

  @Test
  public void testResetPassword() throws Exception {
    // perform reset password
    String gbifAuthorization = getGbifAuthorization(POST, "/admin/user/resetPassword", "user_reset_password", IT_APP_KEY, secret);
    mvc
        .perform(
            post("/admin/user/resetPassword")
                .header(HEADER_GBIF_USER, "user_reset_password")
                .header(AUTHORIZATION, gbifAuthorization))
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
    String gbifAuthorization = getGbifAuthorization(POST, "/admin/user/updatePassword", APPLICATION_JSON, contentMd5, "user_update_password", IT_APP_KEY, secret);

    mvc
        .perform(
            post("/admin/user/updatePassword")
                .contentType(APPLICATION_JSON)
                .content(content)
                .header(HEADER_CONTENT_MD5, contentMd5)
                .header(HEADER_GBIF_USER, "user_update_password")
                .header(AUTHORIZATION, gbifAuthorization))
        .andExpect(status().isUnauthorized());

    // ask to reset password
    gbifAuthorization = getGbifAuthorization(POST, "/admin/user/resetPassword", "user_update_password", IT_APP_KEY, secret);
    mvc
        .perform(
            post("/admin/user/resetPassword")
                .header(HEADER_GBIF_USER, "user_update_password")
                .header(AUTHORIZATION, gbifAuthorization))
        .andExpect(status().isNoContent());

    // check the challengeCode is present now
    final GbifUser newUser = userMapper.get("user_update_password");
    final UUID confirmationKey = challengeCodeMapper.getChallengeCode(userMapper.getChallengeCodeKey(newUser.getKey()));
    assertThat(confirmationKey, notNullValue());

    // ensure we can check if the challengeCode is valid for the user
    gbifAuthorization = getGbifAuthorization(GET, "/admin/user/confirmationKeyValid", "user_update_password", IT_APP_KEY, secret);
    mvc
        .perform(
            get("/admin/user/confirmationKeyValid")
                .param("confirmationKey", confirmationKey.toString())
                .header(HEADER_GBIF_USER, "user_update_password")
                .header(AUTHORIZATION, gbifAuthorization))
        .andExpect(status().isNoContent());

    // change password using that code
    content = "{\"password\":\"newpass\",\"challengeCode\":\"" + confirmationKey + "\"}";
    contentMd5 = md5EncodeService.encode(content);
    gbifAuthorization = getGbifAuthorization(POST, "/admin/user/updatePassword", APPLICATION_JSON, contentMd5, "user_update_password", IT_APP_KEY, secret);
    mvc
        .perform(
            post("/admin/user/updatePassword")
                .contentType(APPLICATION_JSON)
                .content(content)
                .header(HEADER_CONTENT_MD5, contentMd5)
                .header(HEADER_GBIF_USER, "user_update_password")
                .header(AUTHORIZATION, gbifAuthorization))
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
  public void testUpdateUser() throws Exception {
    final GbifUser user = userMapper.get("justadmin");
    user.setFirstName("Joseph");
    final String userJsonString = objectMapper.writeValueAsString(user);

    // update user with a new name
    String contentMd5 = md5EncodeService.encode(userJsonString);
    String gbifAuthorization = getGbifAuthorization(PUT, "/admin/user/justadmin", APPLICATION_JSON, contentMd5, IT_APP_KEY, IT_APP_KEY, secret);
    mvc
        .perform(
            put("/admin/user/{username}", "justadmin")
                .content(userJsonString)
                .contentType(APPLICATION_JSON)
                .header(HEADER_CONTENT_MD5, contentMd5)
                .header(HEADER_GBIF_USER, IT_APP_KEY)
                .header(AUTHORIZATION, gbifAuthorization))
        .andExpect(status().isNoContent());

    // load user directly from the database
    final GbifUser result = userMapper.get("justadmin");
    assertThat(result.getFirstName(), is("Joseph"));

    // update user using an existing email
    user.setEmail("justuser@gbif.org"); // should be an existing email
    final String userJsonString_2 = objectMapper.writeValueAsString(user);
    String contentMd5_2 = md5EncodeService.encode(userJsonString_2);
    String gbifAuthorization_2 = getGbifAuthorization(PUT, "/admin/user/justadmin", APPLICATION_JSON, contentMd5_2, IT_APP_KEY, IT_APP_KEY, secret);
    mvc
        .perform(
            put("/admin/user/{username}", "justadmin")
                .content(userJsonString_2)
                .contentType(APPLICATION_JSON)
                .header(HEADER_CONTENT_MD5, contentMd5_2)
                .header(HEADER_GBIF_USER, IT_APP_KEY)
                .header(AUTHORIZATION, gbifAuthorization_2))
        .andDo(print())
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_IN_USE"));

    // try with ADMIN role
    user.setFirstName("Joe");
    user.setEmail("justadmin@gbif.org");
    final String userJsonString_3 = objectMapper.writeValueAsString(user);
    mvc
        .perform(
            put("/admin/user/{username}", "justadmin")
                .with(httpBasic("justadmin", "welcome"))
                .content(userJsonString_3)
                .contentType(APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isNoContent());

    // load user directly from the database
    final GbifUser result2 = userMapper.get("justadmin");
    assertThat(result2.getFirstName(), is("Joe"));
  }

  @Test
  public void testUserEditorRights() throws Exception {
    final UUID uuid = UUID.randomUUID();

    // Admin add a right to the user
    mvc
        .perform(
            post("/admin/user/{username}/editorRight", "justuser")
                .content(uuid.toString())
                .contentType(TEXT_PLAIN_VALUE)
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

  @Test
  public void testUserEditorRightsErrors() throws Exception {
    final UUID uuid = UUID.randomUUID();

    // User doesn't exist
    mvc
        .perform(
            post("/admin/user/{username}/editorRight", "notexistinguser")
                .content(uuid.toString())
                .contentType(TEXT_PLAIN_VALUE)
                .with(httpBasic("justadmin", "welcome")))
        .andExpect(status().isNotFound());

    // Not an admin user
    mvc
        .perform(
            post("/admin/user/{username}/editorRight", "justuser")
                .content(uuid.toString())
                .contentType(TEXT_PLAIN_VALUE)
                .with(httpBasic("justuser", "welcome")))
        .andExpect(status().isForbidden());

    // Right already exists
    mvc
        .perform(
            post("/admin/user/{username}/editorRight", "justuser")
                .content(uuid.toString())
                .contentType(TEXT_PLAIN_VALUE)
                .with(httpBasic("justadmin", "welcome")))
        .andExpect(status().isCreated());

    mvc
        .perform(
            post("/admin/user/{username}/editorRight", "justuser")
                .content(uuid.toString())
                .contentType(TEXT_PLAIN_VALUE)
                .with(httpBasic("justadmin", "welcome")))
        .andExpect(status().isConflict());

    // Right doesn't exist
    mvc
        .perform(
            delete("/admin/user/{username}/editorRight/{key}", "justuser", UUID.randomUUID())
                .with(httpBasic("justadmin", "welcome")))
        .andExpect(status().isNotFound());
  }

  // TODO: 2019-08-19 move to the common class 
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

  private UserCreation prepareUserCreation() {
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
