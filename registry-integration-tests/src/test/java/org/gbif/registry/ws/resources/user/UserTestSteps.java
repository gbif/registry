/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.ws.resources.user;

import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.domain.ws.AuthenticationDataParameters;
import org.gbif.registry.identity.model.LoggedUserWithToken;
import org.gbif.registry.ws.resources.TestResource;
import org.gbif.ws.security.GbifAuthServiceImpl;
import org.gbif.ws.server.DelegatingServletInputStream;
import org.gbif.ws.server.GbifHttpServletRequestWrapper;
import org.gbif.ws.util.SecurityConstants;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.util.Enumeration;
import java.util.Objects;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    classes = {RegistryIntegrationTestsConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class UserTestSteps {

  private ResultActions result;

  private LoggedUserWithToken loggedUserWithToken;

  private GbifHttpServletRequestWrapper requestWrapper;

  private HttpServletRequest rawRequestMock;

  private MockMvc mvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private WebApplicationContext context;

  @Autowired private GbifAuthServiceImpl gbifAuthService;

  @Autowired private DataSource ds;

  private Connection connection;

  @Before("@User")
  public void setUp() throws Exception {
    connection = ds.getConnection();
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/user/user_cleanup.sql"));
    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/user/user_prepare.sql"));

    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @After("@User")
  public void tearDown() throws Exception {
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/user/user_cleanup.sql"));

    connection.close();
  }

  @Given("user {string}")
  public void prepareUser(String username) {
    // data prepared by scripts
  }

  @When("login {string} with no credentials")
  public void loginGetWithNoCredentials(String method) throws Exception {
    MockHttpServletRequestBuilder requestBuilder =
        "GET".equals(method) ? get("/user/login") : post("/user/login");
    result = mvc.perform(requestBuilder);
  }

  @Then("response status should be {int}")
  public void checkResponseStatus(int status) throws Exception {
    result.andExpect(status().is(status));
  }

  @When("login {string} with valid credentials login {string} and password {string}")
  public void loginWithValidCredentials(String method, String login, String password)
      throws Exception {
    login(method, login, password);
  }

  private void login(String method, String login, String password) throws Exception {
    MockHttpServletRequestBuilder requestBuilder =
        "GET".equals(method) ? get("/user/login") : post("/user/login");
    result = mvc.perform(requestBuilder.with(httpBasic(login, password)));
  }

  @Then("user {string} is logged in")
  public void checkUserLoggedIn(String username, LoggedUserWithToken expectedUser)
      throws Exception {
    MvcResult mvcResult = result.andReturn();

    String contentAsString = mvcResult.getResponse().getContentAsString();
    loggedUserWithToken = objectMapper.readValue(contentAsString, LoggedUserWithToken.class);

    assertUserLogged(expectedUser, loggedUserWithToken);
  }

  @Then("JWT is present in the response")
  public void checkJwtInResponse() {
    assertThat(loggedUserWithToken.getToken(), not(isEmptyOrNullString()));
  }

  @When("change password for user {string} from {string} to {string}")
  public void changePassword(String login, String oldPassword, String newPassword)
      throws Exception {
    AuthenticationDataParameters params = new AuthenticationDataParameters();
    params.setPassword(newPassword);

    result =
        mvc.perform(
                put("/user/changePassword")
                    .content(objectMapper.writeValueAsString(params))
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(httpBasic(login, oldPassword)))
            .andDo(print());
  }

  @When("login {string} with old password {string}")
  public void loginWithOldPassword(String login, String oldPassword) throws Exception {
    login("GET", login, oldPassword);
  }

  @When("login {string} with new password {string}")
  public void loginWithNewPassword(String login, String newPassword) throws Exception {
    login("GET", login, newPassword);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Given("{word} request with {word} body and sign")
  public void prepareInvalidRequestAndSign(String state, String dataPresence) throws Exception {
    if ("invalid".equals(state)) {
      rawRequestMock = mock(HttpServletRequest.class);

      final Enumeration enumeration = new StringTokenizer("Content-Type x-gbif-user");
      when(rawRequestMock.getHeaderNames()).thenReturn(enumeration);
      when(rawRequestMock.getHeader(HttpHeaders.CONTENT_TYPE))
          .thenReturn(MediaType.APPLICATION_JSON_VALUE);
      when(rawRequestMock.getHeader(SecurityConstants.HEADER_GBIF_USER)).thenReturn("fake");

      requestWrapper = new GbifHttpServletRequestWrapper(rawRequestMock);
    } else {
      final TestResource.TestRequest requestToSign = new TestResource.TestRequest("test");
      final byte[] contentToSign = objectMapper.writeValueAsBytes(requestToSign);

      rawRequestMock = mock(HttpServletRequest.class);
      when(rawRequestMock.getMethod()).thenReturn(RequestMethod.POST.name());
      when(rawRequestMock.getRequestURI()).thenReturn("/test/app2");
      when(rawRequestMock.getInputStream())
          .thenReturn(new DelegatingServletInputStream(new ByteArrayInputStream(contentToSign)));
      final Enumeration enumeration = new StringTokenizer("Content-Type");
      when(rawRequestMock.getHeaderNames()).thenReturn(enumeration);
      when(rawRequestMock.getHeader(HttpHeaders.CONTENT_TYPE))
          .thenReturn(MediaType.APPLICATION_JSON_VALUE);

      requestWrapper =
          gbifAuthService.signRequest(
              "gbif.registry-ws-client-it", new GbifHttpServletRequestWrapper(rawRequestMock));
    }
  }

  @When("{word} login by APP role")
  public void loginByAppRole(String state) throws Exception {
    if ("invalid".equals(state)) {
      result = mvc.perform(post("/test/app").headers(requestWrapper.getHttpHeaders()));
    } else {
      result =
          mvc.perform(
              post("/test/app2")
                  .content(requestWrapper.getContent())
                  .contentType(MediaType.APPLICATION_JSON)
                  .headers(requestWrapper.getHttpHeaders()));
    }
  }

  @Then("{word} request verifications passed")
  public void invalidRequestVerifications(String state) {
    if ("invalid".equals(state)) {
      verify(rawRequestMock).getHeaderNames();
      verify(rawRequestMock).getHeader(HttpHeaders.CONTENT_TYPE);
      verify(rawRequestMock).getHeader(SecurityConstants.HEADER_GBIF_USER);
    } else {
      verify(rawRequestMock).getMethod();
      verify(rawRequestMock, atLeastOnce()).getRequestURI();
      verify(rawRequestMock).getHeaderNames();
      verify(rawRequestMock).getHeader(HttpHeaders.CONTENT_TYPE);
    }
  }

  @When("perform whoami request for user {string} with password {string}")
  public void getUserInformation(String user, String password) throws Exception {
    result = mvc.perform(post("/user/whoami").with(httpBasic(user, password)));
  }

  @Then("change password response contains error information {string}")
  public void checkChangePasswordErrorResponse(String errorInformation) throws Exception {
    result.andExpect(jsonPath("$.error").value(errorInformation));
  }

  private void assertUserLogged(LoggedUserWithToken expected, LoggedUserWithToken actual) {
    assertEquals(expected.getUserName(), actual.getUserName());
    assertEquals(expected.getEmail(), actual.getEmail());
    assertEquals(expected.getFirstName(), actual.getFirstName());
    assertEquals(expected.getLastName(), actual.getLastName());
  }
}
