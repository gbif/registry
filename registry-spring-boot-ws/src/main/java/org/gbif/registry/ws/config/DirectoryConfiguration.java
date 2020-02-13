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
package org.gbif.registry.ws.config;

import org.gbif.api.service.directory.NodePersonService;
import org.gbif.api.service.directory.NodeService;
import org.gbif.api.service.directory.ParticipantPersonService;
import org.gbif.api.service.directory.ParticipantService;
import org.gbif.api.service.directory.PersonRoleService;
import org.gbif.api.service.directory.PersonService;
import org.gbif.directory.client.DirectoryWsClientFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DirectoryConfiguration {
  private final DirectoryWsClientFactory directoryWsClientFactory;

  @Autowired
  public DirectoryConfiguration(
      @Value("${directory.app.key}") String directoryAppKey,
      @Value("${directory.app.secret}") String directorySecret,
      @Value("${directory.ws.url}") String directoryUrl) {

    directoryWsClientFactory =
        new DirectoryWsClientFactory(directoryAppKey, directorySecret, directoryUrl);
    System.out.println("Initializing stuff");
  }

  @Bean
  public ParticipantService provideParticipantService() {
    return directoryWsClientFactory.provideParticipantService();
  }

  @Bean
  public NodeService provideNodeService() {
    return directoryWsClientFactory.provideNodeService();
  }

  @Bean
  public PersonService providePersonService() {
    return directoryWsClientFactory.providePersonService();
  }

  @Bean
  public ParticipantPersonService provideParticipantPersonService() {
    return directoryWsClientFactory.provideParticipantPersonService();
  }

  @Bean
  public NodePersonService provideNodePersonService() {
    return directoryWsClientFactory.provideNodePersonService();
  }

  @Bean
  public PersonRoleService providePersonRoleService() {
    return directoryWsClientFactory.providePersonRoleService();
  }
}
