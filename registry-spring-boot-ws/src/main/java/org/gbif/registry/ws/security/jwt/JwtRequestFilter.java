package org.gbif.registry.ws.security.jwt;

import org.gbif.api.model.common.GbifUser;
import org.gbif.ws.security.GbifAuthentication;
import org.gbif.ws.security.GbifAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.gbif.ws.util.SecurityConstants.HEADER_TOKEN;

/**
 * Filter to validate the JWT tokens.
 * <p>
 * If the token is not present this validation is skipped.
 */
@Component
public class JwtRequestFilter extends GenericFilterBean {

  private static final Logger LOG = LoggerFactory.getLogger(JwtRequestFilter.class);

  //Patterns that catches case insensitive versions of word 'bearer'
  private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)bearer");

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
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    final HttpServletRequest httpRequest = (HttpServletRequest) request;
    final HttpServletResponse httpResponse = (HttpServletResponse) response;
    final Optional<String> token = findTokenInRequest(httpRequest);

    if (!token.isPresent()) {
      // if there is no token in the request we ignore this authentication
      LOG.debug("No JWT token present.");
      filterChain.doFilter(request, response);
    } else {
      try {
        final GbifUser gbifUser = jwtAuthenticateService.authenticate(token.get());

        LOG.debug("JWT successfully validated for user {}", gbifUser.getUserName());

        final UserDetails userDetails = userDetailsService.loadUserByUsername(gbifUser.getUserName());

        final GbifAuthentication gbifAuthentication =
          new GbifAuthenticationToken(userDetails, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(gbifAuthentication);

        // refresh the token and add it to the headers
        final String newToken = jwtIssuanceService.generateJwt(gbifUser.getUserName());
        httpResponse.addHeader(HEADER_TOKEN, newToken);
      } catch (GbifJwtException e) {
        LOG.warn("JWT validation failed: {}", e.getErrorCode());
        httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
      }

      filterChain.doFilter(httpRequest, httpResponse);
    }
  }

  /**
   * Tries to find the token in the {@link HttpHeaders#AUTHORIZATION} header.
   */
  public Optional<String> findTokenInRequest(HttpServletRequest request) {
    // check header first
    return Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
        .filter(this::containsBearer)
        .map(this::removeBearer);
  }

  /**
   * Removes 'bearer' token, leading an trailing whitespaces.
   *
   * @param token to be clean
   * @return a token without whitespaces and the word 'bearer'
   */
  private String removeBearer(String token) {
    return BEARER_PATTERN.matcher(token).replaceAll("").trim();
  }

  private boolean containsBearer(String header) {
    return BEARER_PATTERN.matcher(header).find();
  }
}
