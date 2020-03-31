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

import org.gbif.registry.directory.client.NodeClient;
import org.gbif.registry.directory.client.ParticipantClient;
import org.gbif.registry.directory.client.PersonClient;
import org.gbif.ws.client.ClientContract;
import org.gbif.ws.client.ClientErrorDecoder;
import org.gbif.ws.client.GbifAuthRequestInterceptor;
import org.gbif.ws.security.Md5EncodeService;
import org.gbif.ws.security.SigningService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import feign.Contract;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import feign.jackson.JacksonDecoder;

@Configuration
public class DirectoryClientConfiguration {

  private String url;
  private GbifAuthRequestInterceptor requestInterceptor;
  private Decoder decoder;
  private ErrorDecoder errorDecoder;
  private Contract contract;

  public DirectoryClientConfiguration(
      @Value("${directory.ws.url}") String url,
      @Value("${directory.app.key}") String appKey,
      @Value("${directory.app.secret}") String secretKey,
      @Qualifier("secretKeySigningService") SigningService signingService,
      Md5EncodeService md5EncodeService,
      @Qualifier("registryObjectMapper") ObjectMapper objectMapper) {
    this.url = url;
    this.requestInterceptor =
        new GbifAuthRequestInterceptor(appKey, secretKey, signingService, md5EncodeService);
    this.decoder = new JacksonDecoder(objectMapper);
    this.errorDecoder = new ClientErrorDecoder();
    this.contract = new ClientContract();
  }

  @Bean
  public NodeClient nodeClient() {
    return Feign.builder()
        .decoder(decoder)
        .errorDecoder(errorDecoder)
        .contract(contract)
        .requestInterceptor(requestInterceptor)
        .target(NodeClient.class, url);
  }

  @Bean
  public ParticipantClient participantClient() {
    return Feign.builder()
        .decoder(decoder)
        .errorDecoder(errorDecoder)
        .contract(contract)
        .requestInterceptor(requestInterceptor)
        .target(ParticipantClient.class, url);
  }

  @Bean
  public PersonClient personClient() {
    return Feign.builder()
        .decoder(decoder)
        .errorDecoder(errorDecoder)
        .contract(contract)
        .requestInterceptor(requestInterceptor)
        .target(PersonClient.class, url);
  }
}
