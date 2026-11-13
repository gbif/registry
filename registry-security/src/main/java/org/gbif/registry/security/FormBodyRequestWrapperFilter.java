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

import org.gbif.ws.server.GbifHttpServletRequestWrapper;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Wraps multipart and form requests without buffering their body.
 *
 * <p>gbif-common-ws' HttpServletRequestWrapperFilter buffers the body of every request it wraps.
 * The servlet container parses multipart parts and form parameters from the original input
 * stream, which is then empty: uploads lose their parts and legacy IPT forms their parameters. A
 * request already wrapped is left as it is by that filter, so wrapping these without content here
 * keeps their body for the container. Their Content-MD5 cannot be verified, as before
 * gbif-common-ws 2.1.4. It is not a bean, so it is only registered in the security filter chain.
 */
public class FormBodyRequestWrapperFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (isFormBody(request) && !(request instanceof GbifHttpServletRequestWrapper)) {
      filterChain.doFilter(new GbifHttpServletRequestWrapper(request, false), response);
    } else {
      filterChain.doFilter(request, response);
    }
  }

  static boolean isFormBody(HttpServletRequest request) {
    String contentType = request.getContentType();
    if (contentType == null) {
      return false;
    }
    try {
      MediaType mediaType = MediaType.parseMediaType(contentType);
      return MediaType.MULTIPART_FORM_DATA.includes(mediaType)
          || MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
