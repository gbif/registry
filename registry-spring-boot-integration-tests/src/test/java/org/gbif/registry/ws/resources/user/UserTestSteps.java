package org.gbif.registry.ws.resources.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.gbif.api.service.common.LoggedUserWithToken;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.ws.TestEmailConfiguration;
import org.gbif.registry.ws.model.AuthenticationDataParameters;
import org.gbif.registry.ws.resources.TestResource;
import org.gbif.ws.security.GbifAuthServiceImpl;
import org.gbif.ws.server.DelegatingServletInputStream;
import org.gbif.ws.server.RequestObject;
import org.gbif.ws.util.SecurityConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
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

@SpringBootTest(classes = {TestEmailConfiguration.class, RegistryIntegrationTestsConfiguration.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class UserTestSteps {

  private ResultActions result;

  private LoggedUserWithToken loggedUserWithToken;

  private RequestObject requestObject;

  private HttpServletRequest rawRequestMock;

  private MockMvc mvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private GbifAuthServiceImpl gbifAuthService;

  @Before("@User")
  public void setUp() {
    mvc = MockMvcBuilders
      .webAppContextSetup(context)
      .apply(springSecurity())
      .build();
  }

  @When("login {string} with no credentials")
  public void loginGetWithNoCredentials(String method) throws Exception {
    MockHttpServletRequestBuilder requestBuilder = "GET".equals(method)
      ? get("/user/login") : post("/user/login");
    result = mvc.perform(requestBuilder);
  }

  @Then("response status should be {int}")
  public void checkResponseStatus(int status) throws Exception {
    result
      .andExpect(status().is(status));
  }

  @When("login {string} with valid credentials login {string} and password {string}")
  public void loginWithValidCredentials(String method, String login, String password) throws Exception {
    login(method, login, password);
  }

  private void login(String method, String login, String password) throws Exception {
    MockHttpServletRequestBuilder requestBuilder = "GET".equals(method)
      ? get("/user/login") : post("/user/login");
    result = mvc
      .perform(
        requestBuilder
          .with(httpBasic(login, password)));
  }

  @Then("user {string} is logged in")
  public void checkUserLoggedIn(String username) throws Exception {
    MvcResult mvcResult = result.andReturn();

    String contentAsString = mvcResult.getResponse().getContentAsString();
    loggedUserWithToken = objectMapper.readValue(contentAsString, LoggedUserWithToken.class);

    assertUserLogged(loggedUserWithToken, username);
  }

  @Then("JWT is present in the response")
  public void checkJwtInResponse() {
    assertThat(loggedUserWithToken.getToken(), not(isEmptyOrNullString()));
  }

  @When("change password for user {string} from {string} to {string}")
  public void changePassword(String login, String oldPassword, String newPassword) throws Exception {
    AuthenticationDataParameters params = new AuthenticationDataParameters();
    params.setPassword(newPassword);

    result = mvc
      .perform(
        put("/user/changePassword")
          .content(objectMapper.writeValueAsString(params))
          .contentType(MediaType.APPLICATION_JSON)
          .with(httpBasic(login, oldPassword)));
  }

  @When("login {string} with old password {string}")
  public void loginWithOldPassword(String login, String oldPassword) throws Exception {
    login("GET", login, oldPassword);
  }

  @When("login {string} with new password {string}")
  public void loginWithNewPassword(String login, String newPassword) throws Exception {
    login("GET", login, newPassword);
  }

  @SuppressWarnings("unchecked")
  @Given("invalid request with no body and sign")
  public void prepareInvalidRequestAndSign() {
    rawRequestMock = mock(HttpServletRequest.class);

    final Enumeration enumeration = new StringTokenizer("Content-Type x-gbif-user");
    when(rawRequestMock.getHeaderNames()).thenReturn(enumeration);
    when(rawRequestMock.getHeader(HttpHeaders.CONTENT_TYPE)).thenReturn(MediaType.APPLICATION_JSON_VALUE);
    when(rawRequestMock.getHeader(SecurityConstants.HEADER_GBIF_USER)).thenReturn("fake");

    requestObject = new RequestObject(rawRequestMock);
  }

  @SuppressWarnings("unchecked")
  @Given("valid request with body and sign")
  public void prepareValidRequestAndSign() throws Exception {
    final TestResource.TestRequest requestToSign = new TestResource.TestRequest("test");
    final byte[] contentToSign = objectMapper.writeValueAsBytes(requestToSign);

    rawRequestMock = mock(HttpServletRequest.class);
    when(rawRequestMock.getMethod()).thenReturn(RequestMethod.POST.name());
    when(rawRequestMock.getRequestURI()).thenReturn("/test/app2");
    when(rawRequestMock.getInputStream()).thenReturn(new DelegatingServletInputStream(new ByteArrayInputStream(contentToSign)));
    final Enumeration enumeration = new StringTokenizer("Content-Type");
    when(rawRequestMock.getHeaderNames()).thenReturn(enumeration);
    when(rawRequestMock.getHeader(HttpHeaders.CONTENT_TYPE)).thenReturn(MediaType.APPLICATION_JSON_VALUE);

    // TODO: 11/10/2019 replace manual sign with a filter
    requestObject = gbifAuthService.signRequest("gbif.registry-ws-client-it", new RequestObject(rawRequestMock));
  }

  @When("{string} login by APP role")
  public void loginByAppRole(String state) throws Exception {
    if ("invalid".equals(state)) {
      result = mvc
        .perform(
          post("/test/app")
            .headers(requestObject.getHttpHeaders()));
    } else {
      result = mvc
        .perform(
          post("/test/app2")
            .content(requestObject.getContent())
            .contentType(MediaType.APPLICATION_JSON)
            .headers(requestObject.getHttpHeaders()));
    }
  }

  @Then("invalid request verifications passed")
  public void invalidRequestVerifications() {
    verify(rawRequestMock).getHeaderNames();
    verify(rawRequestMock).getHeader(HttpHeaders.CONTENT_TYPE);
    verify(rawRequestMock).getHeader(SecurityConstants.HEADER_GBIF_USER);
  }

  @Then("valid request verifications passed")
  public void validRequestVerifications() {
    verify(rawRequestMock).getMethod();
    verify(rawRequestMock, atLeastOnce()).getRequestURI();
    verify(rawRequestMock).getHeaderNames();
    verify(rawRequestMock).getHeader(HttpHeaders.CONTENT_TYPE);
  }

  @When("get user {string} and password {string} information")
  public void getUserInformation(String user, String password) throws Exception {
    result = mvc
      .perform(
        post("/user/whoami")
          .with(httpBasic(user, password)));
  }

  @Then("response should match the user {string}")
  public void checkWhoamiResponse(String user) throws Exception {
    MvcResult mvcResult = result.andReturn();
    final String contentAsString = mvcResult.getResponse().getContentAsString();
    final LoggedUserWithToken loggedUserWithToken = objectMapper.readValue(contentAsString, LoggedUserWithToken.class);

    assertUserLogged(loggedUserWithToken, user);
  }

  // TODO: 20/10/2019 predefine users
  private void assertUserLogged(LoggedUserWithToken loggedUserWithToken, String login) {
    String username = login.contains("@") ? login.split("@")[0] : login;
    assertEquals(username, loggedUserWithToken.getUserName());
    assertEquals(username + "@gbif.org", loggedUserWithToken.getEmail());
    assertEquals("Tim", loggedUserWithToken.getFirstName());
    assertEquals(1, loggedUserWithToken.getRoles().size());
  }
}
