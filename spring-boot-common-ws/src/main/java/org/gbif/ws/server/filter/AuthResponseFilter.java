package org.gbif.ws.server.filter;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.validation.constraints.NotNull;

/**
 * Response filter that adds a WWW-Authenticate header if response status is Unauthenticated,
 * indicating a Basic Authentication scheme to be used.
 */
@ControllerAdvice
public class AuthResponseFilter implements ResponseBodyAdvice<Object> {

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
    if (response instanceof ServletServerHttpResponse) {
      int responseStatusCode = ((ServletServerHttpResponse) response).getServletResponse().getStatus();

      if (responseStatusCode == HttpStatus.UNAUTHORIZED.value()) {
        // TODO: 10/01/2020 real GBIF only?
        response.getHeaders().add("WWW-Authenticate", "Basic realm=\"GBIF\"");
      }
    }

    return body;
  }
}
