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
package org.gbif.registry.security;

import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.server.GbifHttpServletRequestWrapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EditorAuthorizationFilterTest {

  private static final UUID KEY = UUID.randomUUID();
  private static final int SUB_KEY = 123;
  private static final String USERNAME = "user";
  private static final String CONTENT = "{\"key\": \"" + KEY + "\"}";
  private static final Organization ORG = new Organization();
  private static final List<GrantedAuthority> ROLES_EDITOR_ONLY =
      Collections.singletonList(new SimpleGrantedAuthority(UserRoles.EDITOR_ROLE));
  private static final List<GrantedAuthority> ROLES_USER_ONLY =
      Collections.singletonList(new SimpleGrantedAuthority(UserRoles.USER_ROLE));
  private static final List<GrantedAuthority> ROLES_ADMIN_AND_EDITOR =
      Arrays.asList(
          new SimpleGrantedAuthority(UserRoles.EDITOR_ROLE),
          new SimpleGrantedAuthority(UserRoles.ADMIN_ROLE));

  @Mock private GbifHttpServletRequestWrapper mockRequest;
  @Mock private HttpServletResponse mockResponse;
  @Mock private FilterChain mockFilterChain;
  @Mock private AuthenticationFacade mockAuthenticationFacade;
  @Mock private EditorAuthorizationService mockEditorAuthService;
  @Mock private Authentication mockAuthentication;
  @Spy private final ObjectMapper objectMapper = new ObjectMapper();
  @InjectMocks private EditorAuthorizationFilter filter;

  @Test
  public void testOrganizationPostNotNullEditorUserSuccess() throws Exception {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/organization");
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockRequest.getContent()).thenReturn("{\"key\": \"" + KEY + "\"}");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    when(mockEditorAuthService.allowedToModifyOrganization(USERNAME, ORG)).thenReturn(true);
    when(objectMapper.readValue(CONTENT, Organization.class)).thenReturn(ORG);
    when(mockRequest.getContent()).thenReturn(CONTENT);

    // WHEN
    filter.doFilter(mockRequest, mockResponse, mockFilterChain);

    // THEN
    verify(mockAuthenticationFacade).getAuthentication();
    verify(mockRequest, atLeastOnce()).getRequestURI();
    verify(mockRequest, atLeast(2)).getMethod();
    verify(mockAuthentication, atLeastOnce()).getName();
    verify(mockAuthentication, atLeast(2)).getAuthorities();
    verify(mockEditorAuthService).allowedToModifyOrganization(USERNAME, ORG);
  }

  @Test
  public void testDatasetPutNotNullEditorUserSuccess() throws Exception {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/dataset/" + KEY);
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    when(mockEditorAuthService.allowedToModifyDataset(USERNAME, KEY)).thenReturn(true);

    // WHEN
    filter.doFilter(mockRequest, mockResponse, mockFilterChain);

    // THEN
    verify(mockAuthenticationFacade).getAuthentication();
    verify(mockRequest).getRequestURI();
    verify(mockRequest, atLeast(2)).getMethod();
    verify(mockAuthentication, atLeastOnce()).getName();
    verify(mockAuthentication, atLeast(2)).getAuthorities();
    verify(mockEditorAuthService).allowedToModifyDataset(USERNAME, KEY);
  }

  @Test
  public void testInstallationPostNotNullEditorUserSuccess() throws Exception {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/installation");
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockRequest.getContent()).thenReturn("{\"key\": \"" + KEY + "\"}");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    when(mockEditorAuthService.allowedToModifyInstallation(
            any(String.class), any(Installation.class)))
        .thenReturn(true);

    // WHEN
    filter.doFilter(mockRequest, mockResponse, mockFilterChain);

    // THEN
    verify(mockAuthenticationFacade).getAuthentication();
    verify(mockRequest, atLeastOnce()).getRequestURI();
    verify(mockRequest, atLeast(2)).getMethod();
    verify(mockAuthentication, atLeastOnce()).getName();
    verify(mockAuthentication, atLeast(2)).getAuthorities();
    verify(mockEditorAuthService)
        .allowedToModifyInstallation(any(String.class), any(Installation.class));
  }

  @Test
  public void testNodePutNotNullEditorUserSuccess() throws Exception {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/node/" + KEY);
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    when(mockEditorAuthService.allowedToModifyEntity(USERNAME, KEY)).thenReturn(true);

    // WHEN
    filter.doFilter(mockRequest, mockResponse, mockFilterChain);

    // THEN
    verify(mockAuthenticationFacade).getAuthentication();
    verify(mockRequest, atLeastOnce()).getRequestURI();
    verify(mockRequest, atLeast(2)).getMethod();
    verify(mockAuthentication, atLeastOnce()).getName();
    verify(mockAuthentication, atLeast(2)).getAuthorities();
    verify(mockEditorAuthService).allowedToModifyEntity(USERNAME, KEY);
  }

  @Test
  public void testOrganizationGetIgnore() throws Exception {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/organization/" + KEY);
    when(mockRequest.getMethod()).thenReturn("GET");

    // WHEN
    filter.doFilter(mockRequest, mockResponse, mockFilterChain);

    // THEN
    verify(mockAuthenticationFacade).getAuthentication();
    verify(mockRequest).getRequestURI();
    verify(mockRequest, atLeastOnce()).getMethod();
  }

  @Test
  public void testOrganizationOptionsIgnore() throws Exception {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/organization/" + KEY);
    when(mockRequest.getMethod()).thenReturn("OPTIONS");

    // WHEN
    filter.doFilter(mockRequest, mockResponse, mockFilterChain);

    // THEN
    verify(mockAuthenticationFacade).getAuthentication();
    verify(mockRequest).getRequestURI();
    verify(mockRequest, atLeast(2)).getMethod();
  }

  @Test
  public void testOrganizationPutNullUserFail() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/organization/" + KEY);
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockAuthentication.getName()).thenReturn(null);

    // WHEN & THEN
    assertThrows(
        WebApplicationException.class,
        () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
    verify(mockAuthenticationFacade).getAuthentication();
    verify(mockRequest).getRequestURI();
    verify(mockRequest, atLeast(2)).getMethod();
    verify(mockAuthentication).getName();
  }

  @Test
  public void testOrganizationPutNotNullButNotEditorUserFail() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/organization/" + KEY);
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_USER_ONLY).when(mockAuthentication).getAuthorities();

    // WHEN & THEN
    assertThrows(
        WebApplicationException.class,
        () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
    verify(mockAuthenticationFacade).getAuthentication();
    verify(mockRequest).getRequestURI();
    verify(mockRequest, atLeast(2)).getMethod();
    verify(mockAuthentication, atLeastOnce()).getName();
    verify(mockAuthentication, atLeast(2)).getAuthorities();
  }

  @Test
  public void testOrganizationPutNotNullEditorUserButWithoutRightsOnThisEntityFail() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/organization/" + KEY);
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    when(mockEditorAuthService.allowedToModifyOrganization(USERNAME, KEY)).thenReturn(false);

    // WHEN & THEN
    assertThrows(
        WebApplicationException.class,
        () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
    verify(mockAuthenticationFacade).getAuthentication();
    verify(mockRequest).getRequestURI();
    verify(mockRequest, atLeast(2)).getMethod();
    verify(mockAuthentication, atLeastOnce()).getName();
    verify(mockAuthentication, atLeast(2)).getAuthorities();
    verify(mockEditorAuthService).allowedToModifyOrganization(USERNAME, KEY);
  }

  @Test
  public void testDatasetPutNotNullEditorUserButWithoutRightsOnThisEntityFail() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/dataset/" + KEY);
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    when(mockEditorAuthService.allowedToModifyDataset(USERNAME, KEY)).thenReturn(false);

    // WHEN & THEN
    assertThrows(
        WebApplicationException.class,
        () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
    verify(mockAuthenticationFacade).getAuthentication();
    verify(mockRequest).getRequestURI();
    verify(mockRequest, atLeast(2)).getMethod();
    verify(mockAuthentication, atLeastOnce()).getName();
    verify(mockAuthentication, atLeast(2)).getAuthorities();
    verify(mockEditorAuthService).allowedToModifyDataset(USERNAME, KEY);
  }

  @Test
  public void testInstallationPostNotNullEditorUserButWithoutRightsOnThisEntityFail() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/installation");
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockRequest.getContent()).thenReturn("{\"key\": \"" + KEY + "\"}");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    when(mockEditorAuthService.allowedToModifyInstallation(
            any(String.class), any(Installation.class)))
        .thenReturn(false);

    // WHEN & THEN
    assertThrows(
        WebApplicationException.class,
        () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
    // THEN
    verify(mockAuthenticationFacade).getAuthentication();
    verify(mockRequest, atLeastOnce()).getRequestURI();
    verify(mockRequest, atLeast(2)).getMethod();
    verify(mockAuthentication, atLeastOnce()).getName();
    verify(mockAuthentication, atLeast(2)).getAuthorities();
    verify(mockEditorAuthService)
        .allowedToModifyInstallation(any(String.class), any(Installation.class));
  }

  @Test
  public void testNetworkPostNotNullEditorUserButWithoutRightsOnThisEntityFail() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/network");
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_EDITOR_ONLY).when(mockAuthentication).getAuthorities();

    // WHEN & THEN
    assertThrows(
        WebApplicationException.class,
        () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
    verify(mockAuthenticationFacade).getAuthentication();
    verify(mockRequest, atLeastOnce()).getRequestURI();
    verify(mockRequest, atLeast(2)).getMethod();
    verify(mockAuthentication, atLeastOnce()).getName();
    verify(mockAuthentication, atLeast(2)).getAuthorities();
  }

  @Test
  public void testOrganizationDeleteNotNullAdminUserSuccess() throws Exception {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/organization/" + KEY + "/endpoint/" + SUB_KEY);
    when(mockRequest.getMethod()).thenReturn("DELETE");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_ADMIN_AND_EDITOR).when(mockAuthentication).getAuthorities();

    // WHEN
    filter.doFilter(mockRequest, mockResponse, mockFilterChain);

    // THEN
    verify(mockAuthenticationFacade).getAuthentication();
    verify(mockRequest).getRequestURI();
    verify(mockRequest, atLeast(2)).getMethod();
    verify(mockAuthentication, atLeastOnce()).getName();
    verify(mockAuthentication).getAuthorities();
  }

  @Test
  public void testOrganizationEndorsementPostAnyUserSuccess() throws Exception {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/organization/" + KEY + "/endorsement");
    when(mockRequest.getMethod()).thenReturn("POST");

    // WHEN
    filter.doFilter(mockRequest, mockResponse, mockFilterChain);

    // THEN
    verify(mockAuthenticationFacade).getAuthentication();
    verify(mockRequest).getRequestURI();
    verify(mockRequest, atLeast(2)).getMethod();
  }
}
