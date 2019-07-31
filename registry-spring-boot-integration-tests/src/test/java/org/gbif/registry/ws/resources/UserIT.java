package org.gbif.registry.ws.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.util.Strings;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.service.common.LoggedUserWithToken;
import org.gbif.registry.persistence.ChallengeCodeSupportMapper;
import org.gbif.registry.persistence.mapper.ChallengeCodeMapper;
import org.gbif.registry.ws.TestEmailConfiguration;
import org.gbif.registry.ws.TestJwtConfiguration;
import org.gbif.registry.ws.fixtures.UserTestFixture;
import org.gbif.registry.ws.model.AuthenticationDataParameters;
import org.gbif.registry.ws.security.CustomRequestObject;
import org.gbif.registry.ws.security.GbifAuthService;
import org.gbif.registry.ws.security.jwt.JwtIssuanceService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {TestEmailConfiguration.class, TestJwtConfiguration.class},
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
  @Qualifier("jwtIssuanceService")
  private JwtIssuanceService jwtIssuanceService;

  @Autowired
  private IdentityService identityService;

  @Autowired
  private ChallengeCodeMapper challengeCodeMapper;

  @Autowired
  private ChallengeCodeSupportMapper<Integer> challengeCodeSupportMapper;

  @Autowired
  private GbifAuthService gbifAuthService;

  private UserTestFixture userTestFixture;

  @Before
  public void setUp() {
    mvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();

    userTestFixture = new UserTestFixture(identityService, challengeCodeMapper, challengeCodeSupportMapper);
  }

  // TODO: 2019-07-29 users should be in the DB before tests! (use prepareUser ?)
  // TODO: 2019-07-30 revise test names

  @Test
  public void testLoginNoCredentials() throws Exception {
    // GET login
    mvc
        .perform(
            get("/user/login"))
        .andDo(print())
        .andExpect(status().isUnauthorized());

    // POST login
    mvc
        .perform(
            post("/user/login"))
        .andDo(print())
        .andExpect(status().isUnauthorized());
  }

  @Test
  public void testLoginGet() throws Exception {
    final GbifUser user = userTestFixture.prepareUser();
    final MvcResult mvcResult = mvc
        .perform(
            get("/user/login")
                .with(httpBasic(user.getUserName(), "welcome")))
        .andDo(print())
        .andExpect(status().isOk())
        .andReturn();

    // check jwt token
    final String contentAsString = mvcResult.getResponse().getContentAsString();
    final LoggedUserWithToken loggedUserWithToken = objectMapper.readValue(contentAsString, LoggedUserWithToken.class);

    assertUserLogged(loggedUserWithToken, user);
    assertFalse(Strings.isNullOrEmpty(loggedUserWithToken.getToken()));

    // TODO: 2019-07-30 it's not working (401). check why
    // try to login using the email instead of the username
    mvc
        .perform(
            get("/user/login")
                .with(httpBasic(user.getEmail(), "welcome")))
        .andDo(print())
        .andExpect(status().isOk())
        .andReturn();
  }

  // TODO: 2019-07-30 gets 200 instead of expected 201
  @Test
  public void testLoginPost() throws Exception {
    final GbifUser user = userTestFixture.prepareUser();
    final MvcResult mvcResult = mvc
        .perform(
            post("/user/login")
                .with(httpBasic(user.getUserName(), "welcome")))
        .andDo(print())
        .andExpect(status().isCreated())
        .andReturn();

    // check jwt token
    final String contentAsString = mvcResult.getResponse().getContentAsString();
    final LoggedUserWithToken loggedUserWithToken = objectMapper.readValue(contentAsString, LoggedUserWithToken.class);

    assertUserLogged(loggedUserWithToken, user);
    assertFalse(Strings.isNullOrEmpty(loggedUserWithToken.getToken()));

    // try to login using the email instead of the username
    mvc
        .perform(
            post("/user/login")
                .with(httpBasic(user.getEmail(), "welcome")))
        .andDo(print())
        .andExpect(status().isCreated())
        .andReturn();
  }

  @Test
  public void testChangePassword() throws Exception {
    final GbifUser user = userTestFixture.prepareUser();

    final String newPassword = "123456";
    AuthenticationDataParameters params = new AuthenticationDataParameters();
    params.setPassword(newPassword);

    final MvcResult mvcResult = mvc
        .perform(
            put("/user/changePassword")
                .content(objectMapper.writeValueAsString(params))
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("utf-8")
                .with(httpBasic(user.getUserName(), "welcome")))
        .andDo(print())
        .andExpect(status().isNoContent())
        .andReturn();

    // try to login using the previous password
    mvc
        .perform(
            get("/user/login")
                .with(httpBasic(user.getUserName(), "welcome")))
        .andDo(print())
        .andExpect(status().isUnauthorized())
        .andReturn();

    // try with the new password
    mvc
        .perform(
            get("/user/login")
                .with(httpBasic(user.getUserName(), "123456")))
        .andDo(print())
        .andExpect(status().isOk())
        .andReturn();
  }

  // TODO: 2019-07-31 we need a test opposite this
  /**
   * The login endpoint only accepts HTTP Basic request.
   * Application that uses appkeys are trusted.
   */
  @Test
  public void testLoginWithAppKeys() throws Exception {
    GbifUser user = userTestFixture.prepareUser();

    final HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

    final CustomRequestObject customRequestObject = gbifAuthService.signRequest(user.getUserName(), new CustomRequestObject(RequestMethod.GET, "/user/login", null, httpHeaders));

    final MvcResult mvcResult = mvc
        .perform(
            get("/user/login")
                .headers(customRequestObject.getHeaders()))
        .andDo(print())
        .andExpect(status().isForbidden())
        .andReturn();
  }

  @Test
  public void testWhoAmI() throws Exception {
    // create test user
    GbifUser user = userTestFixture.prepareUser();

    final MvcResult mvcResult = mvc
        .perform(
            post("/user/whoami")
                .with(httpBasic(user.getUserName(), "welcome")))
        .andDo(print())
        .andExpect(status().isOk()) // TODO: 2019-07-31 should be 'created' instead
        .andReturn();

    final String contentAsString = mvcResult.getResponse().getContentAsString();
    final LoggedUserWithToken loggedUserWithToken = objectMapper.readValue(contentAsString, LoggedUserWithToken.class);

    assertUserLogged(loggedUserWithToken, user);
  }

  private void assertUserLogged(LoggedUserWithToken loggedUserWithToken, GbifUser user) {
    assertEquals(user.getUserName(), loggedUserWithToken.getUserName());
    assertEquals(user.getEmail(), loggedUserWithToken.getEmail());
    assertEquals(user.getFirstName(), loggedUserWithToken.getFirstName());
    assertEquals(user.getRoles().size(), loggedUserWithToken.getRoles().size());
  }
}
