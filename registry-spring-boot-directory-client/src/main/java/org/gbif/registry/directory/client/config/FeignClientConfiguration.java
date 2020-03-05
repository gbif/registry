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
package org.gbif.registry.directory.client.config;

import org.gbif.api.ws.mixin.Mixins;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.security.Md5EncodeService;
import org.gbif.ws.security.PrivateKeyNotFoundException;
import org.gbif.ws.security.RequestDataToSign;
import org.gbif.ws.security.SigningService;

import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import feign.Contract;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import feign.jackson.JacksonDecoder;

@Configuration
public class FeignClientConfiguration {

  private SigningService signingService;
  private Md5EncodeService md5EncodeService;

  public FeignClientConfiguration(
      SigningService signingService, Md5EncodeService md5EncodeService) {
    this.signingService = signingService;
    this.md5EncodeService = md5EncodeService;
  }

  @Value("${directory.app.key}")
  private String appKey;

  /** Sign request, set required headers. */
  @Bean
  public RequestInterceptor requestInterceptor() {
    return requestTemplate -> {
      RequestDataToSign requestDataToSign = new RequestDataToSign();
      requestDataToSign.setMethod(requestTemplate.method());
      requestDataToSign.setUrl(removeQueryParameters(requestTemplate.url()));
      requestDataToSign.setUser(appKey);

      if ("POST".equals(requestTemplate.method()) || "PUT".equals(requestTemplate.method())) {
        Map<String, Collection<String>> headers = requestTemplate.headers();

        Collection<String> contentTypeHeaders = headers.get(HttpHeaders.CONTENT_TYPE);

        String contentType =
            (contentTypeHeaders != null && !contentTypeHeaders.isEmpty())
                ? contentTypeHeaders.iterator().next()
                : "application/json";
        requestDataToSign.setContentType(contentType);

        String contentMd5 = md5EncodeService.encode(requestTemplate.requestBody().asString());
        requestDataToSign.setContentTypeMd5(contentMd5);

        requestTemplate.header("Content-MD5", contentMd5);
      }

      try {
        String signature = signingService.buildSignature(requestDataToSign, appKey);

        requestTemplate.header("x-gbif-user", appKey);
        requestTemplate.header("Authorization", "GBIF " + appKey + ":" + signature);
      } catch (PrivateKeyNotFoundException e) {
        throw new WebApplicationException(
            "Private key was not found for the application " + appKey, HttpStatus.UNAUTHORIZED);
      }
    };
  }

  /** Remove query parameters from the URL. */
  private String removeQueryParameters(String url) {
    return url.split("\\?")[0];
  }

  @Bean
  public Decoder feignDecoder() {
    return new JacksonDecoder(customObjectMapper());
  }

  public ObjectMapper customObjectMapper() {
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    // determines whether encountering of unknown properties (ones that do not map to a property,
    // and there is no
    // "any setter" or handler that can handle it) should result in a failure (throwing a
    // JsonMappingException) or not.
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    // Enforce use of ISO-8601 format dates (http://wiki.fasterxml.com/JacksonFAQDateHandling)
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    Mixins.getPredefinedMixins().forEach(objectMapper::addMixIn);

    return objectMapper;
  }

  @Primary
  @Bean
  public Contract feignContract() {
    return new HierarchicalContract();
  }

  @Bean
  public ErrorDecoder errorDecoder() {
    return new ClientErrorDecoder();
  }
}
