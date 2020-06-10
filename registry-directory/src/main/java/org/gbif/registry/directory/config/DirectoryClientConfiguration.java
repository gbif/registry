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
package org.gbif.registry.directory.config;

import org.gbif.api.service.directory.NodeService;
import org.gbif.api.service.directory.ParticipantService;
import org.gbif.api.service.directory.PersonService;
import org.gbif.directory.client.NodeClient;
import org.gbif.directory.client.ParticipantClient;
import org.gbif.directory.client.PersonClient;
import org.gbif.ws.client.ClientFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DirectoryClientConfiguration {

  private ClientFactory clientFactory;

  public DirectoryClientConfiguration(
      @Value("${api.root.url}") String url,
      @Value("${directory.app.key}") String appKey,
      @Value("${directory.app.secret}") String secretKey) {
    this.clientFactory = new ClientFactory(appKey, url, appKey, secretKey);
  }

  @Bean
  public NodeService directoryNodeClient() {
    return clientFactory.newInstance(NodeClient.class);
  }

  @Bean
  public ParticipantService directoryParticipantClient() {
    return clientFactory.newInstance(ParticipantClient.class);
  }

  @Bean
  public PersonService directoryPersonClient() {
    return clientFactory.newInstance(PersonClient.class);
  }
}
