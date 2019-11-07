package org.gbif.registry.ws.security;

import org.gbif.ws.WebApplicationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EditorAuthorizationFilterTest {

  private static final UUID KEY = UUID.randomUUID();
  private static final String USERNAME = "user";
  private static final List<GrantedAuthority> ROLES_EDITOR_ONLY =
    Collections.singletonList(new SimpleGrantedAuthority(UserRoles.EDITOR_ROLE));
  private static final List<GrantedAuthority> ROLES_USER_ONLY =
    Collections.singletonList(new SimpleGrantedAuthority(UserRoles.USER_ROLE));
  private static final List<GrantedAuthority> ROLES_ADMIN_AND_EDITOR =
    Arrays.asList(new SimpleGrantedAuthority(UserRoles.EDITOR_ROLE), new SimpleGrantedAuthority(UserRoles.ADMIN_ROLE));

  @Mock
  private HttpServletRequest mockRequest;
  @Mock
  private HttpServletResponse mockResponse;
  @Mock
  private FilterChain mockFilterChain;
  @Mock
  private AuthenticationFacade mockAuthenticationFacade;
  @Mock
  private EditorAuthorizationService mockEditorAuthService;
  @Mock
  private Authentication mockAuthentication;
  @InjectMocks
  private EditorAuthorizationFilter filter;

  @Test
  public void testOrganizationPostNotNullEditorUserSuccess() throws Exception {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/organization/" + KEY);
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    when(mockEditorAuthService.allowedToModifyOrganization(USERNAME, KEY)).thenReturn(true);

    // WHEN
    filter.doFilter(mockRequest, mockResponse, mockFilterChain);

    // THEN
    verify(mockAuthenticationFacade).getAuthentication();
    verify(mockRequest).getRequestURI();
    verify(mockRequest, times(2)).getMethod();
    verify(mockAuthentication).getName();
    verify(mockAuthentication, times(2)).getAuthorities();
    verify(mockEditorAuthService).allowedToModifyOrganization(USERNAME, KEY);
  }

  @Test
  public void testDatasetPutNotNullEditorUserSuccess() throws Exception {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/dataset/" + KEY);
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    when(mockEditorAuthService.allowedToModifyDataset(USERNAME, KEY))
      .thenReturn(true);

    // WHEN
    filter.doFilter(mockRequest, mockResponse, mockFilterChain);

    // THEN
    verify(mockAuthenticationFacade).getAuthentication();
    verify(mockRequest).getRequestURI();
    verify(mockRequest, times(2)).getMethod();
    verify(mockAuthentication).getName();
    verify(mockAuthentication, times(2)).getAuthorities();
    verify(mockEditorAuthService).allowedToModifyDataset(USERNAME, KEY);
  }

  @Test
  public void testInstallationPostNotNullEditorUserSuccess() throws Exception {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/installation/" + KEY);
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    when(mockEditorAuthService.allowedToModifyInstallation(USERNAME, KEY))
      .thenReturn(true);

    // WHEN
    filter.doFilter(mockRequest, mockResponse, mockFilterChain);

    // THEN
    verify(mockAuthenticationFacade).getAuthentication();
    verify(mockRequest).getRequestURI();
    verify(mockRequest, times(2)).getMethod();
    verify(mockAuthentication).getName();
    verify(mockAuthentication, times(2)).getAuthorities();
    verify(mockEditorAuthService).allowedToModifyInstallation(USERNAME, KEY);
  }

  @Test
  public void testNodePutNotNullEditorUserSuccess() throws Exception {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/node/" + KEY);
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    when(mockEditorAuthService.allowedToModifyEntity(USERNAME, KEY))
      .thenReturn(true);

    // WHEN
    filter.doFilter(mockRequest, mockResponse, mockFilterChain);

    // THEN
    verify(mockAuthenticationFacade).getAuthentication();
    verify(mockRequest).getRequestURI();
    verify(mockRequest, times(2)).getMethod();
    verify(mockAuthentication).getName();
    verify(mockAuthentication, times(2)).getAuthorities();
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
    verify(mockRequest).getMethod();
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
    verify(mockRequest, times(2)).getMethod();
  }

  @Test
  public void testOrganizationPutNullUserFail() throws Exception {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/organization/" + KEY);
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockAuthentication.getName()).thenReturn(null);

    try {
      // WHEN
      filter.doFilter(mockRequest, mockResponse, mockFilterChain);
      fail("WebApplicationException is expected");
    } catch (WebApplicationException e) {
      // THEN
      verify(mockAuthenticationFacade).getAuthentication();
      verify(mockRequest).getRequestURI();
      verify(mockRequest, times(2)).getMethod();
      verify(mockAuthentication).getName();
    }
  }

  @Test
  public void testOrganizationPutNotNullButNotEditorUserFail() throws Exception {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/organization/" + KEY);
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_USER_ONLY).when(mockAuthentication).getAuthorities();

    try {
      // WHEN
      filter.doFilter(mockRequest, mockResponse, mockFilterChain);
      fail("WebApplicationException is expected");
    } catch (WebApplicationException e) {
      // THEN
      verify(mockAuthenticationFacade).getAuthentication();
      verify(mockRequest).getRequestURI();
      verify(mockRequest, times(2)).getMethod();
      verify(mockAuthentication).getName();
      verify(mockAuthentication, times(2)).getAuthorities();
    }
  }

  @Test
  public void testOrganizationPutNotNullEditorUserButWithoutRightsOnThisEntityFail() throws Exception {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/organization/" + KEY);
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    when(mockEditorAuthService.allowedToModifyOrganization(USERNAME, KEY)).thenReturn(false);

    try {
      // WHEN
      filter.doFilter(mockRequest, mockResponse, mockFilterChain);
      fail("WebApplicationException is expected");
    } catch (WebApplicationException e) {
      // THEN
      verify(mockAuthenticationFacade).getAuthentication();
      verify(mockRequest).getRequestURI();
      verify(mockRequest, times(2)).getMethod();
      verify(mockAuthentication).getName();
      verify(mockAuthentication, times(2)).getAuthorities();
      verify(mockEditorAuthService).allowedToModifyOrganization(USERNAME, KEY);
    }
  }

  @Test
  public void testDatasetPutNotNullEditorUserButWithoutRightsOnThisEntityFail() throws Exception {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/dataset/" + KEY);
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    when(mockEditorAuthService.allowedToModifyDataset(USERNAME, KEY)).thenReturn(false);

    try {
      // WHEN
      filter.doFilter(mockRequest, mockResponse, mockFilterChain);
      fail("WebApplicationException is expected");
    } catch (WebApplicationException e) {
      // THEN
      verify(mockAuthenticationFacade).getAuthentication();
      verify(mockRequest).getRequestURI();
      verify(mockRequest, times(2)).getMethod();
      verify(mockAuthentication).getName();
      verify(mockAuthentication, times(2)).getAuthorities();
      verify(mockEditorAuthService).allowedToModifyDataset(USERNAME, KEY);
    }
  }

  @Test
  public void testInstallationPostNotNullEditorUserButWithoutRightsOnThisEntityFail() throws Exception {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/installation/" + KEY);
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    when(mockEditorAuthService.allowedToModifyInstallation(USERNAME, KEY)).thenReturn(false);

    try {
      // WHEN
      filter.doFilter(mockRequest, mockResponse, mockFilterChain);
      fail("WebApplicationException is expected");
    } catch (WebApplicationException e) {
      // THEN
      verify(mockAuthenticationFacade).getAuthentication();
      verify(mockRequest).getRequestURI();
      verify(mockRequest, times(2)).getMethod();
      verify(mockAuthentication).getName();
      verify(mockAuthentication, times(2)).getAuthorities();
      verify(mockEditorAuthService).allowedToModifyInstallation(USERNAME, KEY);
    }
  }

  @Test
  public void testNetworkPostNotNullEditorUserButWithoutRightsOnThisEntityFail() throws Exception {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/network/" + KEY);
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    when(mockEditorAuthService.allowedToModifyEntity(USERNAME, KEY)).thenReturn(false);

    try {
      // WHEN
      filter.doFilter(mockRequest, mockResponse, mockFilterChain);
      fail("WebApplicationException is expected");
    } catch (WebApplicationException e) {
      // THEN
      verify(mockAuthenticationFacade).getAuthentication();
      verify(mockRequest).getRequestURI();
      verify(mockRequest, times(2)).getMethod();
      verify(mockAuthentication).getName();
      verify(mockAuthentication, times(2)).getAuthorities();
      verify(mockEditorAuthService).allowedToModifyEntity(USERNAME, KEY);
    }
  }

  @Test
  public void testOrganizationDeleteNotNullAdminUserSuccess() throws Exception {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/organization/" + KEY + "/endpoint");
    when(mockRequest.getMethod()).thenReturn("DELETE");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_ADMIN_AND_EDITOR).when(mockAuthentication).getAuthorities();

    // WHEN
    filter.doFilter(mockRequest, mockResponse, mockFilterChain);

    // THEN
    verify(mockAuthenticationFacade).getAuthentication();
    verify(mockRequest).getRequestURI();
    verify(mockRequest, times(2)).getMethod();
    verify(mockAuthentication).getName();
    verify(mockAuthentication).getAuthorities();
  }
}
