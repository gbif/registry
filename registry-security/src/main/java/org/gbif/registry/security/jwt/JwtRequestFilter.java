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
package org.gbif.registry.security.jwt;

import org.gbif.api.model.common.GbifUser;
import org.gbif.ws.security.GbifAuthentication;
import org.gbif.ws.security.GbifAuthenticationToken;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import static org.gbif.ws.util.SecurityConstants.HEADER_TOKEN;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;

/**
 * Filter to validate the JWT tokens.
 *
 * <p>If the token is not present this validation is skipped.
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(JwtRequestFilter.class);

  private final UserDetailsService userDetailsService;
  private final JwtAuthenticateService jwtAuthenticateService;
  private final JwtIssuanceService jwtIssuanceService;

  public JwtRequestFilter(
      @Qualifier("registryUserDetailsService") UserDetailsService userDetailsService,
      JwtAuthenticateService jwtAuthenticateService,
      JwtIssuanceService jwtIssuanceService) {
    this.userDetailsService = userDetailsService;
    this.jwtAuthenticateService = jwtAuthenticateService;
    this.jwtIssuanceService = jwtIssuanceService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    final Optional<String> token = JwtUtils.findTokenInRequest(request);

    if (!token.isPresent()) {
      // if there is no token in the request we ignore this authentication
      LOG.debug("No JWT token present.");
    } else {
      try {
        final GbifUser gbifUser = jwtAuthenticateService.authenticate(token.get());

        LOG.debug("JWT successfully validated for user {}", gbifUser.getUserName());

        final UserDetails userDetails =
            userDetailsService.loadUserByUsername(gbifUser.getUserName());

        final GbifAuthentication gbifAuthentication =
            new GbifAuthenticationToken(userDetails, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(gbifAuthentication);

        // refresh the token and add it to the headers
        final String newToken = jwtIssuanceService.generateJwt(gbifUser.getUserName());
        response.addHeader(HEADER_TOKEN, newToken);
        response.addHeader(ACCESS_CONTROL_EXPOSE_HEADERS, HEADER_TOKEN);
      } catch (GbifJwtException e) {
        LOG.warn("JWT validation failed: {}", e.getErrorCode());
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
      }
    }

    filterChain.doFilter(request, response);
  }
}
