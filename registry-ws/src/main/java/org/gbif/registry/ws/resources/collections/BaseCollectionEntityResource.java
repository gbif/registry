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
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Commentable;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.collections.CrudService;
import org.gbif.api.service.registry.CommentService;
import org.gbif.api.service.registry.IdentifierService;
import org.gbif.api.service.registry.MachineTagService;
import org.gbif.api.service.registry.TagService;
import org.gbif.api.vocabulary.TagName;
import org.gbif.api.vocabulary.TagNamespace;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.collections.ChangedCollectionEntityComponentEvent;
import org.gbif.registry.events.collections.DeleteCollectionEntityEvent;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.BaseMapper;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.groups.Default;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_EDITOR_ROLE;

/** Base class to implement the CRUD methods of a {@link CollectionEntity}. */
@Validated
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public abstract class BaseCollectionEntityResource<
        T extends CollectionEntity & Taggable & Identifiable & MachineTaggable & Commentable>
    implements CrudService<T>, TagService, IdentifierService, MachineTagService, CommentService {

  private static final Logger LOG = LoggerFactory.getLogger(BaseCollectionEntityResource.class);

  private final BaseMapper<T> baseMapper;
  private final Class<T> objectClass;
  private final TagMapper tagMapper;
  private final MachineTagMapper machineTagMapper;
  private final IdentifierMapper identifierMapper;
  private final CommentMapper commentMapper;
  private final EventManager eventManager;
  private final WithMyBatis withMyBatis;

  protected BaseCollectionEntityResource(
      BaseMapper<T> baseMapper,
      TagMapper tagMapper,
      MachineTagMapper machineTagMapper,
      IdentifierMapper identifierMapper,
      CommentMapper commentMapper,
      EventManager eventManager,
      Class<T> objectClass,
      WithMyBatis withMyBatis) {
    this.baseMapper = baseMapper;
    this.tagMapper = tagMapper;
    this.machineTagMapper = machineTagMapper;
    this.identifierMapper = identifierMapper;
    this.commentMapper = commentMapper;
    this.eventManager = eventManager;
    this.objectClass = objectClass;
    this.withMyBatis = withMyBatis;
  }

  public void preCreate(T entity) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final String username = authentication.getName();
    entity.setCreatedBy(username);
    entity.setModifiedBy(username);
  }

  @DeleteMapping("{key}")
  @Transactional
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Override
  public void delete(@PathVariable UUID key) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    T entityToDelete = get(key);
    checkArgument(entityToDelete != null, "Entity to delete doesn't exist");

    entityToDelete.setModifiedBy(authentication.getName());
    update(entityToDelete);

    baseMapper.delete(key);
    eventManager.post(DeleteCollectionEntityEvent.newInstance(entityToDelete, objectClass));
  }

  @Nullable
  @Override
  public T get(@PathVariable UUID key) {
    return baseMapper.get(key);
  }

  public void preUpdate(T entity) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    entity.setModifiedBy(authentication.getName());
  }

  @PostMapping(value = "{key}/identifier", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Trim
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Override
  public int addIdentifier(
      @PathVariable("key") UUID entityKey, @RequestBody @Trim Identifier identifier) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    identifier.setCreatedBy(authentication.getName());
    int identifierKey =
        withMyBatis.addIdentifier(identifierMapper, baseMapper, entityKey, identifier);
    eventManager.post(
        ChangedCollectionEntityComponentEvent.newInstance(
            entityKey, objectClass, Identifier.class));
    return identifierKey;
  }

  @DeleteMapping("{key}/identifier/{identifierKey}")
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Transactional
  @Override
  public void deleteIdentifier(
      @PathVariable("key") UUID entityKey, @PathVariable int identifierKey) {
    baseMapper.deleteIdentifier(entityKey, identifierKey);
    eventManager.post(
        ChangedCollectionEntityComponentEvent.newInstance(
            entityKey, objectClass, Identifier.class));
  }

  @GetMapping("{key}/identifier")
  @Nullable
  @Override
  public List<Identifier> listIdentifiers(@PathVariable UUID key) {
    return baseMapper.listIdentifiers(key);
  }

  @PostMapping(value = "{key}/tag", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Trim
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Override
  public int addTag(@PathVariable("key") UUID entityKey, @RequestBody @Trim Tag tag) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    tag.setCreatedBy(authentication.getName());
    int tagKey = withMyBatis.addTag(tagMapper, baseMapper, entityKey, tag);
    eventManager.post(
        ChangedCollectionEntityComponentEvent.newInstance(entityKey, objectClass, Tag.class));
    return tagKey;
  }

  @Override
  public int addTag(UUID key, String value) {
    Tag tag = new Tag();
    tag.setValue(value);
    return addTag(key, tag);
  }

  @DeleteMapping("{key}/tag/{tagKey}")
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Transactional
  @Override
  public void deleteTag(@PathVariable("key") UUID entityKey, @PathVariable int tagKey) {
    baseMapper.deleteTag(entityKey, tagKey);
    eventManager.post(
        ChangedCollectionEntityComponentEvent.newInstance(entityKey, objectClass, Tag.class));
  }

  @GetMapping("{key}/tag")
  @Nullable
  @Override
  public List<Tag> listTags(
      @PathVariable("key") UUID key,
      @RequestParam(value = "owner", required = false) String owner) {
    if (owner != null) {
      LOG.warn("Owner is not supported. Passed value: {}", owner);
    }
    return baseMapper.listTags(key);
  }

  /**
   * Adding most machineTags is restricted based on the namespace. For some tags, it is restricted
   * based on the editing role as usual.
   *
   * @param targetEntityKey key of target entity to add MachineTag to
   * @param machineTag MachineTag to add
   * @return key of MachineTag created
   */
  @PostMapping(value = "{key}/machineTag", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Secured(GRSCICOLL_ADMIN_ROLE)
  @Trim
  @Transactional
  @Override
  public int addMachineTag(
      @PathVariable("key") UUID targetEntityKey, @RequestBody @Trim MachineTag machineTag) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final String nameFromContext = authentication != null ? authentication.getName() : null;
    machineTag.setCreatedBy(nameFromContext);
    int key = withMyBatis.addMachineTag(machineTagMapper, baseMapper, targetEntityKey, machineTag);
    eventManager.post(
        ChangedCollectionEntityComponentEvent.newInstance(
            targetEntityKey, objectClass, MachineTag.class));
    return key;
  }

  @Transactional
  @Override
  public int addMachineTag(UUID targetEntityKey, String namespace, String name, String value) {
    MachineTag machineTag = new MachineTag();
    machineTag.setNamespace(namespace);
    machineTag.setName(name);
    machineTag.setValue(value);
    return addMachineTag(targetEntityKey, machineTag);
  }

  @Transactional
  @Override
  public int addMachineTag(UUID targetEntityKey, TagName tagName, String value) {
    MachineTag machineTag = MachineTag.newInstance(tagName, value);
    return addMachineTag(targetEntityKey, machineTag);
  }

  /**
   * The webservice method to delete a machine tag. Ensures that the caller is authorized to perform
   * the action by looking at the namespace.
   */
  @DeleteMapping("{key}/machineTag/{machineTagKey:[0-9]+}")
  @Secured(GRSCICOLL_ADMIN_ROLE)
  public void deleteMachineTagByMachineTagKey(
      @PathVariable("key") UUID targetEntityKey, @PathVariable("machineTagKey") int machineTagKey) {
    deleteMachineTag(targetEntityKey, machineTagKey);
  }

  /**
   * Deletes the MachineTag according to interface without security restrictions.
   *
   * @param targetEntityKey key of target entity to delete MachineTag from
   * @param machineTagKey key of MachineTag to delete
   */
  @Override
  public void deleteMachineTag(UUID targetEntityKey, int machineTagKey) {
    baseMapper.deleteMachineTag(targetEntityKey, machineTagKey);
  }

  /**
   * The webservice method to delete all machine tag in a namespace. Ensures that the caller is
   * authorized to perform the action by looking at the namespace.
   */
  @DeleteMapping("{key}/machineTag/{namespace:.*[^0-9]+.*}")
  @Secured(GRSCICOLL_ADMIN_ROLE)
  public void deleteMachineTagsByNamespace(
      @PathVariable("key") UUID targetEntityKey, @PathVariable("namespace") String namespace) {
    deleteMachineTags(targetEntityKey, namespace);
  }

  @Override
  public void deleteMachineTags(UUID targetEntityKey, TagNamespace tagNamespace) {
    deleteMachineTags(targetEntityKey, tagNamespace.getNamespace());
  }

  @Override
  public void deleteMachineTags(UUID targetEntityKey, String namespace) {
    baseMapper.deleteMachineTags(targetEntityKey, namespace, null);
    eventManager.post(
        ChangedCollectionEntityComponentEvent.newInstance(
            targetEntityKey, objectClass, MachineTag.class));
  }

  @Override
  public void deleteMachineTags(UUID targetEntityKey, TagName tagName) {
    deleteMachineTags(targetEntityKey, tagName.getNamespace().getNamespace(), tagName.getName());
    eventManager.post(
        ChangedCollectionEntityComponentEvent.newInstance(
            targetEntityKey, objectClass, MachineTag.class));
  }

  /**
   * The webservice method to delete all machine tag of a particular name in a namespace. Ensures
   * that the caller is authorized to perform the action by looking at the namespace.
   */
  @DeleteMapping("{key}/machineTag/{namespace}/{name}")
  @Secured(GRSCICOLL_ADMIN_ROLE)
  @Override
  public void deleteMachineTags(
      @PathVariable("key") UUID targetEntityKey,
      @PathVariable("namespace") String namespace,
      @PathVariable("name") String name) {
    baseMapper.deleteMachineTags(targetEntityKey, namespace, name);
    eventManager.post(
        ChangedCollectionEntityComponentEvent.newInstance(
            targetEntityKey, objectClass, MachineTag.class));
  }

  @GetMapping("{key}/machineTag")
  @Override
  public List<MachineTag> listMachineTags(@PathVariable("key") UUID targetEntityKey) {
    return baseMapper.listMachineTags(targetEntityKey);
  }

  public PagingResponse<T> listByMachineTag(
      String namespace, @Nullable String name, @Nullable String value, Pageable page) {
    page = page == null ? new PagingRequest() : page;
    return withMyBatis.listByMachineTag(baseMapper, namespace, name, value, page);
  }

  /**
   * This method ensures that the caller is authorized to perform the action and then adds the
   * server controlled fields for createdBy and modifiedBy.
   *
   * @param targetEntityKey key of target entity to add comment to
   * @param comment Comment to add
   * @return key of Comment created
   */
  @PostMapping(value = "{key}/comment", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Trim
  @Transactional
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Override
  public int addComment(
      @PathVariable("key") UUID targetEntityKey, @RequestBody @Trim Comment comment) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    comment.setCreatedBy(authentication.getName());
    comment.setModifiedBy(authentication.getName());
    int key = withMyBatis.addComment(commentMapper, baseMapper, targetEntityKey, comment);
    eventManager.post(
        ChangedCollectionEntityComponentEvent.newInstance(
            targetEntityKey, objectClass, Comment.class));
    return key;
  }

  /**
   * This method ensures that the caller is authorized to perform the action, and then deletes the
   * Comment.
   *
   * @param targetEntityKey key of target entity to delete comment from
   * @param commentKey key of Comment to delete
   */
  @DeleteMapping("{key}/comment/{commentKey}")
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Override
  public void deleteComment(
      @PathVariable("key") UUID targetEntityKey, @PathVariable int commentKey) {
    baseMapper.deleteComment(targetEntityKey, commentKey);
    eventManager.post(
        ChangedCollectionEntityComponentEvent.newInstance(
            targetEntityKey, objectClass, Comment.class));
  }

  @GetMapping(value = "{key}/comment")
  @Override
  public List<Comment> listComments(@PathVariable("key") UUID targetEntityKey) {
    return baseMapper.listComments(targetEntityKey);
  }
}
