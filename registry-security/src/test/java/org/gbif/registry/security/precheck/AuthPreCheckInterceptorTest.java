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
