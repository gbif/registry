/*
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

import org.gbif.ws.WebApplicationException;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WebApplicationExceptionFilterTest {

  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain filterChain;

  private final WebApplicationExceptionFilter filter = new WebApplicationExceptionFilter();
  private final StringWriter body = new StringWriter();

  @BeforeEach
  public void setUp() throws Exception {
    lenient().when(response.getWriter()).thenReturn(new PrintWriter(body));
  }

  @Test
  public void testNoException() throws Exception {
    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(response, never()).setStatus(any(Integer.class));
  }

  @Test
  public void testWebApplicationExceptionAnswered() throws Exception {
    doThrow(new WebApplicationException("Forbidden to modify", HttpStatus.FORBIDDEN))
        .when(filterChain)
        .doFilter(request, response);

    filter.doFilter(request, response, filterChain);

    verify(response).resetBuffer();
    verify(response).setStatus(HttpStatus.FORBIDDEN.value());
    verify(response).setContentType("text/plain");
    assertEquals("Forbidden to modify", body.toString().trim());
  }

  @Test
  public void testCommittedResponseRethrows() throws Exception {
    WebApplicationException e =
        new WebApplicationException("Unauthorized", HttpStatus.UNAUTHORIZED);
    doThrow(e).when(filterChain).doFilter(request, response);
    when(response.isCommitted()).thenReturn(true);

    assertSame(
        e,
        assertThrows(
            WebApplicationException.class,
            () -> filter.doFilter(request, response, filterChain)));
    verify(response, never()).setStatus(any(Integer.class));
  }

  @Test
  public void testOtherExceptionsPropagate() throws Exception {
    doThrow(new IllegalStateException("boom")).when(filterChain).doFilter(request, response);

    assertThrows(
        IllegalStateException.class, () -> filter.doFilter(request, response, filterChain));
    verify(response, never()).setStatus(any(Integer.class));
  }
}
