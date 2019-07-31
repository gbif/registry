package org.gbif.ws.server.filter;

import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.security.GbifAuthentication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.stereotype.Component;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Override a built-in spring filter because of legacy behaviour.
 * <p>
 * Replacement for AuthFilter (legacy gbif-common-ws).
 * <p>
 * Server filter that looks for a http BasicAuthentication with user accounts based on a {@link IdentityAccessService}
 * or GBIF trusted application schema to impersonate a user and populates the security context.
 * <p>
 * As we have another custom authorization filter in the registry that understands a registry internal authentication,
 * all Basic authentication requests that have a UUID as the username are simply passed through and passwords are not
 * evaluated.
 */
@Component
public class IdentityFilter extends BasicAuthenticationFilter {

  public IdentityFilter(AuthenticationManager authenticationManager) {
    super(authenticationManager);
  }

  @Override
  public void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain) throws IOException, ServletException {
    // authenticates the HTTP method, but ignores legacy UUID user names
    try {
      final Authentication authentication = getAuthenticationManager().authenticate(new GbifAuthentication(null, null, null, request));
      SecurityContextHolder.getContext().setAuthentication(authentication);
    } catch (WebApplicationException e) {
      response.setStatus(e.getResponse().getStatusCode().value());
    }
    filterChain.doFilter(request, response);
  }
}
