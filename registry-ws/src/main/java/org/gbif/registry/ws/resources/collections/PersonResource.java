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
package org.gbif.registry.ws.resources.collections;

import org.gbif.api.annotation.NullToNotFound;
import org.gbif.api.annotation.Trim;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.PostPersist;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.model.registry.search.collections.PersonSuggestResult;
import org.gbif.api.service.collections.PersonService;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.collections.CreateCollectionEntityEvent;
import org.gbif.registry.events.collections.UpdateCollectionEntityEvent;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.PersonMapper;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.groups.Default;

import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_EDITOR_ROLE;

@Validated
@RestController
@RequestMapping(value = "grscicoll/person", produces = MediaType.APPLICATION_JSON_VALUE)
public class PersonResource extends BaseCollectionEntityResource<Person> implements PersonService {

  private final PersonMapper personMapper;
  private final AddressMapper addressMapper;
  private final IdentifierMapper identifierMapper;
  private final TagMapper tagMapper;
  private final MachineTagMapper machineTagMapper;
  private final EventManager eventManager;

  public PersonResource(
      PersonMapper personMapper,
      AddressMapper addressMapper,
      IdentifierMapper identifierMapper,
      TagMapper tagMapper,
      MachineTagMapper machineTagMapper,
      CommentMapper commentMapper,
      EventManager eventManager,
      WithMyBatis withMyBatis) {
    super(
        personMapper,
        tagMapper,
        machineTagMapper,
        identifierMapper,
        commentMapper,
        eventManager,
        Person.class,
        withMyBatis);
    this.personMapper = personMapper;
    this.addressMapper = addressMapper;
    this.identifierMapper = identifierMapper;
    this.tagMapper = tagMapper;
    this.machineTagMapper = machineTagMapper;
    this.eventManager = eventManager;
  }

  @GetMapping("{key}")
  @NullToNotFound("/grscicoll/person/{key}")
  @Override
  public Person get(@PathVariable UUID key) {
    return super.get(key);
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Trim
  @Transactional
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Override
  public UUID create(@RequestBody @Trim Person person) {
    checkArgument(person.getKey() == null, "Unable to create an entity which already has a key");
    preCreate(person);

    if (person.getMailingAddress() != null) {
      addressMapper.create(person.getMailingAddress());
    }

    person.setKey(UUID.randomUUID());
    personMapper.create(person);

    eventManager.post(CreateCollectionEntityEvent.newInstance(person, Person.class));
    return person.getKey();
  }

  @PutMapping(value = "{key}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PostPersist.class, Default.class})
  @Trim
  @Transactional
  public void update(@PathVariable("key") UUID key, @Valid @RequestBody @Trim Person entity) {
    checkArgument(key.equals(entity.getKey()));
    update(entity);
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Transactional
  @Override
  public void update(Person person) {
    preUpdate(person);
    Person oldPerson = get(person.getKey());
    checkArgument(oldPerson != null, "Entity doesn't exist");

    if (oldPerson.getDeleted() != null) {
      // if it's deleted we only allow to update it if we undelete it
      checkArgument(
          person.getDeleted() == null,
          "Unable to update a previously deleted entity unless you clear the deletion timestamp");
    } else {
      // not allowed to delete when updating
      checkArgument(person.getDeleted() == null, "Can't delete an entity when updating");
    }

    // update mailing address
    if (person.getMailingAddress() != null) {
      if (oldPerson.getMailingAddress() == null) {
        checkArgument(
            person.getMailingAddress().getKey() == null,
            "Unable to create an address which already has a key");
        addressMapper.create(person.getMailingAddress());
      } else {
        addressMapper.update(person.getMailingAddress());
      }
    }

    // update entity
    personMapper.update(person);

    // check if we have to delete the mailing address
    if (person.getMailingAddress() == null && oldPerson.getMailingAddress() != null) {
      addressMapper.delete(oldPerson.getMailingAddress().getKey());
    }

    // check if we have to delete the address
    Person newPerson = get(person.getKey());
    eventManager.post(UpdateCollectionEntityEvent.newInstance(newPerson, oldPerson, Person.class));
  }

  @GetMapping
  @Override
  public PagingResponse<Person> list(
      @Nullable @RequestParam(value = "q", required = false) String query,
      @Nullable @RequestParam(value = "primaryInstitution", required = false) UUID institutionKey,
      @Nullable @RequestParam(value = "primaryCollection", required = false) UUID collectionKey,
      @Nullable Pageable page) {
    page = page == null ? new PagingRequest() : page;
    query = query != null ? Strings.emptyToNull(CharMatcher.WHITESPACE.trimFrom(query)) : query;
    long total = personMapper.count(institutionKey, collectionKey, query);
    return new PagingResponse<>(
        page, total, personMapper.list(institutionKey, collectionKey, query, page));
  }

  @GetMapping("deleted")
  @Override
  public PagingResponse<Person> listDeleted(Pageable page) {
    page = page == null ? new PagingRequest() : page;
    return new PagingResponse<>(page, personMapper.countDeleted(), personMapper.deleted(page));
  }

  @GetMapping("suggest")
  @Override
  public List<PersonSuggestResult> suggest(@RequestParam(value = "q", required = false) String q) {
    return personMapper.suggest(q);
  }
}
