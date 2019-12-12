package org.gbif.ws.server.filter;

import org.gbif.ws.server.RequestObject;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
public class HttpServletRequestWrapperFilter extends GenericFilterBean {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    if (request instanceof HttpServletRequest) {
      final HttpServletRequest httpRequest = (HttpServletRequest) request;
      final RequestObject requestObject = new RequestObject(httpRequest);
      chain.doFilter(requestObject, response);
    } else {
      chain.doFilter(request, response);
    }
  }
}
