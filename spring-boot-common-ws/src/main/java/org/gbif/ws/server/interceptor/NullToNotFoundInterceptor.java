package org.gbif.ws.server.interceptor;

import org.gbif.ws.NotFoundException;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * This method interceptor throws a {@link NotFoundException} for every {@code null} return value of a method.
 * <p/>
 * This exception is mapped to a <em>404</em> response code (<em>Not found</em>).
 */
@ControllerAdvice
public class NullToNotFoundInterceptor implements ResponseBodyAdvice<Object> {

  @Override
  public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return returnType.getMethodAnnotation(NullToNotFound.class) != null;
  }

  @Override
  public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                ServerHttpRequest request, ServerHttpResponse response) {
    if (body == null) {
      throw new NotFoundException();
    }
    return body;
  }
}
