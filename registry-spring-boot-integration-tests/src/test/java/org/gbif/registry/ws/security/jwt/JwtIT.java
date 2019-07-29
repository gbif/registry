package org.gbif.registry.ws.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gbif.api.service.common.LoggedUserWithToken;
import org.gbif.registry.ws.TestEmailConfiguration;
import org.gbif.registry.ws.TestJwtConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.gbif.registry.ws.security.SecurityConstants.BEARER_SCHEME_PREFIX;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {TestEmailConfiguration.class, TestJwtConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public class JwtIT {

  private MockMvc mvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  @Qualifier("jwtIssuanceService")
  private JwtIssuanceService jwtIssuanceService;

  @Before
  public void setUp() {
    mvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  // TODO: 2019-07-29 users should be in the DB before tests!

  // TODO: 2019-07-29 unstable
  @Test
  public void performTestWithValidTokenShouldReturnStatusCreatedAndUpdateToken() throws Exception {
    final String token = login("justadmin", "welcome");

    final MvcResult mvcResult = mvc
        .perform(
            post("/test")
                .header(HttpHeaders.AUTHORIZATION, BEARER_SCHEME_PREFIX + token))
        .andDo(print())
        .andExpect(status().isCreated())
        .andReturn();

    final String responseToken = mvcResult.getResponse().getHeader("token");
    assertNotNull(responseToken);
    assertNotEquals(token, responseToken);
  }

  @Test
  public void performTestWithWrongHeaderNameShouldReturnStatusForbidden() throws Exception {
    mvc
        .perform(
            post("/test")
                .header(HttpHeaders.AUTHORIZATION, "beare " + "token"))
        .andDo(print())
        .andExpect(status().isForbidden());
  }

  // TODO: 2019-07-29 there are problems with mocks
  // TODO: 2019-07-29 figure out why is not working
  @Test
  public void performTestWithWrongSigningKeyShouldReturnStatusForbidden() throws Exception {
    final JwtIssuanceService jwtIssuanceServiceWithWrongConfig = new JwtIssuanceService(10000, "GBIF", "fake");
    when(jwtIssuanceService.generateJwt("justadmin"))
        .then(p -> jwtIssuanceServiceWithWrongConfig.generateJwt("justadmin"));

    final String token = login("justadmin", "welcome");

    mvc
        .perform(
            post("/test")
                .header(HttpHeaders.AUTHORIZATION, BEARER_SCHEME_PREFIX + token))
        .andDo(print())
        .andExpect(status().isForbidden());
  }

  @Test
  public void performTestWithUserRoleShouldReturnStatusForbidden() throws Exception {
    final String token = login("justuser", "welcome");

    mvc
        .perform(
            post("/test")
                .header(HttpHeaders.AUTHORIZATION, BEARER_SCHEME_PREFIX + token))
        .andDo(print())
        .andExpect(status().isForbidden());
  }

  // TODO: 2019-07-29 implement a mapper for WebApplicationException
  @Test
  public void performTestWithTokenIssuedForFakeUserShouldReturnStatusForbidden() throws Exception {
    final String token = jwtIssuanceService.generateJwt("fake");

    mvc
        .perform(
            post("/test")
                .header(HttpHeaders.AUTHORIZATION, BEARER_SCHEME_PREFIX + token))
        .andDo(print())
        .andExpect(status().isForbidden());
  }

  @Test
  public void performTestWithNoBasicAndNoJwtShouldReturnStatusForbidden() throws Exception {
    mvc
        .perform(
            post("/test"))
        .andDo(print())
        .andExpect(status().isForbidden());
  }

  @Test
  public void performTestWithBasicAndNoJwtShouldReturnStatusCreated() throws Exception {
    mvc
        .perform(
            post("/test")
                .with(httpBasic("justadmin", "welcome")))
        .andDo(print())
        .andExpect(status().isCreated());
  }

  private String login(String user, String password) throws Exception {
    // login first (see UserManagementIT)
    ResultActions resultActions = mvc
        .perform(
            post("/user/login")
                .with(httpBasic(user, password))
                .characterEncoding("utf-8"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty());

    final MvcResult result = resultActions.andReturn();
    final String contentAsString = result.getResponse().getContentAsString();

    final LoggedUserWithToken loggedUserWithToken = objectMapper.readValue(contentAsString, LoggedUserWithToken.class);
    return loggedUserWithToken.getToken();
  }
}
