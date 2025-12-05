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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Logs SecurityContext state after HttpServletRequestWrapperFilter.
 */
public class AfterHttpServletRequestWrapperFilterLogger extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(AfterHttpServletRequestWrapperFilterLogger.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      LOG.debug("[AfterHttpServletRequestWrapperFilter] {} {} - SecurityContext: NULL",
          request.getMethod(), request.getRequestURI());
    } else {
      LOG.debug("[AfterHttpServletRequestWrapperFilter] {} {} - SecurityContext: user='{}', authenticated={}, type={}, authorities={}",
          request.getMethod(), request.getRequestURI(), auth.getName(), auth.isAuthenticated(), 
          auth.getClass().getSimpleName(), auth.getAuthorities());
    }

    filterChain.doFilter(request, response);
  }
}

