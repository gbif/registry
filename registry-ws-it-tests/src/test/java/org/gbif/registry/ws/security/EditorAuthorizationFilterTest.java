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
package org.gbif.registry.ws.security;

import org.gbif.registry.security.AuthenticationFacade;
import org.gbif.registry.security.EditorAuthorizationFilter;
import org.gbif.registry.security.EditorAuthorizationService;
import org.gbif.registry.security.UserRoles;

import java.security.Principal;
import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EditorAuthorizationFilterTest {

  private final String userWithRights = "with";

  @Mock ServletContext secContext;

  @Mock EditorAuthorizationService authService;

  @Mock HttpServletRequest mockRequest;

  @Mock HttpServletResponse mockResponse;

  @Mock FilterChain mockFilterChain;

  @Mock AuthenticationFacade authenticationFacade;

  private EditorAuthorizationFilter filter;

  @Autowired private ObjectMapper objectMapper;

  @Before
  public void setupMocks() throws Exception {
    // setup filter with mocks
    filter = new EditorAuthorizationFilter(authService, authenticationFacade, objectMapper);
    filter.setServletContext(secContext);
    when(mockRequest.isUserInRole(not(eq(UserRoles.EDITOR_ROLE)))).thenReturn(false);
    when(mockRequest.isUserInRole(eq(UserRoles.EDITOR_ROLE))).thenReturn(true);

    // setup mocks to authorize only based on user
    when(authService.allowedToModifyEntity(Matchers.any(), Matchers.any(UUID.class)))
        .thenAnswer(
            new Answer<Boolean>() {
              @Override
              public Boolean answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return ((Principal) args[0]).getName().equals(userWithRights);
              }
            });
    when(authService.allowedToModifyInstallation(Matchers.any(), Matchers.any(UUID.class)))
        .thenAnswer(
            new Answer<Boolean>() {
              @Override
              public Boolean answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return ((Principal) args[0]).getName().equals(userWithRights);
              }
            });
    when(authService.allowedToModifyOrganization(Matchers.any(), Matchers.any(UUID.class)))
        .thenAnswer(
            new Answer<Boolean>() {
              @Override
              public Boolean answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return ((Principal) args[0]).getName().equals(userWithRights);
              }
            });
    when(authService.allowedToModifyDataset(Matchers.any(), Matchers.any(UUID.class)))
        .thenAnswer(
            new Answer<Boolean>() {
              @Override
              public Boolean answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return ((Principal) args[0]).getName().equals(userWithRights);
              }
            });
  }

  @Test
  public void testFilterGET() throws Exception {
    setRequestUser(userWithRights);
    // GETs don't need auth
    mockRequest("GET", "dataset");
    filter.doFilter(mockRequest, mockResponse, mockFilterChain);
  }

  @Test
  public void testFilterPOSTgood() throws Exception {
    setRequestUser(userWithRights);
    mockRequest("POST", "dataset");
    filter.doFilter(mockRequest, mockResponse, mockFilterChain);
  }

  @Test
  public void testFilterPOSTgoodCreate() throws Exception {
    // this is allowed as the filter does not handle paths without a UUID such as the create ones
    setRequestUser("erfunden");
    mockRequest("POST", "dataset");
    filter.doFilter(mockRequest, mockResponse, mockFilterChain);
  }

  @Test(expected = WebApplicationException.class)
  public void testFilterPOSTbad() throws Exception {
    setRequestUser("erfunden");
    mockRequest("POST", "dataset/" + UUID.randomUUID().toString() + "/endpoint");
    filter.doFilter(mockRequest, mockResponse, mockFilterChain);
  }

  @Test
  public void testFilterKeyInPathGood() throws Exception {
    setRequestUser(userWithRights);
    mockRequest("DELETE", "dataset/" + UUID.randomUUID().toString());
    filter.doFilter(mockRequest, mockResponse, mockFilterChain);
  }

  @Test(expected = WebApplicationException.class)
  public void testFilterKeyInPathBad() throws Exception {
    setRequestUser("erfunden");
    mockRequest("DELETE", "dataset/" + UUID.randomUUID().toString());
    filter.doFilter(mockRequest, mockResponse, mockFilterChain);
  }

  private void mockRequest(String method, String path) {
    when(mockRequest.getRequestURI()).thenReturn(path);
    when(mockRequest.getMethod()).thenReturn(method.toUpperCase().trim());
  }

  private void setRequestUser(String user) {
    when(mockRequest.getUserPrincipal()).thenReturn(principal(user));
  }

  private Principal principal(final String user) {
    return new Principal() {
      @Override
      public String getName() {
        return user;
      }
    };
  }
}
