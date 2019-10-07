package org.gbif.ws.server.filter;

import org.gbif.ws.security.AppkeysConfiguration;
import org.gbif.ws.security.GbifAuthService;
import org.gbif.ws.server.DelegatingServletInputStream;
import org.gbif.ws.server.RequestObject;
import org.gbif.ws.util.SecurityConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests related to {@link AppIdentityFilter}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AppIdentityFilterTest {

  //initialize in setUp()
  private AppIdentityFilter appIdentityFilter;

  @Mock
  private GbifAuthService authServiceMock;

  private SecurityContext context;

  private String content;

  @Before
  public void setUp() {
    content = "content";
    context = new SecurityContextImpl();
    SecurityContextHolder.setContext(context);
    AppkeysConfiguration appkeysConfiguration = new AppkeysConfiguration();
    appkeysConfiguration.setWhitelist(Collections.singletonList("appkey"));
    appIdentityFilter = new AppIdentityFilter(authServiceMock, appkeysConfiguration);
  }

  /**
   * AppIdentityFilter expect the appkey as username.
   * If the user (header 'x-gbif-user') does not match appkey then the APP role will not be provided.
   */
  @Test
  public void testRandomUsername() throws Exception {
    // GIVEN
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("GBIF appkey:blabla");
    when(mockRequest.getHeader(SecurityConstants.HEADER_GBIF_USER)).thenReturn("myuser"); // user does not match appkey
    when(mockRequest.getInputStream()).thenReturn(new DelegatingServletInputStream(new ByteArrayInputStream(content.getBytes())));
    when(authServiceMock.isValidRequest(any(RequestObject.class))).thenReturn(true);

    // WHEN
    appIdentityFilter.doFilter(mockRequest, mockResponse, chain);

    // THEN
    assertNull(context.getAuthentication());
    verify(mockRequest).getHeader(HttpHeaders.AUTHORIZATION);
    verify(mockRequest).getHeader(SecurityConstants.HEADER_GBIF_USER);
    verify(mockRequest, atLeastOnce()).getInputStream();
    verify(authServiceMock).isValidRequest(any(RequestObject.class));
  }

  /**
   * Try with the appkey which matches the user header but not present in the white list.
   * So the APP role will not be provided.
   */
  @Test
  public void testAppkeyNotInWhiteList() throws Exception {
    // GIVEN
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("GBIF myuser:blabla");
    when(mockRequest.getHeader(SecurityConstants.HEADER_GBIF_USER)).thenReturn("myuser"); // user does not match appkey
    when(mockRequest.getInputStream()).thenReturn(new DelegatingServletInputStream(new ByteArrayInputStream(content.getBytes())));
    when(authServiceMock.isValidRequest(any(RequestObject.class))).thenReturn(true);

    // WHEN
    appIdentityFilter.doFilter(mockRequest, mockResponse, chain);

    // THEN
    assertNull(context.getAuthentication());
    verify(mockRequest).getHeader(HttpHeaders.AUTHORIZATION);
    verify(mockRequest).getHeader(SecurityConstants.HEADER_GBIF_USER);
    verify(mockRequest, atLeastOnce()).getInputStream();
    verify(authServiceMock).isValidRequest(any(RequestObject.class));
  }

  /**
   * Try with the appkey which matches the user header and presents in the white list.
   * The user should be successfully authenticated with the APP role.
   */
  @Test
  public void testRightRequest() throws Exception {
    // GIVEN
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("GBIF appkey:blabla");
    when(mockRequest.getHeader(SecurityConstants.HEADER_GBIF_USER)).thenReturn("appkey");
    when(mockRequest.getInputStream()).thenReturn(new DelegatingServletInputStream(new ByteArrayInputStream(content.getBytes())));
    when(authServiceMock.isValidRequest(any(RequestObject.class))).thenReturn(true);

    // WHEN
    appIdentityFilter.doFilter(mockRequest, mockResponse, chain);

    // THEN
    assertNotNull(context.getAuthentication());
    assertEquals("appkey", context.getAuthentication().getName());
    assertNotNull(context.getAuthentication().getAuthorities());
    assertEquals("APP", ((List<SimpleGrantedAuthority>) context.getAuthentication().getAuthorities()).get(0).getAuthority());
    verify(mockRequest).getHeader(HttpHeaders.AUTHORIZATION);
    verify(mockRequest).getHeader(SecurityConstants.HEADER_GBIF_USER);
    verify(mockRequest, atLeastOnce()).getInputStream();
    verify(authServiceMock).isValidRequest(any(RequestObject.class));
  }
}
