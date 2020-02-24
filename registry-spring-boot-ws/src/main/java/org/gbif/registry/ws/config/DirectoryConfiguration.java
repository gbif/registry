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

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.directory.Node;
import org.gbif.api.model.directory.NodePerson;
import org.gbif.api.model.directory.Participant;
import org.gbif.api.model.directory.ParticipantPerson;
import org.gbif.api.model.directory.Person;
import org.gbif.api.model.directory.PersonRole;
import org.gbif.api.service.directory.NodePersonService;
import org.gbif.api.service.directory.NodeService;
import org.gbif.api.service.directory.ParticipantPersonService;
import org.gbif.api.service.directory.ParticipantService;
import org.gbif.api.service.directory.PersonRoleService;
import org.gbif.api.service.directory.PersonService;
import org.gbif.registry.directory.client.DirectoryWsClientFactory;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

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
    return new ParticipantService() {
      @Override
      public Participant create(@NotNull Participant entity) {
        return null;
      }

      @Override
      public Participant get(@NotNull Integer id) {
        return null;
      }

      @Override
      public void update(@NotNull Participant entity) {}

      @Override
      public void delete(@NotNull Integer id) {}

      @Override
      public PagingResponse<Participant> list(@Nullable String query, @Nullable Pageable page) {
        return null;
      }
    };
  }

  @Bean
  @ConditionalOnProperty(value = "directory.enabled", havingValue = "false")
  public NodeService nodeServiceStub() {
    return new NodeService() {
      @Override
      public Node create(@NotNull Node node) {
        return null;
      }

      @Override
      public Node get(@NotNull Integer integer) {
        return null;
      }

      @Override
      public void update(@NotNull Node node) {}

      @Override
      public void delete(@NotNull Integer integer) {}

      @Override
      public PagingResponse<Node> list(@Nullable String s, @Nullable Pageable pageable) {
        return null;
      }
    };
  }

  @Bean
  @ConditionalOnProperty(value = "directory.enabled", havingValue = "false")
  public PersonService personServiceStub() {
    return new PersonService() {
      @Override
      public Person create(@NotNull Person person) {
        return null;
      }

      @Override
      public Person get(@NotNull Integer integer) {
        return null;
      }

      @Override
      public void update(@NotNull Person person) {}

      @Override
      public void delete(@NotNull Integer integer) {}

      @Override
      public PagingResponse<Person> list(@Nullable String s, @Nullable Pageable pageable) {
        return null;
      }
    };
  }

  @Bean
  @ConditionalOnProperty(value = "directory.enabled", havingValue = "false")
  public ParticipantPersonService participantPersonServiceStub() {
    return new ParticipantPersonService() {
      @Override
      public ParticipantPerson create(@NotNull ParticipantPerson participantPerson) {
        return null;
      }

      @Override
      public ParticipantPerson get(@NotNull Integer integer) {
        return null;
      }

      @Override
      public void update(@NotNull ParticipantPerson participantPerson) {}

      @Override
      public void delete(@NotNull Integer integer) {}

      @Override
      public PagingResponse<ParticipantPerson> list(
          @Nullable String s, @Nullable Pageable pageable) {
        return null;
      }
    };
  }

  @Bean
  @ConditionalOnProperty(value = "directory.enabled", havingValue = "false")
  public NodePersonService nodePersonServiceStub() {
    return new NodePersonService() {
      @Override
      public NodePerson create(@NotNull NodePerson nodePerson) {
        return null;
      }

      @Override
      public NodePerson get(@NotNull Integer integer) {
        return null;
      }

      @Override
      public void update(@NotNull NodePerson nodePerson) {}

      @Override
      public void delete(@NotNull Integer integer) {}

      @Override
      public PagingResponse<NodePerson> list(@Nullable String s, @Nullable Pageable pageable) {
        return null;
      }
    };
  }

  @Bean
  @ConditionalOnProperty(value = "directory.enabled", havingValue = "false")
  public PersonRoleService personRoleServiceStub() {
    return new PersonRoleService() {
      @Override
      public PersonRole create(@NotNull PersonRole personRole) {
        return null;
      }

      @Override
      public PersonRole get(@NotNull Integer integer) {
        return null;
      }

      @Override
      public void update(@NotNull PersonRole personRole) {}

      @Override
      public void delete(@NotNull Integer integer) {}

      @Override
      public PagingResponse<PersonRole> list(@Nullable String s, @Nullable Pageable pageable) {
        return null;
      }
    };
  }
}
