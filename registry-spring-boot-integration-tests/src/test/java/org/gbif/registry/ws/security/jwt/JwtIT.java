package org.gbif.registry.ws.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gbif.api.service.common.LoggedUserWithToken;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.gbif.ws.util.SecurityConstants.BEARER_SCHEME_PREFIX;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public class JwtIT {

  private MockMvc mvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private JwtIssuanceService jwtIssuanceService;

  @Before
  public void setUp() {
    mvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  // Tty to login with valid credentials then extract a token from the response.
  // Then try with that token. It should be CREATED and the token should be updated.
  @Test
  public void testWithValidTokenShouldReturnStatusCreatedAndUpdateToken() throws Exception {
    final String token = login("registry_admin", "welcome");

    // otherwise the service may issue the same token because of the same time (seconds)
    Thread.sleep(1000);

    final MvcResult mvcResult = mvc
        .perform(
            post("/test")
                .header(HttpHeaders.AUTHORIZATION, BEARER_SCHEME_PREFIX + token))
        .andExpect(status().isCreated())
        .andReturn();

    final String responseToken = mvcResult.getResponse().getHeader("token");
    assertNotNull(responseToken);
    assertNotEquals(token, responseToken);
  }

  // Try with a wrong header name ('beare' instead of 'Bearer'). It should be FORBIDDEN.
  @Test
  public void testWithWrongHeaderNameShouldReturnStatusForbidden() throws Exception {
    mvc
        .perform(
            post("/test")
                .header(HttpHeaders.AUTHORIZATION, "beare " + "token"))
        .andExpect(status().isForbidden());
  }

  // Try with a wrong signing key. It should be FORBIDDEN.
  @Test
  public void testWithWrongSigningKeyShouldReturnStatusForbidden() throws Exception {
    final JwtConfiguration jwtConfiguration = new JwtConfiguration();
    jwtConfiguration.setExpiryTimeInMs(600000);
    jwtConfiguration.setIssuer("GBIF-REGISTRY");
    jwtConfiguration.setSigningKey("fake");

    final JwtIssuanceServiceImpl jwtIssuanceServiceWithWrongConfig = new JwtIssuanceServiceImpl(jwtConfiguration);
    final String token = jwtIssuanceServiceWithWrongConfig.generateJwt("registry_admin");

    mvc
        .perform(
            post("/test")
                .header(HttpHeaders.AUTHORIZATION, BEARER_SCHEME_PREFIX + token))
        .andExpect(status().isForbidden());
  }

  // Service expects the ADMIN role. If try with the USER role it should return FORBIDDEN.
  @Test
  public void testWithInsufficientUserRoleShouldReturnStatusForbidden() throws Exception {
    final String token = login("USER", "welcome");

    mvc
        .perform(
            post("/test")
                .header(HttpHeaders.AUTHORIZATION, BEARER_SCHEME_PREFIX + token))
        .andExpect(status().isForbidden());
  }

  // Service expects the valid token. If the token was issued for the unknown user it should return FORBIDDEN.
  @Test
  public void testWithTokenIssuedForFakeUserShouldReturnStatusForbidden() throws Exception {
    final String token = jwtIssuanceService.generateJwt("fake");

    mvc
        .perform(
            post("/test")
                .header(HttpHeaders.AUTHORIZATION, BEARER_SCHEME_PREFIX + token))
        .andExpect(status().isForbidden());
  }

  // Service expects either Basic auth or JWT. Otherwise it returns FORBIDDEN.
  @Test
  public void testWithNoBasicAndNoJwtShouldReturnStatusForbidden() throws Exception {
    mvc
        .perform(
            post("/test"))
        .andExpect(status().isForbidden());
  }

  // Try with valid Basic auth. It should be CREATED.
  @Test
  public void testWithBasicAndNoJwtShouldReturnStatusCreated() throws Exception {
    mvc
        .perform(
            post("/test")
                .with(httpBasic("registry_admin", "welcome")))
        .andExpect(status().isCreated());
  }

  private String login(String user, String password) throws Exception {
    // login first (see UserManagementIT)
    ResultActions resultActions = mvc
        .perform(
            post("/user/login")
                .with(httpBasic(user, password))
                .characterEncoding("utf-8"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.token").isNotEmpty());

    final MvcResult result = resultActions.andReturn();
    final String contentAsString = result.getResponse().getContentAsString();

    final LoggedUserWithToken loggedUserWithToken = objectMapper.readValue(contentAsString, LoggedUserWithToken.class);
    return loggedUserWithToken.getToken();
  }
}
