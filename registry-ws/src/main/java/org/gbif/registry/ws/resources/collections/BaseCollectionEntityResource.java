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
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Commentable;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.collections.CollectionEntityService;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Base class to implement the CRUD methods of a {@link CollectionEntity}. */
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public abstract class BaseCollectionEntityResource<
    T extends CollectionEntity & Taggable & Identifiable & MachineTaggable & Commentable> {

  private static final Logger LOG = LoggerFactory.getLogger(BaseCollectionEntityResource.class);

  protected final Class<T> objectClass;
  protected final CollectionEntityService<T> collectionEntityService;

  protected BaseCollectionEntityResource(
      Class<T> objectClass, CollectionEntityService<T> collectionEntityService) {
    this.objectClass = objectClass;
    this.collectionEntityService = collectionEntityService;
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
