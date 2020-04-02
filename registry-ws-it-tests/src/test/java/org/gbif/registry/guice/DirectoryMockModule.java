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
package org.gbif.registry.guice;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.directory.Participant;
import org.gbif.api.model.directory.Person;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Node;
import org.gbif.api.service.directory.NodeService;
import org.gbif.api.service.directory.ParticipantService;
import org.gbif.api.service.directory.PersonService;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.registry.directory.Augmenter;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.google.inject.AbstractModule;

public class DirectoryMockModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Augmenter.class).to(AugmenterMock.class);
    bind(ParticipantService.class).to(ParticipantServiceMock.class);
    bind(NodeService.class).to(NodeServiceMock.class);
    bind(PersonService.class).to(PersonServiceMock.class);
  }

  private static class AugmenterMock implements Augmenter {
    @Override
    public Node augment(Node node) {
      if (node != null) {
        node.setParticipantSince(2001);
        node.setAbbreviation("GBIF.ES");
        node.setCity("Madrid");
        node.setPostalCode("E-28014");
        node.setOrganization("Real Jardín Botánico - CSIC");
        for (int i = 1; i < 8; i++) {
          Contact c = new Contact();
          c.setKey(i);
          c.setLastName("Name " + i);
          c.setType(ContactType.NODE_STAFF);
          node.getContacts().add(c);
        }
      }
      return node;
    }
  }

  /** Mock all directory services. */
  private static class NodeServiceMock implements NodeService {

    @Override
    public org.gbif.api.model.directory.Node create(
        @NotNull org.gbif.api.model.directory.Node node) {
      return null;
    }

    @Override
    public org.gbif.api.model.directory.Node get(@NotNull Integer integer) {
      return null;
    }

    @Override
    public void update(@NotNull org.gbif.api.model.directory.Node node) {}

    @Override
    public void delete(@NotNull Integer integer) {}

    @Override
    public PagingResponse<org.gbif.api.model.directory.Node> list(
        @Nullable String s, @Nullable Pageable pageable) {
      return null;
    }
  }

  private static class ParticipantServiceMock implements ParticipantService {

    @Override
    public Participant create(@NotNull Participant participant) {
      return null;
    }

    @Override
    public Participant get(@NotNull Integer integer) {
      return null;
    }

    @Override
    public void update(@NotNull Participant participant) {}

    @Override
    public void delete(@NotNull Integer integer) {}

    @Override
    public PagingResponse<Participant> list(@Nullable String s, @Nullable Pageable pageable) {
      return null;
    }
  }

  private static class PersonServiceMock implements PersonService {

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
  }
}
