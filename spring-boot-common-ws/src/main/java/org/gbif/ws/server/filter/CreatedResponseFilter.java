package org.gbif.ws.server.filter;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Filter that updates http headers when a new resource is successfully created via a POST request unless
 * the response returns 204 No Content.
 *
 * The following headers are added or replaced if they existed:
 * <ul>
 *   <li>Http response code 201</li>
 *   <li>Location header is set accordingly based on returned key</li>
 * </ul>
 */
@ControllerAdvice
public class CreatedResponseFilter implements ResponseBodyAdvice<Object> {

  @Override
  public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                ServerHttpRequest request, ServerHttpResponse response) {
    final int intStatus = ((ServletServerHttpResponse) response).getServletResponse().getStatus();
    final HttpStatus httpStatus = HttpStatus.resolve(intStatus);

    if (request.getMethod() != null && request.getMethod() == HttpMethod.POST
        && httpStatus != HttpStatus.NO_CONTENT && httpStatus.is2xxSuccessful()) {
      response.setStatusCode(HttpStatus.CREATED);

      // if response contains the key, also set Location
      // TODO: 2019-08-01 implement this stuff (see gbif-common-ws)
    }

    return body;
  }
}
