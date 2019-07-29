package org.gbif.registry.ws.security.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filter to add the JWT token to the responses.
 * <p>
 * This filter is needed to add a newly generated token to the response. If there isn't a new token set in the request
 * nothing is added to the response.
 */
@Component
public class JwtResponseFilter extends GenericFilterBean {

  private static final Logger LOG = LoggerFactory.getLogger(JwtResponseFilter.class);

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
    final HttpServletRequest httpRequest = (HttpServletRequest) request;
    final String token = httpRequest.getHeader("token");

    if (token != null) {
      LOG.debug("Adding jwt token to the response");
      ((HttpServletResponse) response).addHeader("token", token);
      ((HttpServletResponse) response).addHeader("Access-Control-Expose-Headers", token);
    }

    filterChain.doFilter(request, response);
  }
}
