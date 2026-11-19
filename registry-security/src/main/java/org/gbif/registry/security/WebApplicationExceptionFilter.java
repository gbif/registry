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

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Answers a {@link WebApplicationException} thrown by the registry security filters (legacy, JWT,
 * editor authorization, ...) with its status and message.
 *
 * <p>gbif-common-ws' IdentityFilter used to do this for every filter after it; since 2.1.4 it only
 * handles its own authentication errors. It is not a bean, so it is only registered in the security
 * filter chain.
 */
public class WebApplicationExceptionFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(WebApplicationExceptionFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      filterChain.doFilter(request, response);
    } catch (WebApplicationException e) {
      LOG.debug("Request rejected with status {}: {}", e.getStatus(), e.getMessage());
      if (response.isCommitted()) {
        throw e;
      }
      response.resetBuffer();
      response.setStatus(e.getStatus());
      response.setContentType(MediaType.TEXT_PLAIN_VALUE);
      response.setCharacterEncoding(StandardCharsets.UTF_8.name());
      response.getWriter().println(e.getMessage());
    }
  }
}
