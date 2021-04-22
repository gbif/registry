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

import org.gbif.api.annotation.Trim;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.OccurrenceMappeable;
import org.gbif.api.model.collections.OccurrenceMapping;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.collections.merge.MergeParams;
import org.gbif.api.model.collections.suggestions.ChangeSuggestion;
import org.gbif.api.model.collections.suggestions.ChangeSuggestionService;
import org.gbif.api.model.collections.suggestions.Status;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Commentable;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.collections.ContactService;
import org.gbif.api.service.collections.OccurrenceMappingService;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.collections.ChangedCollectionEntityComponentEvent;
import org.gbif.registry.events.collections.CreateCollectionEntityEvent;
import org.gbif.registry.events.collections.UpdateCollectionEntityEvent;
import org.gbif.registry.persistence.ContactableMapper;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.BaseMapper;
import org.gbif.registry.persistence.mapper.collections.OccurrenceMappeableMapper;
import org.gbif.registry.persistence.mapper.collections.OccurrenceMappingMapper;
import org.gbif.registry.service.collections.ExtendedCollectionService;
import org.gbif.registry.service.collections.merge.MergeService;
import org.gbif.ws.WebApplicationException;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_EDITOR_ROLE;
import static org.gbif.registry.security.UserRoles.IDIGBIO_GRSCICOLL_EDITOR_ROLE;

/**
 * Base class to implement the main methods of {@link CollectionEntity} that are also @link
 * Taggable}, {@link Identifiable} and {@link Contactable}.
 *
 * <p>It inherits from {@link BaseCollectionEntityResource} to test the CRUD operations.
 */
@Validated
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public abstract class ExtendedCollectionEntityResource<
        T extends
            CollectionEntity & Taggable & Identifiable & MachineTaggable & Contactable & Commentable
                & OccurrenceMappeable,
        R extends ChangeSuggestion<T>>
    extends BaseCollectionEntityResource<T> implements ContactService, OccurrenceMappingService {

  private final ContactableMapper contactableMapper;
  private final OccurrenceMappingMapper occurrenceMappingMapper;
  private final OccurrenceMappeableMapper occurrenceMappeableMapper;
  private final MergeService<T> mergeService;
  private final ExtendedCollectionService<T> extendedCollectionService;
  private final ChangeSuggestionService<T, R> changeSuggestionService;
  private final EventManager eventManager;
  private final Class<T> objectClass;

  protected ExtendedCollectionEntityResource(
      BaseMapper<T> baseMapper,
      TagMapper tagMapper,
      IdentifierMapper identifierMapper,
      ContactableMapper contactableMapper,
      MachineTagMapper machineTagMapper,
      CommentMapper commentMapper,
      OccurrenceMappingMapper occurrenceMappingMapper,
      OccurrenceMappeableMapper occurrenceMappeableMapper,
      MergeService<T> mergeService,
      ExtendedCollectionService<T> extendedCollectionService,
      ChangeSuggestionService<T, R> changeSuggestionService,
      EventManager eventManager,
      Class<T> objectClass,
      WithMyBatis withMyBatis) {
    super(
        baseMapper,
        tagMapper,
        machineTagMapper,
        identifierMapper,
        commentMapper,
        eventManager,
        objectClass,
        withMyBatis,
        extendedCollectionService);
    this.contactableMapper = contactableMapper;
    this.occurrenceMappingMapper = occurrenceMappingMapper;
    this.occurrenceMappeableMapper = occurrenceMappeableMapper;
    this.mergeService = mergeService;
    this.changeSuggestionService = changeSuggestionService;
    this.eventManager = eventManager;
    this.objectClass = objectClass;
    this.extendedCollectionService = extendedCollectionService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Trim
  @Transactional
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Override
  public UUID create(@RequestBody @Trim T entity) {
    UUID key = extendedCollectionService.create(entity);
    entity.setKey(key);
    eventManager.post(CreateCollectionEntityEvent.newInstance(entity, objectClass));
    return key;
  }

  @PutMapping(
      value = {"", "{key}"},
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Transactional
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Override
  public void update(@RequestBody @Trim T entity) {
    T entityOld = get(entity.getKey());
    extendedCollectionService.update(entity);
    T newEntity = get(entity.getKey());

    // TODO: move this to service??
    eventManager.post(UpdateCollectionEntityEvent.newInstance(newEntity, entityOld, objectClass));
  }

  @PostMapping(
      value = "{key}/contact",
      consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE})
  @Transactional
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Override
  public void addContact(
      @PathVariable("key") @NotNull UUID entityKey, @RequestBody @NotNull UUID personKey) {
    // check if the contact exists
    List<Person> contacts = contactableMapper.listContacts(entityKey);

    if (contacts != null && contacts.stream().anyMatch(p -> p.getKey().equals(personKey))) {
      throw new WebApplicationException("Duplicate contact", HttpStatus.CONFLICT);
    }

    contactableMapper.addContact(entityKey, personKey);
    eventManager.post(
        ChangedCollectionEntityComponentEvent.newInstance(entityKey, objectClass, Person.class));
  }

  @DeleteMapping("{key}/contact/{personKey}")
  @Transactional
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Override
  public void removeContact(
      @PathVariable("key") @NotNull UUID entityKey, @PathVariable @NotNull UUID personKey) {
    contactableMapper.removeContact(entityKey, personKey);
    eventManager.post(
        ChangedCollectionEntityComponentEvent.newInstance(entityKey, objectClass, Person.class));
  }

  @GetMapping("{key}/contact")
  @Nullable
  @Override
  public List<Person> listContacts(@PathVariable UUID key) {
    return contactableMapper.listContacts(key);
  }

  @PostMapping(value = "{key}/occurrenceMapping", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Transactional
  @Trim
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Override
  public int addOccurrenceMapping(
      @PathVariable("key") UUID entityKey, @RequestBody @Trim OccurrenceMapping occurrenceMapping) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    occurrenceMapping.setCreatedBy(authentication.getName());
    checkArgument(
        occurrenceMapping.getKey() == null, "Unable to create an entity which already has a key");
    occurrenceMappingMapper.createOccurrenceMapping(occurrenceMapping);
    occurrenceMappeableMapper.addOccurrenceMapping(entityKey, occurrenceMapping.getKey());

    eventManager.post(
        ChangedCollectionEntityComponentEvent.newInstance(
            entityKey, objectClass, OccurrenceMapping.class));

    return occurrenceMapping.getKey();
  }

  @GetMapping("{key}/occurrenceMapping")
  @Nullable
  @Override
  public List<OccurrenceMapping> listOccurrenceMappings(@NotNull @PathVariable("key") UUID uuid) {
    return occurrenceMappeableMapper.listOccurrenceMappings(uuid);
  }

  @DeleteMapping("{key}/occurrenceMapping/{occurrenceMappingKey}")
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Transactional
  @Override
  public void deleteOccurrenceMapping(
      @PathVariable("key") UUID entityKey, @PathVariable int occurrenceMappingKey) {
    occurrenceMappeableMapper.deleteOccurrenceMapping(entityKey, occurrenceMappingKey);
    eventManager.post(
        ChangedCollectionEntityComponentEvent.newInstance(
            entityKey, objectClass, OccurrenceMapping.class));
  }

  @PostMapping(value = "{key}/merge")
  @Secured({GRSCICOLL_ADMIN_ROLE, IDIGBIO_GRSCICOLL_EDITOR_ROLE})
  public void merge(@PathVariable("key") UUID entityKey, @RequestBody MergeParams params) {
    mergeService.merge(entityKey, params.getReplacementEntityKey());
  }

  @PostMapping(value = "changeSuggestion")
  public int createChangeSuggestion(@RequestBody R createSuggestion) {
    return changeSuggestionService.createChangeSuggestion(createSuggestion);
  }

  // TODO: suggestions roles

  @PutMapping(value = "changeSuggestion/{key}")
  @Secured({GRSCICOLL_ADMIN_ROLE})
  public void updateChangeSuggestion(@PathVariable("key") int key, @RequestBody R suggestion) {
    checkArgument(key == suggestion.getKey());
    changeSuggestionService.updateChangeSuggestion(suggestion);
  }

  @GetMapping(value = "changeSuggestion/{key}")
  public R getChangeSuggestion(@PathVariable("key") int key) {
    return changeSuggestionService.getChangeSuggestion(key);
  }

  @GetMapping(value = "changeSuggestion")
  public PagingResponse<R> listChangeSuggestion(
      @Nullable @RequestParam(value = "status", required = false) Status status,
      @Nullable @RequestParam(value = "type", required = false) Type type,
      @Nullable Country country,
      @Nullable @RequestParam(value = "proposedBy", required = false) String proposedBy,
      @Nullable @RequestParam(value = "entityKey", required = false) UUID entityKey,
      @Nullable Pageable page) {
    return changeSuggestionService.list(status, type, country, proposedBy, entityKey, page);
  }

  @PutMapping(value = "changeSuggestion/{key}/discard")
  @Secured({GRSCICOLL_ADMIN_ROLE})
  public void discardChangeSuggestion(@PathVariable("key") int key) {
    changeSuggestionService.discardChangeSuggestion(key);
  }

  @PutMapping(value = "changeSuggestion/{key}/apply")
  @Secured({GRSCICOLL_ADMIN_ROLE})
  public ApplySuggestionResult applyChangeSuggestion(@PathVariable("key") int key) {
    UUID entityCreatedKey = changeSuggestionService.applyChangeSuggestion(key);
    ApplySuggestionResult result = new ApplySuggestionResult();
    result.setEntityCreatedKey(entityCreatedKey);
    return result;
  }

  public static class ApplySuggestionResult {
    private UUID entityCreatedKey;

    public UUID getEntityCreatedKey() {
      return entityCreatedKey;
    }

    public void setEntityCreatedKey(UUID entityCreatedKey) {
      this.entityCreatedKey = entityCreatedKey;
    }
  }
}
