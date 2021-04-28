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
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.service.collections.PrimaryCollectionEntityService;
import org.gbif.registry.service.collections.merge.MergeService;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
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
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public abstract class PrimaryCollectionEntityResource<
        T extends
            CollectionEntity & Taggable & Identifiable & MachineTaggable & Contactable & Commentable
                & OccurrenceMappeable,
        R extends ChangeSuggestion<T>>
    extends BaseCollectionEntityResource<T> {

  private final MergeService<T> mergeService;
  private final PrimaryCollectionEntityService<T> primaryCollectionEntityService;
  private final ChangeSuggestionService<T, R> changeSuggestionService;

  protected PrimaryCollectionEntityResource(
      MergeService<T> mergeService,
      PrimaryCollectionEntityService<T> primaryCollectionEntityService,
      ChangeSuggestionService<T, R> changeSuggestionService,
      Class<T> objectClass) {
    super(objectClass, primaryCollectionEntityService);
    this.mergeService = mergeService;
    this.changeSuggestionService = changeSuggestionService;
    this.primaryCollectionEntityService = primaryCollectionEntityService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  public UUID create(@RequestBody @Trim T entity) {
    return primaryCollectionEntityService.create(entity);
  }

  @PutMapping(value = "{key}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  public void update(@RequestBody @Trim T entity) {
    primaryCollectionEntityService.update(entity);
  }

  @PostMapping(
      value = "{key}/contact",
      consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE})
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  public void addContact(@PathVariable("key") UUID entityKey, @RequestBody UUID personKey) {
    primaryCollectionEntityService.addContact(entityKey, personKey);
  }

  @DeleteMapping("{key}/contact/{personKey}")
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  public void removeContact(@PathVariable("key") UUID entityKey, @PathVariable UUID personKey) {
    primaryCollectionEntityService.removeContact(entityKey, personKey);
  }

  @GetMapping("{key}/contact")
  @Nullable
  public List<Person> listContacts(@PathVariable UUID key) {
    return primaryCollectionEntityService.listContacts(key);
  }

  @PostMapping(value = "{key}/occurrenceMapping", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  public int addOccurrenceMapping(
      @PathVariable("key") UUID entityKey, @RequestBody @Trim OccurrenceMapping occurrenceMapping) {
    return primaryCollectionEntityService.addOccurrenceMapping(entityKey, occurrenceMapping);
  }

  @GetMapping("{key}/occurrenceMapping")
  @Nullable
  public List<OccurrenceMapping> listOccurrenceMappings(@PathVariable("key") UUID uuid) {
    return primaryCollectionEntityService.listOccurrenceMappings(uuid);
  }

  @DeleteMapping("{key}/occurrenceMapping/{occurrenceMappingKey}")
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  public void deleteOccurrenceMapping(
      @PathVariable("key") UUID entityKey, @PathVariable int occurrenceMappingKey) {
    primaryCollectionEntityService.deleteOccurrenceMapping(entityKey, occurrenceMappingKey);
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
      @RequestParam(value = "status", required = false) Status status,
      @RequestParam(value = "type", required = false) Type type,
      Country country,
      @RequestParam(value = "proposedBy", required = false) String proposedBy,
      @RequestParam(value = "entityKey", required = false) UUID entityKey,
      Pageable page) {
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

  // TODO: move to gbif-api
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
