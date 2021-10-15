/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.security.jwt;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import static org.gbif.ws.util.SecurityConstants.HEADER_TOKEN;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;

/**
 * Filter to add the JWT token to the responses.
 *
 * <p>This filter is needed to add a newly generated token to the response. If there isn't a new
 * token set in the request nothing is added to the response.
 */
@ControllerAdvice
public class JwtResponseFilter implements ResponseBodyAdvice<Object> {

  private static final Logger LOG = LoggerFactory.getLogger(JwtResponseFilter.class);

  @Override
  public boolean supports(
      MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter returnType,
      MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType,
      ServerHttpRequest request,
      ServerHttpResponse response) {
    final HttpServletRequest httpRequest = ((ServletServerHttpRequest) request).getServletRequest();
    final String token = httpRequest.getHeader(HEADER_TOKEN);

    if (token != null) {
      LOG.debug("Adding jwt token to the response");
      response.getHeaders().add(HEADER_TOKEN, token);
      response.getHeaders().add(ACCESS_CONTROL_EXPOSE_HEADERS, token);
    }

    return body;
  }
}
