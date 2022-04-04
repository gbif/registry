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
package org.gbif.registry.service.collections;

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
import org.gbif.api.service.collections.CollectionEntityService;
import org.gbif.api.util.IdentifierUtils;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.TagName;
import org.gbif.api.vocabulary.TagNamespace;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.collections.DeleteCollectionEntityEvent;
import org.gbif.registry.events.collections.EventType;
import org.gbif.registry.events.collections.SubEntityCollectionEvent;
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.BaseMapper;
import org.gbif.registry.service.WithMyBatis;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.groups.Default;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_EDITOR_ROLE;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_MEDIATOR_ROLE;

@Validated
public abstract class BaseCollectionEntityService<
        T extends CollectionEntity & Taggable & Identifiable & MachineTaggable & Commentable>
    implements CollectionEntityService<T> {

  private static final Logger LOG = LoggerFactory.getLogger(BaseCollectionEntityService.class);

  protected final Class<T> objectClass;
  protected final BaseMapper<T> baseMapper;
  protected final TagMapper tagMapper;
  protected final MachineTagMapper machineTagMapper;
  protected final IdentifierMapper identifierMapper;
  protected final CommentMapper commentMapper;
  protected final EventManager eventManager;
  protected final WithMyBatis withMyBatis;

  protected BaseCollectionEntityService(
      BaseMapper<T> baseMapper,
      TagMapper tagMapper,
      MachineTagMapper machineTagMapper,
      IdentifierMapper identifierMapper,
      CommentMapper commentMapper,
      Class<T> objectClass,
      EventManager eventManager,
      WithMyBatis withMyBatis) {
    this.baseMapper = baseMapper;
    this.tagMapper = tagMapper;
    this.machineTagMapper = machineTagMapper;
    this.identifierMapper = identifierMapper;
    this.commentMapper = commentMapper;
    this.objectClass = objectClass;
    this.eventManager = eventManager;
    this.withMyBatis = withMyBatis;
  }

  @Override
  public T get(UUID key) {
    return baseMapper.get(key);
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Transactional
  @Override
  public void delete(UUID key) {
    T entityToDelete = get(key);
    delete(entityToDelete);
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Transactional
  protected void delete(T entityToDelete) {
    checkArgument(entityToDelete != null, "Entity to delete doesn't exist");
    checkArgument(entityToDelete.getKey() != null, "Entity to delete doesn't have key");

    // updates audit fields like the modifiedBy
    update(entityToDelete);

    baseMapper.delete(entityToDelete.getKey());

    eventManager.post(
        DeleteCollectionEntityEvent.newInstance(entityToDelete, get(entityToDelete.getKey())));
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Transactional
  @Validated({PrePersist.class, Default.class})
  @Override
  public int addIdentifier(UUID entityKey, Identifier identifier) {
    validateIdentifier(identifier);
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    identifier.setCreatedBy(authentication.getName());
    int identifierKey =
        withMyBatis.addIdentifier(identifierMapper, baseMapper, entityKey, identifier);
    eventManager.post(
        SubEntityCollectionEvent.newInstance(
            entityKey, objectClass, identifier, identifierKey, EventType.CREATE));
    return identifierKey;
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Transactional
  @Override
  public void deleteIdentifier(UUID entityKey, int identifierKey) {
    Identifier identifierToDelete = identifierMapper.get(identifierKey);
    checkArgument(identifierToDelete != null, "Identifier to delete doesn't exist");

    baseMapper.deleteIdentifier(entityKey, identifierKey);

    eventManager.post(
        SubEntityCollectionEvent.newInstance(
            entityKey, objectClass, identifierToDelete, identifierKey, EventType.DELETE));
  }

  @Override
  public List<Identifier> listIdentifiers(UUID key) {
    return baseMapper.listIdentifiers(key);
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Validated({PrePersist.class, Default.class})
  @Override
  public int addTag(UUID entityKey, Tag tag) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    tag.setCreatedBy(authentication.getName());
    int tagKey = withMyBatis.addTag(tagMapper, baseMapper, entityKey, tag);
    eventManager.post(
        SubEntityCollectionEvent.newInstance(
            entityKey, objectClass, tag, tagKey, EventType.CREATE));
    return tagKey;
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Override
  public int addTag(UUID key, String value) {
    Tag tag = new Tag();
    tag.setValue(value);
    return addTag(key, tag);
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Override
  public void deleteTag(UUID entityKey, int tagKey) {
    Tag tagToDelete = tagMapper.get(tagKey);
    checkArgument(tagToDelete != null, "Tag to delete doesn't exist");

    baseMapper.deleteTag(entityKey, tagKey);
    eventManager.post(
        SubEntityCollectionEvent.newInstance(
            entityKey, objectClass, tagToDelete, tagKey, EventType.DELETE));
  }

  @Override
  public List<Tag> listTags(UUID key, String owner) {
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
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Transactional
  @Override
  public int addMachineTag(UUID targetEntityKey, MachineTag machineTag) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final String nameFromContext = authentication != null ? authentication.getName() : null;
    machineTag.setCreatedBy(nameFromContext);
    int key = withMyBatis.addMachineTag(machineTagMapper, baseMapper, targetEntityKey, machineTag);
    eventManager.post(
        SubEntityCollectionEvent.newInstance(
            targetEntityKey, objectClass, machineTag, key, EventType.CREATE));
    return key;
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Transactional
  @Validated({PrePersist.class, Default.class})
  @Override
  public int addMachineTag(UUID targetEntityKey, String namespace, String name, String value) {
    MachineTag machineTag = new MachineTag();
    machineTag.setNamespace(namespace);
    machineTag.setName(name);
    machineTag.setValue(value);
    return addMachineTag(targetEntityKey, machineTag);
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Transactional
  @Override
  public int addMachineTag(UUID targetEntityKey, TagName tagName, String value) {
    MachineTag machineTag = MachineTag.newInstance(tagName, value);
    return addMachineTag(targetEntityKey, machineTag);
  }

  /**
   * Deletes the MachineTag according to interface without security restrictions.
   *
   * @param targetEntityKey key of target entity to delete MachineTag from
   * @param machineTagKey key of MachineTag to delete
   */
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Override
  public void deleteMachineTag(UUID targetEntityKey, int machineTagKey) {
    MachineTag machineTagToDelete = machineTagMapper.get(machineTagKey);
    checkArgument(machineTagToDelete != null, "Machine Tag to delete doesn't exist");

    baseMapper.deleteMachineTag(targetEntityKey, machineTagKey);
    eventManager.post(
        SubEntityCollectionEvent.newInstance(
            targetEntityKey, objectClass, machineTagToDelete, machineTagKey, EventType.DELETE));
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Override
  public void deleteMachineTags(UUID targetEntityKey, TagNamespace tagNamespace) {
    deleteMachineTags(targetEntityKey, tagNamespace.getNamespace());
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Override
  public void deleteMachineTags(UUID targetEntityKey, String namespace) {
    List<MachineTag> machineTagsToDelete =
        baseMapper.listMachineTags(targetEntityKey).stream()
            .filter(mt -> mt.getNamespace().equals(namespace))
            .collect(Collectors.toList());

    // we delete the machine tags one by one to make it easier for children classes to override the
    // deletion of machine tags behaviour
    machineTagsToDelete.forEach(
        mt -> {
          deleteMachineTag(targetEntityKey, mt.getKey());
          eventManager.post(
              SubEntityCollectionEvent.newInstance(
                  targetEntityKey, objectClass, mt, mt.getKey(), EventType.DELETE));
        });
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Override
  public void deleteMachineTags(UUID targetEntityKey, TagName tagName) {
    deleteMachineTags(targetEntityKey, tagName.getNamespace().getNamespace(), tagName.getName());
  }

  /**
   * The webservice method to delete all machine tag of a particular name in a namespace. Ensures
   * that the caller is authorized to perform the action by looking at the namespace.
   */
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Override
  public void deleteMachineTags(UUID targetEntityKey, String namespace, String name) {
    List<MachineTag> machineTagsToDelete =
        baseMapper.listMachineTags(targetEntityKey).stream()
            .filter(mt -> mt.getNamespace().equals(namespace) && mt.getName().equals(name))
            .collect(Collectors.toList());

    // we delete the machine tags one by one to make it easier for children classes to override the
    // deletion of machine tags behaviour
    machineTagsToDelete.forEach(
        mt -> {
          deleteMachineTag(targetEntityKey, mt.getKey());
          eventManager.post(
              SubEntityCollectionEvent.newInstance(
                  targetEntityKey, objectClass, mt, mt.getKey(), EventType.DELETE));
        });
  }

  @Override
  public List<MachineTag> listMachineTags(UUID targetEntityKey) {
    return baseMapper.listMachineTags(targetEntityKey);
  }

  @Override
  public PagingResponse<T> listByMachineTag(
      String namespace,
      @Nullable String name,
      @Nullable String value,
      @Nullable Pageable pageable) {
    pageable = pageable == null ? new PagingRequest() : pageable;
    return withMyBatis.listByMachineTag(baseMapper, namespace, name, value, pageable);
  }

  /**
   * This method ensures that the caller is authorized to perform the action and then adds the
   * server controlled fields for createdBy and modifiedBy.
   *
   * @param targetEntityKey key of target entity to add comment to
   * @param comment Comment to add
   * @return key of Comment created
   */
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Transactional
  @Validated({PrePersist.class, Default.class})
  @Override
  public int addComment(UUID targetEntityKey, Comment comment) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    comment.setCreatedBy(authentication.getName());
    comment.setModifiedBy(authentication.getName());
    int key = withMyBatis.addComment(commentMapper, baseMapper, targetEntityKey, comment);
    eventManager.post(
        SubEntityCollectionEvent.newInstance(
            targetEntityKey, objectClass, comment, key, EventType.CREATE));
    return key;
  }

  /**
   * This method ensures that the caller is authorized to perform the action, and then deletes the
   * Comment.
   *
   * @param targetEntityKey key of target entity to delete comment from
   * @param commentKey key of Comment to delete
   */
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Override
  public void deleteComment(UUID targetEntityKey, int commentKey) {
    Comment commentToDelete = commentMapper.get(commentKey);
    checkArgument(commentToDelete != null, "Comment to delete doesn't exist");

    baseMapper.deleteComment(targetEntityKey, commentKey);
    eventManager.post(
        SubEntityCollectionEvent.newInstance(
            targetEntityKey, objectClass, commentToDelete, commentKey, EventType.DELETE));
  }

  @GetMapping(value = "{key}/comment")
  @Override
  public List<Comment> listComments(@PathVariable("key") UUID targetEntityKey) {
    return baseMapper.listComments(targetEntityKey);
  }

  protected void preCreate(T entity) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final String username = authentication.getName();
    entity.setCreatedBy(username);
    entity.setModifiedBy(username);
  }

  protected void preUpdate(T entity) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    entity.setModifiedBy(authentication.getName());
  }

  private void validateIdentifier(Identifier identifier) {
    if (identifier.getType() == IdentifierType.CITES
        && !IdentifierUtils.isValidCitesIdentifier(identifier.getIdentifier())) {
      throw new IllegalArgumentException("Invalid CITES identifier");
    }
  }
}
