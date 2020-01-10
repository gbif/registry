package org.gbif.ws.server.filter;

import com.google.common.base.Strings;
import org.gbif.ws.server.RequestObject;
import org.gbif.ws.util.ExtraMediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * A request filter that overwrites a few common http headers if their query parameter counterparts are given. In
 * particular the query parameters:
 * <dl>
 * <dt>callback</dt>
 * <dd>overwrites the Accept header with {@code application/javascript}</dd>
 * <dt>language</dt>
 * <dd>overwrites the Accept-Language header with the given language</dd>
 * </dl>
 */
@Component
public class RequestHeaderParamUpdateFilter extends GenericFilterBean {

  /**
   * A request filter that overwrites a few common http headers if their query parameter counterparts
   * are given.
   *
   * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html">HTTP 1.1 RFC 2616</a>
   */
  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    RequestObject httpRequestWrapper;
    if (servletRequest instanceof HttpServletRequest) {
      httpRequestWrapper = new RequestObject(((HttpServletRequest) servletRequest));

      // update media type for JSONP request
      processJsonp(httpRequestWrapper);

      // update language headers
      processLanguage(httpRequestWrapper);
    }
  }

  private static void processLanguage(RequestObject request) {
    String language = Strings.nullToEmpty(request.getParameter("language")).trim();
    if (!language.isEmpty()) {
      // overwrite http language
      request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, language.toLowerCase());
    }
  }

  private static void processJsonp(RequestObject request) {
    String callback = Strings.nullToEmpty(request.getParameter("callback")).trim();
    if (!callback.isEmpty()) {
      // this is a jsonp request - force content type to javascript
      request.addHeader(HttpHeaders.ACCEPT, ExtraMediaTypes.APPLICATION_JAVASCRIPT);
    }
  }
}
