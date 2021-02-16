package org.gbif.registry.security.precheck;

import org.gbif.ws.server.GbifHttpServletRequestWrapper;

import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/** Tests the {@link AuthPreCheckInterceptor}. */
@ExtendWith(MockitoExtension.class)
public class AuthPreCheckInterceptorTest {

  @Mock private GbifHttpServletRequestWrapper mockRequest;
  @Mock private HttpServletResponse mockResponse;
  @InjectMocks private AuthPreCheckInterceptor authPreCheckInterceptor;

  @Test
  public void checkPermissionsOnlyRequestTest() throws Exception {
    when(mockRequest.getParameter(AuthPreCheckInterceptor.CHECK_PERMISSIONS_ONLY_PARAM))
        .thenReturn("true");
    assertFalse(authPreCheckInterceptor.preHandle(mockRequest, mockResponse, new Object()));

    when(mockRequest.getParameter(AuthPreCheckInterceptor.CHECK_PERMISSIONS_ONLY_PARAM))
        .thenReturn("TRUE");
    assertFalse(authPreCheckInterceptor.preHandle(mockRequest, mockResponse, new Object()));

    when(mockRequest.getParameter(AuthPreCheckInterceptor.CHECK_PERMISSIONS_ONLY_PARAM))
        .thenReturn("True");
    assertFalse(authPreCheckInterceptor.preHandle(mockRequest, mockResponse, new Object()));

    when(mockRequest.getParameter(AuthPreCheckInterceptor.CHECK_PERMISSIONS_ONLY_PARAM))
        .thenReturn("false");
    assertTrue(authPreCheckInterceptor.preHandle(mockRequest, mockResponse, new Object()));

    when(mockRequest.getParameter(AuthPreCheckInterceptor.CHECK_PERMISSIONS_ONLY_PARAM))
        .thenReturn(null);
    assertTrue(authPreCheckInterceptor.preHandle(mockRequest, mockResponse, new Object()));
  }
}
