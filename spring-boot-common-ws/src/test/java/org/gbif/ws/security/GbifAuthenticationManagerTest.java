package org.gbif.ws.security;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.server.RequestObject;
import org.gbif.ws.util.SecurityConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GbifAuthenticationManagerTest {

  @InjectMocks
  private GbifAuthenticationManagerImpl manager;

  @Mock
  private GbifAuthService authServiceMock;

  @Mock
  private IdentityAccessService identityAccessServiceMock;

  /**
   * Basic authentication.
   * User should be authenticated, GbifAuthentication object must not be null.
   */
  @Test
  public void testBasicSuccess() {
    // GIVEN
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic dXNlcjpwYXNzd29yZA==");
    when(identityAccessServiceMock.authenticate("user", "password")).thenReturn(new GbifUser());

    // WHEN
    final GbifAuthentication result = manager.authenticate(mockRequest);

    // THEN
    assertThat(result, is(notNullValue()));
    assertThat(result.getAuthenticationScheme(), is("BASIC"));
    verify(mockRequest).getHeader(HttpHeaders.AUTHORIZATION);
    verify(identityAccessServiceMock).authenticate("user", "password");
  }

  @Test
  public void testBasicFailWrongPassword() {
    // GIVEN
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic dXNlcjpwYXNzd29yZA==");

    // WHEN & THEN
    try {
      manager.authenticate(mockRequest);
      fail("Method 'authenticate' must throw a WebApplicationException");
    } catch (WebApplicationException e) {
      assertThat(e.getResponse().getStatusCodeValue(), is(401));
      verify(mockRequest).getHeader(HttpHeaders.AUTHORIZATION);
    }
  }

  @Test
  public void testBasicFailWrongAuthHeader() {
    // GIVEN
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic dXNlcg==");

    // WHEN & THEN
    try {
      manager.authenticate(mockRequest);
      fail("Method 'authenticate' must throw a WebApplicationException");
    } catch (WebApplicationException e) {
      assertThat(e.getResponse().getStatusCodeValue(), is(400));
      verify(mockRequest).getHeader(HttpHeaders.AUTHORIZATION);
    }
  }

  /**
   * Basic authentication with UUID username.
   * User should be authenticated as anonymous, GbifAuthentication object must not be null.
   */
  @Test
  public void testLegacySuccess() {
    // GIVEN
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    // username here it's a valid UUID
    when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic ZmExM2EzM2QtZWExZC00MDc4LTliMjAtZjdmZGIxZWIxNjlmOnBhc3N3b3Jk");

    // WHEN
    final GbifAuthentication result = manager.authenticate(mockRequest);

    // THEN
    assertThat(result, is(notNullValue()));
    assertThat(result.getAuthenticationScheme(), isEmptyString());
    verify(mockRequest).getHeader(HttpHeaders.AUTHORIZATION);
  }

  /**
   * GBIF authentication.
   * If the user is successfully identified by IdentityAccessService it will be authenticated.
   */
  @Test
  public void testGbifSuccess() {
    // GIVEN
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("GBIF user:blabla");
    when(mockRequest.getHeader(SecurityConstants.HEADER_GBIF_USER)).thenReturn("user");
    when(authServiceMock.isValidRequest(any(RequestObject.class))).thenReturn(true);
    when(identityAccessServiceMock.get("user")).thenReturn(new GbifUser());

    // WHEN
    final GbifAuthentication result = manager.authenticate(mockRequest);

    // THEN
    assertThat(result, is(notNullValue()));
    assertThat(result.getAuthenticationScheme(), is("GBIF"));
    verify(mockRequest).getHeader(HttpHeaders.AUTHORIZATION);
    verify(mockRequest).getHeader(SecurityConstants.HEADER_GBIF_USER);
    verify(authServiceMock).isValidRequest(any(RequestObject.class));
    verify(identityAccessServiceMock).get("user");
  }

  @Test
  public void testGbifFailNoUserHeader() {
    // GIVEN
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("GBIF user:blabla");

    // WHEN & THEN
    try {
      manager.authenticate(mockRequest);
      fail("Method 'authenticate' must throw a WebApplicationException");
    } catch (WebApplicationException e) {
      assertThat(e.getResponse().getStatusCodeValue(), is(400));
      verify(mockRequest).getHeader(HttpHeaders.AUTHORIZATION);
    }
  }

  @Test
  public void testGbifFailAuthServiceIsUnavailable() {
    // GIVEN
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("GBIF user:blabla");
    when(mockRequest.getHeader(SecurityConstants.HEADER_GBIF_USER)).thenReturn("user");
    GbifAuthenticationManager manager = new GbifAuthenticationManagerImpl(identityAccessServiceMock, null);

    // WHEN & THEN
    try {
      manager.authenticate(mockRequest);
      fail("Method 'authenticate' must throw a WebApplicationException");
    } catch (WebApplicationException e) {
      assertThat(e.getResponse().getStatusCodeValue(), is(401));
      verify(mockRequest).getHeader(HttpHeaders.AUTHORIZATION);
      verify(mockRequest).getHeader(SecurityConstants.HEADER_GBIF_USER);
    }
  }

  @Test
  public void testGbifFailRequestIsInvalid() {
    // GIVEN
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("GBIF user:blabla");
    when(mockRequest.getHeader(SecurityConstants.HEADER_GBIF_USER)).thenReturn("user");
    when(authServiceMock.isValidRequest(any(RequestObject.class))).thenReturn(false);

    // WHEN & THEN
    try {
      manager.authenticate(mockRequest);
      fail("Method 'authenticate' must throw a WebApplicationException");
    } catch (WebApplicationException e) {
      assertThat(e.getResponse().getStatusCodeValue(), is(401));
      verify(mockRequest).getHeader(HttpHeaders.AUTHORIZATION);
      verify(mockRequest).getHeader(SecurityConstants.HEADER_GBIF_USER);
      verify(authServiceMock).isValidRequest(any(RequestObject.class));
    }
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testAuthenticateByAuthenticationObjectThrowException() {
    Authentication authentication = mock(Authentication.class);
    manager.authenticate(authentication);
  }
}
