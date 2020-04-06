/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.registry.ws.advice;

import org.gbif.api.model.pipelines.RunPipelineResponse;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@SuppressWarnings("NullableProblems")
@ControllerAdvice(basePackages = "org.gbif.registry.ws.resources.pipelines")
public class PipelinesResponseBodyAdvice implements ResponseBodyAdvice<RunPipelineResponse> {

  @Override
  public boolean supports(
      MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return returnType.getMethod() != null
        && returnType.getMethod().getReturnType() != RunPipelineResponse.class
        // exclude case when StringHttpMessageConverter is involved, that's an exception
        && !StringHttpMessageConverter.class.equals(converterType);
  }

  @Override
  public RunPipelineResponse beforeBodyWrite(
      RunPipelineResponse body,
      MethodParameter returnType,
      MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType,
      ServerHttpRequest request,
      ServerHttpResponse response) {
    // transform RunPipelineResponse.ResponseStatus to HttpStatus
    if (body.getResponseStatus() == RunPipelineResponse.ResponseStatus.PIPELINE_IN_SUBMITTED) {
      response.setStatusCode(HttpStatus.BAD_REQUEST);
    } else if (body.getResponseStatus() == RunPipelineResponse.ResponseStatus.UNSUPPORTED_STEP) {
      response.setStatusCode(HttpStatus.NOT_ACCEPTABLE);
    } else if (body.getResponseStatus() == RunPipelineResponse.ResponseStatus.ERROR) {
      response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    return body;
  }
}
