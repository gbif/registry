package org.gbif.ws.server.filter;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.validation.constraints.NotNull;

/**
 * Class that always adds a CORS related headers to the response
 * that will make all GBIF webservices available for simple cross domain calls without JSONP.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS">Mozilla documentation</a>
 * @see <a href="http://en.wikipedia.org/wiki/Cross-origin_resource_sharing">Wikipedia</a>
 * @see <a href="http://www.cypressnorth.com/blog/programming/cross-domain-ajax-request-with-xml-response-for-iefirefoxchrome-safari-jquery/">Cross domain blog</a>
 */
@ControllerAdvice
public class CrossOriginResponseFilter implements ResponseBodyAdvice<Object> {

  @Override
  public boolean supports(@NotNull MethodParameter returnType,
                          @NotNull Class<? extends HttpMessageConverter<?>> converterType) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(Object body, @NotNull MethodParameter returnType,
                                @NotNull MediaType selectedContentType,
                                @NotNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                @NotNull ServerHttpRequest request,
                                @NotNull ServerHttpResponse response) {
    response.getHeaders().add("Access-Control-Allow-Origin", "*");
    response.getHeaders().add("Access-Control-Allow-Methods", "HEAD, GET, POST, DELETE, PUT, OPTIONS");

    // Used in response to a preflight request to indicate which HTTP headers can be used when making the actual request.
    // https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS#Preflighted_requests
    // we reflect the headers specified in the Access-Control-Request-Headers header of the request
    if (request.getHeaders().containsKey("Access-Control-Request-Headers")) {
      response.getHeaders().put("Access-Control-Allow-Headers", request.getHeaders().get("Access-Control-Request-Headers"));
    }

    return body;
  }
}
