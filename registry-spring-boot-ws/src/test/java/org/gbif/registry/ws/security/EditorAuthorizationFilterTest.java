package org.gbif.registry.ws.security;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EditorAuthorizationFilterTest {

  private static final UUID KEY = UUID.randomUUID();
  private static final String USERNAME = "user";

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
  public void testOrganizationPostNotNullEditorUser() throws Exception {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/organization/" + KEY);
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(Collections.singletonList(new SimpleGrantedAuthority(UserRoles.EDITOR_ROLE)))
      .when(mockAuthentication).getAuthorities();
    when(mockEditorAuthService.allowedToModifyOrganization(USERNAME, KEY))
      .thenReturn(true);

    // WHEN
    filter.doFilter(mockRequest, mockResponse, mockFilterChain);

    // THEN
    // exception was not thrown and all verification passed
    verify(mockAuthenticationFacade).getAuthentication();
    verify(mockRequest).getRequestURI();
    verify(mockRequest, times(2)).getMethod();
    verify(mockAuthentication).getName();
    verify(mockAuthentication, times(2)).getAuthorities();
    verify(mockEditorAuthService).allowedToModifyOrganization(USERNAME, KEY);
  }
}
