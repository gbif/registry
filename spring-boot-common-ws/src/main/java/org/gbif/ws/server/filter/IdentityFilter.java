package org.gbif.ws.server.filter;

import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.security.GbifAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

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
public class IdentityFilter extends GenericFilterBean {

  private GbifAuthenticationManager authenticationManager;

  public IdentityFilter(GbifAuthenticationManager authenticationManager) {
    this.authenticationManager = authenticationManager;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
    throws ServletException, IOException {
    Objects.requireNonNull(request, "Can't filter null request");
    Objects.requireNonNull(response, "Can't filter null response");

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    // authenticates the HTTP method, but ignores legacy UUID user names
    try {
      final Authentication authentication = authenticationManager.authenticate(httpRequest);
      SecurityContextHolder.getContext().setAuthentication(authentication);
      filterChain.doFilter(httpRequest, httpResponse);
    } catch (final WebApplicationException e) {
      httpResponse.setStatus(e.getResponse().getStatusCode().value());
    }
  }
}
