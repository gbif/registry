package org.gbif.registry.ws.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gbif.api.service.common.LoggedUserWithToken;
import org.gbif.registry.ws.TestEmailConfiguration;
import org.gbif.registry.ws.model.AuthenticationDataParameters;
import org.gbif.ws.security.GbifAuthServiceImpl;
import org.gbif.ws.server.DelegatingServletInputStream;
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

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.util.Enumeration;
import java.util.StringTokenizer;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
  private GbifAuthServiceImpl gbifAuthService;

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

  /**
   * Try to login with no credentials provided (POST\GET). 401 is expected.
   */
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

  /**
   * GET method
   * Try to login with valid credentials (Basic auth).
   * Check that the response is 200 and the JWT is present it the response.
   * Try to login by email. Should be 200 as well.
   */
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
    assertThat(loggedUserWithToken.getToken(), not(isEmptyOrNullString()));

    // try to login using the email instead of the username
    mvc
        .perform(
            get("/user/login")
                .with(httpBasic(EMAIL, "welcome")))
        .andExpect(status().isOk())
        .andReturn();
  }

  /**
   * POST method
   * Try to login with valid credentials (Basic auth).
   * Check that the response is 200 and the JWT is present it the response.
   * Try to login by email. Should be 200 as well.
   */
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
    assertThat(loggedUserWithToken.getToken(), not(isEmptyOrNullString()));

    // try to login using the email instead of the username
    mvc
        .perform(
            post("/user/login")
                .with(httpBasic(EMAIL, "welcome")))
        .andExpect(status().isCreated());
  }

  /**
   * Try to change the password.
   * Then check that the previous password is invalid (401) and the current one is OK (200).
   */
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

  /**
   * Try to login with APP role. Use invalid credentials (appKey).
   * No request body.
   * Mock HttpServletRequest and sign it. (The sign service should return 'Authorization' header).
   * Check the response is 403.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testLoginWithAppKeysFail() throws Exception {
    HttpServletRequest rawRequestMock = mock(HttpServletRequest.class);

    final Enumeration enumeration = new StringTokenizer("Content-Type x-gbif-user");
    when(rawRequestMock.getHeaderNames()).thenReturn(enumeration);
    when(rawRequestMock.getHeader(HttpHeaders.CONTENT_TYPE)).thenReturn(MediaType.APPLICATION_JSON_VALUE);
    when(rawRequestMock.getHeader(SecurityConstants.HEADER_GBIF_USER)).thenReturn("fake");

    RequestObject request = new RequestObject(rawRequestMock);

    mvc
        .perform(
            post("/test/app")
                .headers(request.getHttpHeaders()))
        .andExpect(status().isForbidden());

    verify(rawRequestMock).getHeaderNames();
    verify(rawRequestMock).getHeader(HttpHeaders.CONTENT_TYPE);
    verify(rawRequestMock).getHeader(SecurityConstants.HEADER_GBIF_USER);
  }

  /**
   * Try to login with APP role. Use valid credentials (appKey).
   * There should be a secretKey for this appKey in the storage.
   * Request body is present.
   * Mock HttpServletRequest and sign it. (The sign service should return 'Authorization' header).
   * Check the response is 201.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testLoginWithAppKeysSuccess() throws Exception {
    final TestResource.TestRequest requestToSign = new TestResource.TestRequest("test");
    final byte[] contentToSign = objectMapper.writeValueAsBytes(requestToSign);

    HttpServletRequest rawRequestMock = mock(HttpServletRequest.class);
    when(rawRequestMock.getMethod()).thenReturn(RequestMethod.POST.name());
    when(rawRequestMock.getRequestURI()).thenReturn("/test/app2");
    when(rawRequestMock.getInputStream()).thenReturn(new DelegatingServletInputStream(new ByteArrayInputStream(contentToSign)));
    final Enumeration enumeration = new StringTokenizer("Content-Type");
    when(rawRequestMock.getHeaderNames()).thenReturn(enumeration);
    when(rawRequestMock.getHeader(HttpHeaders.CONTENT_TYPE)).thenReturn(MediaType.APPLICATION_JSON_VALUE);

    RequestObject requestMock = new RequestObject(rawRequestMock);

    // TODO: 11/10/2019 replace manual sign with a filter
    final RequestObject signedRequest = gbifAuthService.signRequest("gbif.registry-ws-client-it", requestMock);

    mvc
        .perform(
            post("/test/app2")
                .content(signedRequest.getContent())
                .contentType(MediaType.APPLICATION_JSON)
                .headers(signedRequest.getHttpHeaders()))
        .andExpect(status().isCreated());

    verify(rawRequestMock).getMethod();
    verify(rawRequestMock, atLeastOnce()).getRequestURI();
    verify(rawRequestMock).getHeaderNames();
    verify(rawRequestMock).getHeader(HttpHeaders.CONTENT_TYPE);
  }

  /**
   * Call whoami and check that the user is valid.
   */
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
