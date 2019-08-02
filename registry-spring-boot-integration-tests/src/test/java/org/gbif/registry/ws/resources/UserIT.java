package org.gbif.registry.ws.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.util.Strings;
import org.gbif.api.service.common.LoggedUserWithToken;
import org.gbif.registry.ws.TestEmailConfiguration;
import org.gbif.registry.ws.model.AuthenticationDataParameters;
import org.gbif.ws.security.GbifAuthService;
import org.gbif.ws.server.RequestObject;
import org.gbif.ws.util.SecurityConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {TestEmailConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public class UserIT {

  private MockMvc mvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private GbifAuthService gbifAuthService;

  private static final String USERNAME = "user_12";
  private static final String EMAIL = "user_12@gbif.org";
  private static final String USERNAME_FOR_CHANGING_PASSWORD = "user_13";

  @Before
  public void setUp() {
    mvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  // TODO: 2019-07-30 revise test names

  @Test
  public void testLoginNoCredentials() throws Exception {
    // GET login
    mvc
        .perform(
            get("/user/login"))
        .andExpect(status().isUnauthorized());

    // POST login
    mvc
        .perform(
            post("/user/login"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  public void testLoginGet() throws Exception {
    final MvcResult mvcResult = mvc
        .perform(
            get("/user/login")
                .with(httpBasic(USERNAME, "welcome")))
        .andExpect(status().isOk())
        .andReturn();

    // check jwt token
    final String contentAsString = mvcResult.getResponse().getContentAsString();
    final LoggedUserWithToken loggedUserWithToken = objectMapper.readValue(contentAsString, LoggedUserWithToken.class);

    assertUserLogged(loggedUserWithToken, USERNAME);
    assertFalse(Strings.isNullOrEmpty(loggedUserWithToken.getToken()));

    // try to login using the email instead of the username
    mvc
        .perform(
            get("/user/login")
                .with(httpBasic(EMAIL, "welcome")))
        .andExpect(status().isOk())
        .andReturn();
  }

  @Test
  public void testLoginPost() throws Exception {
    final MvcResult mvcResult = mvc
        .perform(
            post("/user/login")
                .with(httpBasic(USERNAME, "welcome")))
         .andExpect(status().isCreated())
        .andReturn();

    // check jwt token
    final String contentAsString = mvcResult.getResponse().getContentAsString();
    final LoggedUserWithToken loggedUserWithToken = objectMapper.readValue(contentAsString, LoggedUserWithToken.class);

    assertUserLogged(loggedUserWithToken, USERNAME);
    assertFalse(Strings.isNullOrEmpty(loggedUserWithToken.getToken()));

    // try to login using the email instead of the username
    mvc
        .perform(
            post("/user/login")
                .with(httpBasic(EMAIL, "welcome")))
        .andExpect(status().isCreated());
  }

  @Test
  public void testChangePassword() throws Exception {
    final String newPassword = "123456";
    AuthenticationDataParameters params = new AuthenticationDataParameters();
    params.setPassword(newPassword);

    mvc
        .perform(
            put("/user/changePassword")
                .content(objectMapper.writeValueAsString(params))
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("utf-8")
                .with(httpBasic(USERNAME_FOR_CHANGING_PASSWORD, "welcome")))
        .andExpect(status().isNoContent());

    // try to login using the previous password
    mvc
        .perform(
            get("/user/login")
                .with(httpBasic(USERNAME_FOR_CHANGING_PASSWORD, "welcome")))
        .andExpect(status().isUnauthorized())
        .andReturn();

    // try with the new password
    mvc
        .perform(
            get("/user/login")
                .with(httpBasic(USERNAME_FOR_CHANGING_PASSWORD, "123456")))
        .andExpect(status().isOk())
        .andReturn();
  }

  // TODO: 2019-08-02 fix
  @Test
  public void testLoginWithAppKeysFail() throws Exception {
    final HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    httpHeaders.add(SecurityConstants.HEADER_GBIF_USER, "fake");

    final RequestObject requestObject =
        gbifAuthService.signRequest("fake", new RequestObject(RequestMethod.GET, "/test/app", null, httpHeaders));

    mvc
        .perform(
            post("/test/app")
                .headers(requestObject.getHeaders()))
        .andExpect(status().isForbidden());
  }

  // TODO: 2019-08-02 fix
  /**
   * The login endpoint only accepts HTTP Basic request.
   * Application that uses appkeys are trusted.
   */
  @Test
  public void testLoginWithAppKeysSuccess() throws Exception {
    final HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    httpHeaders.add(SecurityConstants.HEADER_GBIF_USER, "gbif.registry-ws-client-it");

    final TestResource.TestRequest testRequest = new TestResource.TestRequest("test");
    final String content = objectMapper.writeValueAsString(testRequest);

    final TestResource.TestRequest testRequest1 = new TestResource.TestRequest("test1");
    final String content1 = objectMapper.writeValueAsString(testRequest1);

    final RequestObject requestObject =
        gbifAuthService.signRequest("gbif.registry-ws-client-it", new RequestObject(RequestMethod.POST, "/test/app2", content1, httpHeaders));

    mvc
        .perform(
            post("/test/app2")
                .content(content)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(requestObject.getHeaders()))
        .andExpect(status().isCreated());
  }

  @Test
  public void testWhoAmI() throws Exception {
    final MvcResult mvcResult = mvc
        .perform(
            post("/user/whoami")
                .with(httpBasic(USERNAME, "welcome")))
        .andExpect(status().isCreated())
        .andReturn();

    final String contentAsString = mvcResult.getResponse().getContentAsString();
    final LoggedUserWithToken loggedUserWithToken = objectMapper.readValue(contentAsString, LoggedUserWithToken.class);

    assertUserLogged(loggedUserWithToken, USERNAME);
  }

  private void assertUserLogged(LoggedUserWithToken loggedUserWithToken, String username) {
    assertEquals(username, loggedUserWithToken.getUserName());
    assertEquals(username + "@gbif.org", loggedUserWithToken.getEmail());
    assertEquals("Tim", loggedUserWithToken.getFirstName());
    assertEquals(1, loggedUserWithToken.getRoles().size());
  }
}
