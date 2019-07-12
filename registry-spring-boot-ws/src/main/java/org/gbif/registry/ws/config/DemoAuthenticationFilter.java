package org.gbif.registry.ws.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

// TODO: 2019-07-11 disable for now
// put authentication scheme to the context
//@Component
public class DemoAuthenticationFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    if (!"/admin/user".equals(request.getServletPath())) {
      String authHeader = request.getHeader("Authorization");
      String authenticationSchema = null;

      // TODO: 2019-07-11 check GBIF
      if (authHeader.contains("Basic")) {
        authenticationSchema = "Basic";
      }

      final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      final RegistryAuthentication registryAuthentication = new RegistryAuthentication(authentication, authenticationSchema);
      SecurityContextHolder.getContext().setAuthentication(registryAuthentication);
    }

    filterChain.doFilter(request, response);
  }

}
