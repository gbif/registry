/*
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
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.MasterSourceMetadata;
import org.gbif.api.model.collections.OccurrenceMapping;
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
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Commentable;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.collections.CollectionEntityService;
import org.gbif.registry.persistence.mapper.collections.params.DuplicatesSearchParams;
import org.gbif.registry.service.collections.duplicates.DuplicatesService;
import org.gbif.registry.service.collections.merge.MergeService;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
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

/** Base class to implement the CRUD methods of a {@link CollectionEntity}. */
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public abstract class BaseCollectionEntityResource<
    T extends CollectionEntity & Taggable & Identifiable & MachineTaggable & Commentable,
    R extends ChangeSuggestion<T>> {

  private static final Logger LOG = LoggerFactory.getLogger(BaseCollectionEntityResource.class);

  protected final Class<T> objectClass;
  protected final CollectionEntityService<T> collectionEntityService;
  protected final MergeService<T> mergeService;
  protected final ChangeSuggestionService<T, R> changeSuggestionService;
  protected final DuplicatesService duplicatesService;

  protected BaseCollectionEntityResource(
      MergeService<T> mergeService,
      CollectionEntityService<T> collectionEntityService,
      ChangeSuggestionService<T, R> changeSuggestionService,
      DuplicatesService duplicatesService,
      Class<T> objectClass) {
    this.objectClass = objectClass;
    this.mergeService = mergeService;
    this.changeSuggestionService = changeSuggestionService;
    this.collectionEntityService = collectionEntityService;
    this.duplicatesService = duplicatesService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  public UUID create(@RequestBody @Trim T entity) {
    return collectionEntityService.create(entity);
  }

  @PutMapping(value = "{key}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  public void update(@PathVariable("key") UUID key, @RequestBody @Trim T entity) {
    checkArgument(key.equals(entity.getKey()));
    collectionEntityService.update(entity);
  }

  @PostMapping(
      value = "{key}/contactPerson",
      consumes = {MediaType.APPLICATION_JSON_VALUE})
  public int addContactPerson(
      @PathVariable("key") UUID entityKey, @RequestBody @Trim Contact contact) {
    return collectionEntityService.addContactPerson(entityKey, contact);
  }

  @PutMapping(
      value = "{key}/contactPerson/{contactKey}",
      consumes = {MediaType.APPLICATION_JSON_VALUE})
  public void updateContactPerson(
      @PathVariable("key") UUID entityKey,
      @PathVariable("contactKey") int contactKey,
      @RequestBody @Trim Contact contact) {
    checkArgument(
        contactKey == contact.getKey(),
        "The contact key in the path has to match the key in the body");
    collectionEntityService.updateContactPerson(entityKey, contact);
  }

  @DeleteMapping("{key}/contactPerson/{contactKey}")
  public void removeContactPerson(
      @PathVariable("key") UUID entityKey, @PathVariable int contactKey) {
    collectionEntityService.removeContactPerson(entityKey, contactKey);
  }

  @GetMapping("{key}/contactPerson")
  @Nullable
  public List<Contact> listContactPersons(@PathVariable UUID key) {
    return collectionEntityService.listContactPersons(key);
  }

  @PostMapping(value = "{key}/occurrenceMapping", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  public int addOccurrenceMapping(
      @PathVariable("key") UUID entityKey, @RequestBody @Trim OccurrenceMapping occurrenceMapping) {
    return collectionEntityService.addOccurrenceMapping(entityKey, occurrenceMapping);
  }

  @GetMapping("{key}/occurrenceMapping")
  @Nullable
  public List<OccurrenceMapping> listOccurrenceMappings(@PathVariable("key") UUID uuid) {
    return collectionEntityService.listOccurrenceMappings(uuid);
  }

  @DeleteMapping("{key}/occurrenceMapping/{occurrenceMappingKey}")
  public void deleteOccurrenceMapping(
      @PathVariable("key") UUID entityKey, @PathVariable int occurrenceMappingKey) {
    collectionEntityService.deleteOccurrenceMapping(entityKey, occurrenceMappingKey);
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
  public int createChangeSuggestion(@RequestBody @Trim R createSuggestion) {
    return changeSuggestionService.createChangeSuggestion(createSuggestion);
  }

  @PutMapping(value = "changeSuggestion/{key}")
  public void updateChangeSuggestion(
      @PathVariable("key") int key, @RequestBody @Trim R suggestion) {
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

  @PostMapping(value = "{key}/masterSourceMetadata", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  public int addMasterSourceMetadata(
      @PathVariable("key") UUID entityKey,
      @RequestBody @Trim MasterSourceMetadata masterSourceMetadata) {
    return collectionEntityService.addMasterSourceMetadata(entityKey, masterSourceMetadata);
  }

  @GetMapping("{key}/masterSourceMetadata")
  @Nullable
  public MasterSourceMetadata getMasterSourceMetadata(@PathVariable("key") UUID entityKey) {
    return collectionEntityService.getMasterSourceMetadata(entityKey);
  }

  @DeleteMapping("{key}/masterSourceMetadata")
  public void deleteMasterSourceMetadata(@PathVariable("key") UUID entityKey) {
    collectionEntityService.deleteMasterSourceMetadata(entityKey);
  }

  @DeleteMapping("{key}")
  public void delete(@PathVariable UUID key) {
    collectionEntityService.delete(key);
  }

  @PostMapping(value = "{key}/identifier", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  public int addIdentifier(
      @PathVariable("key") UUID entityKey, @RequestBody @Trim Identifier identifier) {
    return collectionEntityService.addIdentifier(entityKey, identifier);
  }

  @DeleteMapping("{key}/identifier/{identifierKey}")
  @Transactional
  public void deleteIdentifier(
      @PathVariable("key") UUID entityKey, @PathVariable int identifierKey) {
    collectionEntityService.deleteIdentifier(entityKey, identifierKey);
  }

  @GetMapping("{key}/identifier")
  @Nullable
  public List<Identifier> listIdentifiers(@PathVariable UUID key) {
    return collectionEntityService.listIdentifiers(key);
  }

  @PostMapping(value = "{key}/tag", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  public int addTag(@PathVariable("key") UUID entityKey, @RequestBody @Trim Tag tag) {
    return collectionEntityService.addTag(entityKey, tag);
  }

  @DeleteMapping("{key}/tag/{tagKey}")
  @Transactional
  public void deleteTag(@PathVariable("key") UUID entityKey, @PathVariable int tagKey) {
    collectionEntityService.deleteTag(entityKey, tagKey);
  }

  @GetMapping("{key}/tag")
  @Nullable
  public List<Tag> listTags(
      @PathVariable("key") UUID key,
      @RequestParam(value = "owner", required = false) String owner) {
    return collectionEntityService.listTags(key, owner);
  }

  @PostMapping(value = "{key}/machineTag", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  public int addMachineTag(
      @PathVariable("key") UUID targetEntityKey, @RequestBody @Trim MachineTag machineTag) {
    return collectionEntityService.addMachineTag(targetEntityKey, machineTag);
  }

  @DeleteMapping("{key}/machineTag/{machineTagKey:[0-9]+}")
  public void deleteMachineTagByMachineTagKey(
      @PathVariable("key") UUID targetEntityKey, @PathVariable("machineTagKey") int machineTagKey) {
    collectionEntityService.deleteMachineTag(targetEntityKey, machineTagKey);
  }

  @DeleteMapping("{key}/machineTag/{namespace:.*[^0-9]+.*}")
  public void deleteMachineTagsByNamespace(
      @PathVariable("key") UUID targetEntityKey, @PathVariable("namespace") String namespace) {
    collectionEntityService.deleteMachineTags(targetEntityKey, namespace);
  }

  @DeleteMapping("{key}/machineTag/{namespace}/{name}")
  public void deleteMachineTags(
      @PathVariable("key") UUID targetEntityKey,
      @PathVariable("namespace") String namespace,
      @PathVariable("name") String name) {
    collectionEntityService.deleteMachineTags(targetEntityKey, namespace, name);
  }

  @GetMapping("{key}/machineTag")
  public List<MachineTag> listMachineTags(@PathVariable("key") UUID targetEntityKey) {
    return collectionEntityService.listMachineTags(targetEntityKey);
  }

  @PostMapping(value = "{key}/comment", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  public int addComment(
      @PathVariable("key") UUID targetEntityKey, @RequestBody @Trim Comment comment) {
    return collectionEntityService.addComment(targetEntityKey, comment);
  }

  @DeleteMapping("{key}/comment/{commentKey}")
  public void deleteComment(
      @PathVariable("key") UUID targetEntityKey, @PathVariable("commentKey") int commentKey) {
    collectionEntityService.deleteComment(targetEntityKey, commentKey);
  }

  @GetMapping(value = "{key}/comment")
  public List<Comment> listComments(@PathVariable("key") UUID targetEntityKey) {
    return collectionEntityService.listComments(targetEntityKey);
  }
}
