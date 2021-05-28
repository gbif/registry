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
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.OccurrenceMappeable;
import org.gbif.api.model.collections.OccurrenceMapping;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.collections.PrimaryCollectionEntity;
import org.gbif.api.model.collections.duplicates.DuplicatesRequest;
import org.gbif.api.model.collections.duplicates.DuplicatesResult;
import org.gbif.api.model.collections.merge.MergeParams;
import org.gbif.api.model.collections.suggestions.ApplySuggestionResult;
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
import org.gbif.api.service.collections.PrimaryCollectionEntityService;
import org.gbif.registry.persistence.mapper.collections.params.DuplicatesSearchParams;
import org.gbif.registry.service.collections.duplicates.DuplicatesService;
import org.gbif.registry.service.collections.merge.MergeService;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Preconditions;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Base class to implement the main methods of {@link CollectionEntity} that are also @link
 * Taggable}, {@link Identifiable} and {@link Contactable}.
 *
 * <p>It inherits from {@link BaseCollectionEntityResource} to test the CRUD operations.
 */
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public abstract class PrimaryCollectionEntityResource<
        T extends
            PrimaryCollectionEntity & Taggable & Identifiable & MachineTaggable & Contactable
                & Commentable & OccurrenceMappeable,
        R extends ChangeSuggestion<T>>
    extends BaseCollectionEntityResource<T> {

  private final MergeService<T> mergeService;
  private final PrimaryCollectionEntityService<T> primaryCollectionEntityService;
  private final ChangeSuggestionService<T, R> changeSuggestionService;
  private final DuplicatesService duplicatesService;

  protected PrimaryCollectionEntityResource(
      MergeService<T> mergeService,
      PrimaryCollectionEntityService<T> primaryCollectionEntityService,
      ChangeSuggestionService<T, R> changeSuggestionService,
      DuplicatesService duplicatesService,
      Class<T> objectClass) {
    super(objectClass, primaryCollectionEntityService);
    this.mergeService = mergeService;
    this.changeSuggestionService = changeSuggestionService;
    this.primaryCollectionEntityService = primaryCollectionEntityService;
    this.duplicatesService = duplicatesService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  public UUID create(@RequestBody @Trim T entity) {
    return primaryCollectionEntityService.create(entity);
  }

  @PutMapping(value = "{key}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  public void update(@PathVariable("key") UUID key, @RequestBody @Trim T entity) {
    checkArgument(key.equals(entity.getKey()));
    primaryCollectionEntityService.update(entity);
  }

  @PostMapping(
      value = "{key}/contact",
      consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE})
  public void addContact(@PathVariable("key") UUID entityKey, @RequestBody UUID personKey) {
    primaryCollectionEntityService.addContact(entityKey, personKey);
  }

  @DeleteMapping("{key}/contact/{personKey}")
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
  public void deleteOccurrenceMapping(
      @PathVariable("key") UUID entityKey, @PathVariable int occurrenceMappingKey) {
    primaryCollectionEntityService.deleteOccurrenceMapping(entityKey, occurrenceMappingKey);
  }

  @PostMapping(value = "{key}/merge")
  public void merge(@PathVariable("key") UUID entityKey, @RequestBody MergeParams params) {
    mergeService.merge(entityKey, params.getReplacementEntityKey());
  }

  @GetMapping("possibleDuplicates")
  public DuplicatesResult findPossibleDuplicates(DuplicatesRequest request) {
    Preconditions.checkArgument(
        !request.isEmpty(), "At least one param to check the same field is required");

    return duplicatesService.findPossibleDuplicates(
        DuplicatesSearchParams.builder()
            .sameFuzzyName(request.getSameFuzzyName())
            .sameName(request.getSameName())
            .sameCode(request.getSameCode())
            .sameCountry(request.getSameCountry())
            .sameCity(request.getSameCity())
            .inCountries(request.getInCountries())
            .notInCountries(request.getNotInCountries())
            .excludeKeys(request.getExcludeKeys())
            .build());
  }

  @PostMapping(value = "changeSuggestion")
  public int createChangeSuggestion(@RequestBody R createSuggestion) {
    return changeSuggestionService.createChangeSuggestion(createSuggestion);
  }

  @PutMapping(value = "changeSuggestion/{key}")
  public void updateChangeSuggestion(@PathVariable("key") int key, @RequestBody R suggestion) {
    checkArgument(key == suggestion.getKey());
    changeSuggestionService.updateChangeSuggestion(suggestion);
  }

  @NullToNotFound
  @GetMapping(value = "changeSuggestion/{key}")
  public R getChangeSuggestion(@PathVariable("key") int key) {
    return changeSuggestionService.getChangeSuggestion(key);
  }

  @GetMapping(value = "changeSuggestion")
  public PagingResponse<R> listChangeSuggestion(
      @RequestParam(value = "status", required = false) Status status,
      @RequestParam(value = "type", required = false) Type type,
      @RequestParam(value = "proposerEmail", required = false) String proposerEmail,
      @RequestParam(value = "entityKey", required = false) UUID entityKey,
      Pageable page) {
    return changeSuggestionService.list(status, type, proposerEmail, entityKey, page);
  }

  @PutMapping(value = "changeSuggestion/{key}/discard")
  public void discardChangeSuggestion(@PathVariable("key") int key) {
    changeSuggestionService.discardChangeSuggestion(key);
  }

  @PutMapping(value = "changeSuggestion/{key}/apply")
  public ApplySuggestionResult applyChangeSuggestion(@PathVariable("key") int key) {
    UUID entityCreatedKey = changeSuggestionService.applyChangeSuggestion(key);
    ApplySuggestionResult result = new ApplySuggestionResult();
    result.setEntityCreatedKey(entityCreatedKey);
    return result;
  }
}
