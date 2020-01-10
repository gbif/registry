package org.gbif.registry.ws.advice;

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
import java.util.Optional;

/**
 * A class that exists to overwrite 401 response codes with a 403 if the client instructs it to by using the header
 * gbif-prefer-403-over-401=true.
 * This exists because ajax based clients (e.g. Angular, JQuery etc) do not get the response before the browser pops up
 * the Authentication window, which happens on a 401 response. This is a known limitation and this is relatively common
 * practice.
 */
@ControllerAdvice
public class AuthResponseCodeOverwriteFilter implements ResponseBodyAdvice<Object> {

  private static final String REQUEST_HEADER_OVERWRITE = "gbif-prefer-403-over-401";

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
    String header = Optional.of(request.getHeaders())
      .map(httpHeaders -> httpHeaders.get(REQUEST_HEADER_OVERWRITE))
      .map(requestHeaderOverwrite -> requestHeaderOverwrite.get(0))
      .orElse(null);

    int responseStatusCode = ((ServletServerHttpResponse) response).getServletResponse().getStatus();

    if (Boolean.parseBoolean(header) && responseStatusCode == HttpStatus.UNAUTHORIZED.value()) {
      response.setStatusCode(HttpStatus.FORBIDDEN);
    }

    return body;
  }
}
