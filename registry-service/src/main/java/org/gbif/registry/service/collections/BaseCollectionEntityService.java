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

import org.gbif.api.model.collections.*;
import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.suggestions.ChangeSuggestion;
import org.gbif.api.model.registry.*;
import org.gbif.api.service.collections.CollectionEntityService;
import org.gbif.api.util.IdentifierUtils;
import org.gbif.api.util.validators.identifierschemes.IdentifierSchemeValidator;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.TagName;
import org.gbif.api.vocabulary.TagNamespace;
import org.gbif.api.vocabulary.collections.MasterSourceType;
import org.gbif.api.vocabulary.collections.Source;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.collections.*;
import org.gbif.registry.persistence.mapper.*;
import org.gbif.registry.persistence.mapper.collections.*;
import org.gbif.registry.persistence.mapper.dto.GrSciCollVocabConceptDto;
import org.gbif.registry.security.SecurityContextCheck;
import org.gbif.registry.service.WithMyBatis;
import org.gbif.registry.service.collections.utils.IdentifierValidatorUtils;
import org.gbif.registry.service.collections.utils.MasterSourceUtils;
import org.gbif.registry.service.collections.utils.Vocabularies;
import org.gbif.vocabulary.client.ConceptClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

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
import static org.gbif.registry.security.UserRoles.*;
import static org.gbif.registry.service.collections.utils.MasterSourceUtils.*;

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
  protected final ConceptClient conceptClient;
  protected final GrScicollVocabConceptMapper grScicollVocabConceptMapper;

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
      WithMyBatis withMyBatis,
      ConceptClient conceptClient,
      GrScicollVocabConceptMapper grScicollVocabConceptMapper) {
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
    this.conceptClient = conceptClient;
    this.grScicollVocabConceptMapper = grScicollVocabConceptMapper;
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
        withMyBatis.addCollectionIdentifier(identifierMapper, baseMapper, entityKey, identifier);
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
  @Transactional
  @Validated({PrePersist.class, Default.class})
  @Override
  public int updateIdentifier(UUID entityKey, int identifierKey, boolean isPrimary) {
      int key = withMyBatis.updateCollectionIdentifier(baseMapper, entityKey, identifierKey, isPrimary);
    eventManager.post(SubEntityCollectionEvent.newInstance(
      entityKey, objectClass, Identifier.class, (long) identifierKey, EventType.UPDATE));
    return key;
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
    if (identifier.getType() == IdentifierType.WIKIDATA
        && !IdentifierUtils.isValidWikidataIdentifier(identifier.getIdentifier())) {
      throw new IllegalArgumentException("Invalid Wikidata identifier");
    }
    if (identifier.getType() == IdentifierType.ROR
        && !IdentifierUtils.isValidRORIdentifier(identifier.getIdentifier())) {
      throw new IllegalArgumentException("Invalid ROR Identifier");
    }
    if (identifier.getType() == IdentifierType.ISIL
        && !IdentifierUtils.isValidISILIdentifier(identifier.getIdentifier())) {
      throw new IllegalArgumentException("Invalid ISIL Identifier");
    }
    if (identifier.getType() == IdentifierType.CLB_DATASET_KEY
        && !IdentifierUtils.isValidCLBDatasetKey(identifier.getIdentifier())) {
      throw new IllegalArgumentException("Invalid CLB_DATASET_KEY");
    }
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Transactional
  @Validated({PrePersist.class, Default.class})
  @Override
  public UUID create(T entity) {
    checkArgument(entity.getKey() == null, "Unable to create an entity which already has a key");
    preCreate(entity);

    // check vocabulary values are valid
    Vocabularies.checkVocabsValues(conceptClient, entity);

    if (entity.getAddress() != null) {
      addressMapper.create(entity.getAddress());
    }

    if (entity.getMailingAddress() != null) {
      addressMapper.create(entity.getMailingAddress());
    }

    entity.setMasterSource(MasterSourceType.GRSCICOLL);
    entity.setKey(UUID.randomUUID());
    baseMapper.create(entity);

    updateCollectionEntityConcepts(entity);

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

    // check vocabulary values are valid
    Vocabularies.checkVocabsValues(conceptClient, entity);

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

    // Update concept links synchronously within the same transaction
    updateCollectionEntityConcepts(newEntity);
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

  //Add contacts to entities created by ih-sync
  @Validated({PrePersist.class, Default.class})
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Transactional
  @Override
  public <R extends CollectionEntity> void addSuggestionContacts(@NotNull UUID createdEntity, @NotNull ChangeSuggestion<R> changeSuggestion) {
    if (changeSuggestion.getSuggestedEntity().getContactPersons() != null
        && !changeSuggestion.getSuggestedEntity().getContactPersons().isEmpty()) {

        changeSuggestion
          .getSuggestedEntity()
          .getContactPersons()
          .forEach(c -> {
            // Check if the proposedBy is "ih-sync"
            if (!IH_SYNC_USER.equals(changeSuggestion.getProposedBy())) {
              // If not "ih-sync", add the contact person
               addContactPerson(createdEntity, c);
            }
            else {
              addContactPersonToEntity(createdEntity,c);
            }
          });
      }
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

  /**
   * Updates concept links for Collection and Institution entities synchronously.
   * This replaces the async event-based approach for better transactional consistency.
   *
   * Uses dynamic vocabulary detection instead of hardcoded assumptions about which
   * vocabularies are hierarchical, making it adaptable to vocabulary structure changes.
   */
  private void updateCollectionEntityConcepts(T entity) {
    if (entity == null || entity.getKey() == null) {
      log.warn("Cannot update concept links for null entity or entity without key");
      return;
    }

    try {
      if (entity instanceof Collection) {
        updateConceptLinksForEntity((Collection) entity);
      } else if (entity instanceof Institution) {
        updateConceptLinksForEntity((Institution) entity);
      }
    } catch (Exception e) {
      log.error("Error updating concept links for entity {}: {}", entity.getKey(), e.getMessage(), e);
      throw new RuntimeException("Failed to update concept links", e);
    }
  }

  /**
   * Updates concept links for any Collection entity by dynamically checking
   * which vocabularies have concepts available in the system.
   */
  private void updateConceptLinksForEntity(Collection collection) {
    log.debug("Updating concept links for collection: {}", collection.getKey());

    // Delete existing concept links for this collection
    grScicollVocabConceptMapper.deleteCollectionConcepts(collection.getKey());

    // Dynamically handle all Collection vocabulary fields
    createConceptLinksFromVocabularyFields(
        collection.getKey(),
        "CollectionContentType", collection.getContentTypes(),
        "PreservationType", collection.getPreservationTypes(),
        "AccessionStatus", collection.getAccessionStatus() != null ?
            Collections.singletonList(collection.getAccessionStatus()) : Collections.emptyList()
    );

    log.debug("Successfully updated concept links for collection: {}", collection.getKey());
  }

  /**
   * Updates concept links for any Institution entity by dynamically checking
   * which vocabularies have concepts available in the system.
   */
  private void updateConceptLinksForEntity(Institution institution) {
    log.debug("Updating concept links for institution: {}", institution.getKey());

    // Delete existing concept links for this institution
    grScicollVocabConceptMapper.deleteInstitutionConcepts(institution.getKey());

    // Dynamically handle all Institution vocabulary fields
    createConceptLinksFromVocabularyFields(
        institution.getKey(),
        "Discipline", institution.getDisciplines(),
        "InstitutionType", institution.getTypes(),
        "InstitutionalGovernance", institution.getInstitutionalGovernances()
    );

    log.debug("Successfully updated concept links for institution: {}", institution.getKey());
  }

  /**
   * Creates concept links from vocabulary fields using a flexible approach.
   * Only creates links for vocabularies that actually have concepts in the system.
   * This makes the system adaptable to vocabulary structure changes.
   *
   * @param entityKey The entity key (collection or institution)
   * @param vocabularyFieldPairs Variable arguments of vocabulary name and field values
   */
  private void createConceptLinksFromVocabularyFields(UUID entityKey, Object... vocabularyFieldPairs) {
    if (vocabularyFieldPairs.length % 2 != 0) {
      throw new IllegalArgumentException("vocabularyFieldPairs must contain pairs of (vocabularyName, fieldValues)");
    }

    for (int i = 0; i < vocabularyFieldPairs.length; i += 2) {
      String vocabularyName = (String) vocabularyFieldPairs[i];
      @SuppressWarnings("unchecked")
      List<String> fieldValues = (List<String>) vocabularyFieldPairs[i + 1];

      if (fieldValues == null || fieldValues.isEmpty()) {
        log.debug("No {} values found for entity: {}", vocabularyName, entityKey);
        continue;
      }

      // Filter valid values
      List<String> validValues = fieldValues.stream()
          .filter(Objects::nonNull)
          .filter(value -> !value.trim().isEmpty())
          .collect(Collectors.toList());

      if (validValues.isEmpty()) {
        log.debug("No valid {} values found for entity: {}", vocabularyName, entityKey);
        continue;
      }

      // Try to create concept links - only if concepts exist for this vocabulary
      createConceptLinksForVocabulary(entityKey, vocabularyName, validValues);
    }
  }

  /**
   * Creates concept links for a specific vocabulary and values.
   * Only creates links if the vocabulary has concepts defined in the system.
   * This prevents assumptions about vocabulary structure.
   */
  private void createConceptLinksForVocabulary(UUID entityKey, String vocabularyName, List<String> values) {
    List<Long> conceptIds = new ArrayList<>();
    Set<Long> allConceptIds = new HashSet<>(); // Use Set to avoid duplicates
    int foundConcepts = 0;
    int missingConcepts = 0;

    for (String value : values) {
      try {
        Long conceptId = grScicollVocabConceptMapper.getConceptKeyByVocabularyAndName(vocabularyName, value);
        if (conceptId != null) {
          conceptIds.add(conceptId);
          allConceptIds.add(conceptId);
          foundConcepts++;

          // For hierarchical vocabularies, also add parent concept links
          addParentConceptLinks(entityKey, vocabularyName, conceptId, allConceptIds);
        } else {
          missingConcepts++;
          log.debug("No concept found for {}: {} (entity: {})", vocabularyName, value, entityKey);
        }
      } catch (Exception e) {
        log.error("Error looking up concept for {} = {} (entity: {}): {}",
                 vocabularyName, value, entityKey, e.getMessage());
        missingConcepts++;
      }
    }

    if (!allConceptIds.isEmpty()) {
      for (Long conceptId : allConceptIds) {
        try {
          // Use appropriate method based on entity type
          if (isCollectionEntity(entityKey)) {
            grScicollVocabConceptMapper.insertCollectionConcept(entityKey, conceptId);
          } else {
            grScicollVocabConceptMapper.insertInstitutionConcept(entityKey, conceptId);
          }
          log.debug("Created {} concept link for entity: {} with concept ID: {}",
                   vocabularyName, entityKey, conceptId);
        } catch (Exception e) {
          log.error("Error creating {} concept link for entity {} with concept ID {}: {}",
                   vocabularyName, entityKey, conceptId, e.getMessage());
        }
      }

      log.debug("Successfully created {} {} concept links for entity: {} ({} direct, {} total with parents, {} missing)",
               allConceptIds.size(), vocabularyName, entityKey, conceptIds.size(), allConceptIds.size(), missingConcepts);
    } else if (missingConcepts > 0) {
      log.debug("No concepts found for vocabulary {} - skipping concept link creation for entity: {} ({} values checked)",
               vocabularyName, entityKey, missingConcepts);
    }
  }

  /**
   * Recursively adds parent concept links for hierarchical vocabularies.
   * This ensures that filtering by parent concepts works correctly.
   */
  private void addParentConceptLinks(UUID entityKey, String vocabularyName, Long conceptId, Set<Long> allConceptIds) {
    try {
      GrSciCollVocabConceptDto concept = grScicollVocabConceptMapper.getByConceptKey(conceptId);
      if (concept != null && concept.getParentKey() != null) {
        // Check if parent concept exists and belongs to the same vocabulary
        GrSciCollVocabConceptDto parentConcept = grScicollVocabConceptMapper.getByConceptKey(concept.getParentKey());
        if (parentConcept != null && vocabularyName.equals(parentConcept.getVocabularyName())) {
          // Add parent concept to the set (Set prevents duplicates)
          allConceptIds.add(concept.getParentKey());
          log.debug("Added parent concept link for entity: {} with parent concept ID: {} (parent of: {})",
                   entityKey, concept.getParentKey(), conceptId);

          // Recursively add grandparent concepts
          addParentConceptLinks(entityKey, vocabularyName, concept.getParentKey(), allConceptIds);
        }
      }
    } catch (Exception e) {
      log.error("Error adding parent concept links for entity: {} concept: {}: {}",
               entityKey, conceptId, e.getMessage());
    }
  }

  /**
   * Determines if an entity key belongs to a Collection (vs Institution).
   * This is a simple heuristic - in a more complex system, you might want to
   * pass entity type explicitly or query the database.
   */
  private boolean isCollectionEntity(UUID entityKey) {
    // Since we're in BaseCollectionEntityService<T>, we can check the objectClass
    return objectClass.equals(Collection.class);
  }
}
