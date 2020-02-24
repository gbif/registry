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
import org.gbif.registry.directory.client.DirectoryWsClientFactory;
import org.gbif.registry.directory.stub.NodePersonServiceStub;
import org.gbif.registry.directory.stub.NodeServiceStub;
import org.gbif.registry.directory.stub.ParticipantPersonServiceStub;
import org.gbif.registry.directory.stub.ParticipantServiceStub;
import org.gbif.registry.directory.stub.PersonRoleServiceStub;
import org.gbif.registry.directory.stub.PersonServiceStub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DirectoryConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(DirectoryConfiguration.class);

  private final DirectoryWsClientFactory directoryWsClientFactory;

  @Autowired
  public DirectoryConfiguration(
      @Value("${directory.enabled}") boolean enabled,
      @Value("${directory.app.key}") String directoryAppKey,
      @Value("${directory.app.secret}") String directorySecret,
      @Value("${directory.ws.url}") String directoryUrl) {
    if (enabled) {
      directoryWsClientFactory =
          new DirectoryWsClientFactory(directoryAppKey, directorySecret, directoryUrl);
      LOG.info("Initialize directory");
    } else {
      directoryWsClientFactory = null;
      LOG.info("Directory stubs activated");
    }
  }

  @Bean
  @ConditionalOnProperty(value = "directory.enabled", havingValue = "true")
  public ParticipantService provideParticipantService() {
    return directoryWsClientFactory.provideParticipantService();
  }

  @Bean
  @ConditionalOnProperty(value = "directory.enabled", havingValue = "true")
  public NodeService provideNodeService() {
    return directoryWsClientFactory.provideNodeService();
  }

  @Bean
  @ConditionalOnProperty(value = "directory.enabled", havingValue = "true")
  public PersonService providePersonService() {
    return directoryWsClientFactory.providePersonService();
  }

  @Bean
  @ConditionalOnProperty(value = "directory.enabled", havingValue = "true")
  public ParticipantPersonService provideParticipantPersonService() {
    return directoryWsClientFactory.provideParticipantPersonService();
  }

  @Bean
  @ConditionalOnProperty(value = "directory.enabled", havingValue = "true")
  public NodePersonService provideNodePersonService() {
    return directoryWsClientFactory.provideNodePersonService();
  }

  @Bean
  @ConditionalOnProperty(value = "directory.enabled", havingValue = "true")
  public PersonRoleService providePersonRoleService() {
    return directoryWsClientFactory.providePersonRoleService();
  }

  @Bean
  @ConditionalOnProperty(value = "directory.enabled", havingValue = "false")
  public ParticipantService participantServiceStub() {
    return new ParticipantServiceStub();
  }

  @Bean
  @ConditionalOnProperty(value = "directory.enabled", havingValue = "false")
  public NodeService nodeServiceStub() {
    return new NodeServiceStub();
  }

  @Bean
  @ConditionalOnProperty(value = "directory.enabled", havingValue = "false")
  public PersonService personServiceStub() {
    return new PersonServiceStub();
  }

  @Bean
  @ConditionalOnProperty(value = "directory.enabled", havingValue = "false")
  public ParticipantPersonService participantPersonServiceStub() {
    return new ParticipantPersonServiceStub();
  }

  @Bean
  @ConditionalOnProperty(value = "directory.enabled", havingValue = "false")
  public NodePersonService nodePersonServiceStub() {
    return new NodePersonServiceStub();
  }

  @Bean
  @ConditionalOnProperty(value = "directory.enabled", havingValue = "false")
  public PersonRoleService personRoleServiceStub() {
    return new PersonRoleServiceStub();
  }
}
