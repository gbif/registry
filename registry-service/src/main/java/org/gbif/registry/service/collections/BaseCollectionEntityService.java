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

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.MasterSourceMetadata;
import org.gbif.api.model.collections.OccurrenceMapping;
import org.gbif.api.model.collections.UserId;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Commentable;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.PostPersist;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.collections.CollectionEntityService;
import org.gbif.api.util.IdentifierUtils;
import org.gbif.api.util.validators.identifierschemes.IdentifierSchemeValidator;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.TagName;
import org.gbif.api.vocabulary.TagNamespace;
import org.gbif.api.vocabulary.collections.MasterSourceType;
import org.gbif.api.vocabulary.collections.Source;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.collections.CreateCollectionEntityEvent;
import org.gbif.registry.events.collections.DeleteCollectionEntityEvent;
import org.gbif.registry.events.collections.EventType;
import org.gbif.registry.events.collections.MasterSourceMetadataAddedEvent;
import org.gbif.registry.events.collections.ReplaceEntityEvent;
import org.gbif.registry.events.collections.SubEntityCollectionEvent;
import org.gbif.registry.events.collections.UpdateCollectionEntityEvent;
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.NetworkEntityMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.BaseMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionContactMapper;
import org.gbif.registry.persistence.mapper.collections.MasterSourceSyncMetadataMapper;
import org.gbif.registry.persistence.mapper.collections.OccurrenceMappingMapper;
import org.gbif.registry.persistence.mapper.collections.params.RangeParam;
import org.gbif.registry.security.SecurityContextCheck;
import org.gbif.registry.service.WithMyBatis;
import org.gbif.registry.service.collections.utils.IdentifierValidatorUtils;
import org.gbif.registry.service.collections.utils.MasterSourceUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.elasticsearch.common.Strings;
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

import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_EDITOR_ROLE;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_MEDIATOR_ROLE;
import static org.gbif.registry.service.collections.utils.MasterSourceUtils.COLLECTION_LOCKABLE_FIELDS;
import static org.gbif.registry.service.collections.utils.MasterSourceUtils.CONTACTS_FIELD_NAME;
import static org.gbif.registry.service.collections.utils.MasterSourceUtils.INSTITUTION_LOCKABLE_FIELDS;
import static org.gbif.registry.service.collections.utils.MasterSourceUtils.hasExternalMasterSource;
import static org.gbif.registry.service.collections.utils.MasterSourceUtils.isLockableEntity;
import static org.gbif.registry.service.collections.utils.MasterSourceUtils.isSourceableField;
import static org.gbif.registry.service.collections.utils.SearchUtils.NUMBER_SPECIMENS_RANGE;
import static org.gbif.registry.service.collections.utils.SearchUtils.WILDCARD_SEARCH;

@Validated
@Slf4j
public class BaseCollectionEntityService<
        T extends CollectionEntity & Taggable & Identifiable & MachineTaggable & Commentable>
    implements CollectionEntityService<T> {

  private static final Logger LOG = LoggerFactory.getLogger(BaseCollectionEntityService.class);

  protected final Class<T> objectClass;
  protected final BaseMapper<T> baseMapper;
  protected final AddressMapper addressMapper;
  protected final CollectionContactMapper contactMapper;
  protected final TagMapper tagMapper;
  protected final MachineTagMapper machineTagMapper;
  protected final IdentifierMapper identifierMapper;
  protected final OccurrenceMappingMapper occurrenceMappingMapper;
  protected final MasterSourceSyncMetadataMapper masterSourceSyncMetadataMapper;
  protected final DatasetMapper datasetMapper;
  protected final OrganizationMapper organizationMapper;
  protected final CommentMapper commentMapper;
  protected final EventManager eventManager;
  protected final WithMyBatis withMyBatis;

  protected BaseCollectionEntityService(
      BaseMapper<T> baseMapper,
      AddressMapper addressMapper,
      CollectionContactMapper contactMapper,
      TagMapper tagMapper,
      MachineTagMapper machineTagMapper,
      IdentifierMapper identifierMapper,
      OccurrenceMappingMapper occurrenceMappingMapper,
      MasterSourceSyncMetadataMapper masterSourceSyncMetadataMapper,
      DatasetMapper datasetMapper,
      OrganizationMapper organizationMapper,
      CommentMapper commentMapper,
      Class<T> objectClass,
      EventManager eventManager,
      WithMyBatis withMyBatis) {
    this.baseMapper = baseMapper;
    this.addressMapper = addressMapper;
    this.contactMapper = contactMapper;
    this.tagMapper = tagMapper;
    this.machineTagMapper = machineTagMapper;
    this.identifierMapper = identifierMapper;
    this.occurrenceMappingMapper = occurrenceMappingMapper;
    this.masterSourceSyncMetadataMapper = masterSourceSyncMetadataMapper;
    this.datasetMapper = datasetMapper;
    this.organizationMapper = organizationMapper;
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
    if (hasExternalMasterSource(entityToDelete)) {
      throw new IllegalArgumentException(
          "Cannot delete an entity whose master source is not GRSciColl");
    }

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
    if (entity.getDisplayOnNHCPortal() == null) {
      entity.setDisplayOnNHCPortal(true);
    }
  }

  protected void preUpdate(T entity) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    entity.setModifiedBy(authentication.getName());
    if (entity.getDisplayOnNHCPortal() == null) {
      entity.setDisplayOnNHCPortal(true);
    }
  }

  private void validateIdentifier(Identifier identifier) {
    if (identifier.getType() == IdentifierType.CITES
        && !IdentifierUtils.isValidCitesIdentifier(identifier.getIdentifier())) {
      throw new IllegalArgumentException("Invalid CITES identifier");
    }
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Transactional
  @Validated({PrePersist.class, Default.class})
  @Override
  public UUID create(T entity) {
    checkArgument(entity.getKey() == null, "Unable to create an entity which already has a key");
    preCreate(entity);

    if (entity.getAddress() != null) {
      addressMapper.create(entity.getAddress());
    }

    if (entity.getMailingAddress() != null) {
      addressMapper.create(entity.getMailingAddress());
    }

    entity.setMasterSource(MasterSourceType.GRSCICOLL);
    entity.setKey(UUID.randomUUID());
    baseMapper.create(entity);

    eventManager.post(CreateCollectionEntityEvent.newInstance(entity));

    return entity.getKey();
  }

  @Validated({PostPersist.class, Default.class})
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Transactional
  @Override
  public void update(T entity) {
    update(entity, true);
  }

  @Override
  public boolean exists(@NotNull UUID key) {
    return baseMapper.exists(key);
  }

  @Validated({PostPersist.class, Default.class})
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Transactional
  @Override
  public void update(@NotNull @Valid T entity, boolean lockFields) {
    preUpdate(entity);
    T entityOld = get(entity.getKey());
    checkArgument(entityOld != null, "Entity doesn't exist");
    checkCodeUpdate(entity, entityOld);
    checkReplacedEntitiesUpdate(entity, entityOld);

    if (entityOld.getDeleted() != null) {
      // if it's deleted we only allow to update it if we undelete it
      checkArgument(
          entity.getDeleted() == null,
          "Unable to update a previously deleted entity unless you clear the deletion timestamp");
    } else {
      // not allowed to delete when updating
      checkArgument(entity.getDeleted() == null, "Can't delete an entity when updating");
    }

    // lock fields
    if (lockFields && isLockableEntity(entityOld)) {
      lockFields(entityOld, entity);
    }

    // update mailing address
    updateAddress(entity.getMailingAddress(), entityOld.getMailingAddress());

    // update address
    updateAddress(entity.getAddress(), entityOld.getAddress());

    // update entity
    baseMapper.update(entity);

    // check if we can delete the mailing address
    if (entity.getMailingAddress() == null && entityOld.getMailingAddress() != null) {
      addressMapper.delete(entityOld.getMailingAddress().getKey());
    }

    // check if we can delete the address
    if (entity.getAddress() == null && entityOld.getAddress() != null) {
      addressMapper.delete(entityOld.getAddress().getKey());
    }

    T newEntity = get(entity.getKey());

    eventManager.post(UpdateCollectionEntityEvent.newInstance(newEntity, entityOld));
  }

  public T lockFields(T entityOld, T entityNew) {
    List<MasterSourceUtils.LockableField> fieldsToLock = new ArrayList<>();
    if (entityOld instanceof Institution) {
      fieldsToLock = INSTITUTION_LOCKABLE_FIELDS.get(entityOld.getMasterSource());
    } else if (entityOld instanceof Collection) {
      fieldsToLock = COLLECTION_LOCKABLE_FIELDS.get(entityOld.getMasterSource());
    }

    fieldsToLock.forEach(
        f -> {
          try {
            f.getSetter().invoke(entityNew, f.getGetter().invoke(entityOld));
          } catch (Exception e) {
            throw new IllegalStateException("Could not lock field", e);
          }
        });

    return entityNew;
  }

  private void updateAddress(Address newAddress, Address oldAddress) {
    if (newAddress != null) {
      if (oldAddress == null) {
        checkArgument(
            newAddress.getKey() == null, "Unable to create an address which already has a key");
        addressMapper.create(newAddress);
      } else {
        addressMapper.update(newAddress);
      }
    }
  }

  @Override
  public List<Contact> listContactPersons(@NotNull UUID entityKey) {
    return baseMapper.listContactPersons(entityKey);
  }

  @Validated({PrePersist.class, Default.class})
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Transactional
  @Override
  public int addContactPerson(@NotNull UUID entityKey, @NotNull @Valid Contact contact) {
    checkArgument(contact.getKey() == null, "Cannot create a contact that already has a key");

    T entity = get(entityKey);
    if (isLockableEntity(entity) && isSourceableField(objectClass, CONTACTS_FIELD_NAME)) {
      throw new IllegalArgumentException(
          "Cannot add contacts to an entity whose master source is not GRSciColl");
    }

    return addContactPersonToEntity(entityKey, contact);
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Transactional
  private int addContactPersonToEntity(@NotNull UUID entityKey, @NotNull Contact contact) {
    checkArgument(contact.getKey() == null, "Cannot create a contact that already has a key");

    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final String username = authentication.getName();
    contact.setCreatedBy(username);
    contact.setModifiedBy(username);

    validateUserIds(contact);

    contactMapper.createContact(contact);
    baseMapper.addContactPerson(entityKey, contact.getKey());
    eventManager.post(
        SubEntityCollectionEvent.newInstance(
            entityKey, objectClass, contact, contact.getKey(), EventType.CREATE));

    return contact.getKey();
  }

  @Validated({PostPersist.class, Default.class})
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Transactional
  @Override
  public void updateContactPerson(@NotNull UUID entityKey, @NotNull @Valid Contact contact) {
    checkArgument(contact.getKey() != null, "Unable to update a contact with no key");

    T entity = get(entityKey);
    if (isLockableEntity(entity) && isSourceableField(objectClass, CONTACTS_FIELD_NAME)) {
      throw new IllegalArgumentException(
          "Cannot update contacts from an entity whose master source is not GRSciColl");
    }

    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final String username = authentication.getName();
    contact.setModifiedBy(username);

    validateUserIds(contact);

    contactMapper.updateContact(contact);

    eventManager.post(
        SubEntityCollectionEvent.newInstance(
            entityKey, objectClass, contact, contact.getKey(), EventType.UPDATE));
  }

  private void validateUserIds(Contact contact) {
    // validate userIds
    if (contact.getUserIds() != null && !contact.getUserIds().isEmpty()) {
      for (UserId userId : contact.getUserIds()) {
        if (userId.getType() == null) {
          throw new IllegalArgumentException("UserId type cannot be null");
        }

        IdentifierSchemeValidator validator =
            IdentifierValidatorUtils.getValidatorByIdType(userId.getType());

        if (validator != null && !validator.isValid(userId.getId())) {
          throw new IllegalArgumentException(
              "Invalid user ID with type " + userId.getType() + " and ID " + userId.getId());
        }
      }
    }
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Transactional
  @Override
  public void removeContactPerson(@NotNull UUID entityKey, @NotNull int contactKey) {
    Contact contactToRemove = contactMapper.getContact(contactKey);
    checkArgument(contactToRemove != null, "Contact to delete doesn't exist");

    T entity = get(entityKey);
    if (isLockableEntity(entity) && isSourceableField(objectClass, CONTACTS_FIELD_NAME)) {
      throw new IllegalArgumentException(
          "Cannot remove contacts from an entity whose master source is not GRSciColl");
    }

    baseMapper.removeContactPerson(entityKey, contactKey);
    eventManager.post(
        SubEntityCollectionEvent.newInstance(
            entityKey, objectClass, contactToRemove, contactKey, EventType.DELETE));
  }

  @Validated({PrePersist.class, Default.class})
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Transactional
  @Override
  public void replaceContactPersons(
      @NotNull UUID entityKey, List<@Valid Contact> newContactPersons) {
    checkArgument(entityKey != null);

    List<Contact> contacts = baseMapper.listContactPersons(entityKey);
    baseMapper.removeAllContactPersons(entityKey);

    contacts.forEach(
        c ->
            eventManager.post(
                SubEntityCollectionEvent.newInstance(
                    entityKey, objectClass, c, c.getKey(), EventType.DELETE)));

    if (newContactPersons != null) {
      newContactPersons.forEach(c -> addContactPersonToEntity(entityKey, c));
    }
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Transactional
  @Validated({PrePersist.class, Default.class})
  @Override
  public int addOccurrenceMapping(UUID entityKey, OccurrenceMapping occurrenceMapping) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    occurrenceMapping.setCreatedBy(authentication.getName());
    checkArgument(
        occurrenceMapping.getKey() == null, "Unable to create an entity which already has a key");
    occurrenceMappingMapper.createOccurrenceMapping(occurrenceMapping);
    baseMapper.addOccurrenceMapping(entityKey, occurrenceMapping.getKey());

    eventManager.post(
        SubEntityCollectionEvent.newInstance(
            entityKey,
            objectClass,
            occurrenceMapping,
            occurrenceMapping.getKey(),
            EventType.CREATE));

    return occurrenceMapping.getKey();
  }

  @Override
  public List<OccurrenceMapping> listOccurrenceMappings(@NotNull UUID uuid) {
    return baseMapper.listOccurrenceMappings(uuid);
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Transactional
  @Override
  public void deleteOccurrenceMapping(UUID entityKey, int occurrenceMappingKey) {
    OccurrenceMapping occurrenceMappingToDelete = occurrenceMappingMapper.get(occurrenceMappingKey);
    checkArgument(occurrenceMappingToDelete != null, "Occurrence Mapping to delete doesn't exist");

    baseMapper.deleteOccurrenceMapping(entityKey, occurrenceMappingKey);
    eventManager.post(
        SubEntityCollectionEvent.newInstance(
            entityKey,
            objectClass,
            occurrenceMappingToDelete,
            occurrenceMappingKey,
            EventType.DELETE));
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Transactional
  @Override
  public void replace(UUID targetEntityKey, UUID replacementKey) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    baseMapper.replace(targetEntityKey, replacementKey, authentication.getName());
    eventManager.post(
        ReplaceEntityEvent.newInstance(
            objectClass, targetEntityKey, replacementKey, EventType.REPLACE));
  }

  @Validated({PrePersist.class, Default.class})
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Transactional
  @Override
  public int addMasterSourceMetadata(
      UUID targetEntityKey, MasterSourceMetadata masterSourceMetadata) {
    checkArgument(targetEntityKey != null);
    checkArgument(masterSourceMetadata != null);
    checkArgument(
        masterSourceMetadata.getKey() == null, "Cannot create a metadata that already has a key");

    T entity = baseMapper.get(targetEntityKey);
    checkArgument(entity != null, "The entity doesn't exist");

    if (entity.getMasterSourceMetadata() != null) {
      throw new IllegalArgumentException(
          "The entity already has a master source. You have to delete the existing one before adding a new one");
    }

    // check preconditions for datasets
    if (masterSourceMetadata.getSource() == Source.DATASET) {
      if (!(entity instanceof Collection)) {
        throw new IllegalArgumentException("Dataset sources can be set only to collections");
      }
      checkExistsNetworkEntity(datasetMapper, masterSourceMetadata.getSourceId(), Dataset.class);
    }

    // check preconditions for organizations
    if (masterSourceMetadata.getSource() == Source.ORGANIZATION) {
      if (!(entity instanceof Institution)) {
        throw new IllegalArgumentException("Organization sources can be set only to institutions");
      }
      checkExistsNetworkEntity(
          organizationMapper, masterSourceMetadata.getSourceId(), Organization.class);
    }

    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    masterSourceMetadata.setCreatedBy(authentication.getName());

    masterSourceSyncMetadataMapper.create(masterSourceMetadata);
    MasterSourceType masterSourceType =
        masterSourceMetadata.getSource() == Source.IH_IRN
            ? MasterSourceType.IH
            : MasterSourceType.GBIF_REGISTRY;

    baseMapper.addMasterSourceMetadata(
        targetEntityKey, masterSourceMetadata.getKey(), masterSourceType);

    // event to sync the entity
    eventManager.post(
        MasterSourceMetadataAddedEvent.newInstance(targetEntityKey, masterSourceMetadata));

    return masterSourceMetadata.getKey();
  }

  @Override
  public void deleteMasterSourceMetadata(UUID targetEntityKey) {
    checkArgument(targetEntityKey != null);

    MasterSourceMetadata metadata = baseMapper.getEntityMasterSourceMetadata(targetEntityKey);

    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!SecurityContextCheck.checkUserInRole(authentication, GRSCICOLL_ADMIN_ROLE)
        && metadata.getSource() == Source.IH_IRN) {
      // non-admins cannot remove a IH metadata if this entity is the only one connected to IH - if
      // it were deleted the next sync would create a new entity for this IH IRN
      int activeEntities =
          masterSourceSyncMetadataMapper.countActiveEntitiesForMasterSource(
              Source.IH_IRN, metadata.getSourceId());

      if (activeEntities <= 1) {
        throw new IllegalArgumentException(
            "This is the only entity connected to IH for the IRN "
                + metadata.getSourceId()
                + ". Only GRSciColl admins can perform this operation");
      }
    }

    baseMapper.removeMasterSourceMetadata(targetEntityKey);
  }

  @Override
  public MasterSourceMetadata getMasterSourceMetadata(@NotNull UUID targetEntityKey) {
    return baseMapper.getEntityMasterSourceMetadata(targetEntityKey);
  }

  @Override
  public List<T> findByMasterSource(Source source, String sourceId) {
    return baseMapper.findByMasterSource(source, sourceId);
  }

  /**
   * Some iDigBio collections and institutions don't have code and we allow that in the DB but not
   * in the API.
   */
  protected void checkCodeUpdate(T newEntity, T oldEntity) {
    if (newEntity.getCode() == null && oldEntity.getCode() != null) {
      throw new IllegalArgumentException("Not allowed to delete the code of a primary entity");
    }
  }

  /**
   * Replaced and converted entities cannot be updated or restored. Also, they can't be replaced or
   * converted in an update
   */
  protected void checkReplacedEntitiesUpdate(T newEntity, T oldEntity) {
    if (oldEntity.getReplacedBy() != null) {
      throw new IllegalArgumentException("Not allowed to update a replaced entity");
    } else if (newEntity.getReplacedBy() != null) {
      throw new IllegalArgumentException("Not allowed to replace an entity while updating");
    }

    if (newEntity instanceof Institution) {
      Institution newInstitution = (Institution) newEntity;
      Institution oldInstitution = (Institution) oldEntity;

      if (oldInstitution.getConvertedToCollection() != null) {
        throw new IllegalArgumentException(
            "Not allowed to update a replaced or converted institution");
      } else if (newInstitution.getConvertedToCollection() != null) {
        throw new IllegalArgumentException(
            "Not allowed to replace or convert an institution while updating");
      }
    }
  }

  private <N extends NetworkEntity> void checkExistsNetworkEntity(
      NetworkEntityMapper<N> mapper, String key, Class<N> entityClass) {
    N networkEntity = mapper.get(UUID.fromString(key));
    if (networkEntity == null) {
      throw new IllegalArgumentException(
          entityClass.getSimpleName() + " not found with key " + key);
    }
    if (networkEntity.getDeleted() != null) {
      throw new IllegalArgumentException(
          "Cannot set a deleted " + entityClass.getSimpleName() + " as master source");
    }
  }

  protected RangeParam parseNumberSpecimensParameter(String numberSpecimens) {
    if (Strings.isNullOrEmpty(numberSpecimens)) {
      return null;
    }

    RangeParam rangeParam = new RangeParam();
    Matcher matcher = NUMBER_SPECIMENS_RANGE.matcher(numberSpecimens);
    if (matcher.matches()) {
      String lowerString = matcher.group(1);
      if (!lowerString.equals(WILDCARD_SEARCH)) {
        rangeParam.setLowerBound(Integer.valueOf(lowerString));
      }

      String higherString = matcher.group(2);
      if (!higherString.equals(WILDCARD_SEARCH)) {
        rangeParam.setHigherBound(Integer.valueOf(higherString));
      }
    } else {
      try {
        rangeParam.setExactValue(Integer.valueOf(numberSpecimens));
      } catch (Exception ex) {
        log.info("Invalid numberSpecimens range {}", numberSpecimens, ex);
      }
    }

    return rangeParam;
  }
}
