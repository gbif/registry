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
import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.OccurrenceMappeable;
import org.gbif.api.model.collections.OccurrenceMapping;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.registry.Commentable;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.collections.ContactService;
import org.gbif.api.service.collections.OccurrenceMappingService;
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
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.BaseMapper;
import org.gbif.registry.persistence.mapper.collections.OccurrenceMappeableMapper;
import org.gbif.registry.persistence.mapper.collections.OccurrenceMappingMapper;
import org.gbif.registry.security.EditorAuthorizationService;
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

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_EDITOR_ROLE;

/**
 * Base class to implement the main methods of {@link CollectionEntity} that are also @link *
 * Taggable}, {@link Identifiable} and {@link Contactable}. * *
 *
 * <p>It inherits from {@link BaseCollectionEntityResource} to test the CRUD operations.
 */
@Validated
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public abstract class ExtendedCollectionEntityResource<
        T extends
            CollectionEntity & Taggable & Identifiable & MachineTaggable & Contactable & Commentable
                & OccurrenceMappeable>
    extends BaseCollectionEntityResource<T> implements ContactService, OccurrenceMappingService {

  private final BaseMapper<T> baseMapper;
  private final AddressMapper addressMapper;
  private final ContactableMapper contactableMapper;
  private final TagMapper tagMapper;
  private final MachineTagMapper machineTagMapper;
  private final IdentifierMapper identifierMapper;
  private final OccurrenceMappingMapper occurrenceMappingMapper;
  private final OccurrenceMappeableMapper occurrenceMappeableMapper;
  private final MergeService mergeService;
  private final EventManager eventManager;
  private final Class<T> objectClass;

  protected ExtendedCollectionEntityResource(
      BaseMapper<T> baseMapper,
      AddressMapper addressMapper,
      TagMapper tagMapper,
      IdentifierMapper identifierMapper,
      ContactableMapper contactableMapper,
      MachineTagMapper machineTagMapper,
      CommentMapper commentMapper,
      OccurrenceMappingMapper occurrenceMappingMapper,
      OccurrenceMappeableMapper occurrenceMappeableMapper,
      MergeService mergeService,
      EventManager eventManager,
      Class<T> objectClass,
      EditorAuthorizationService userAuthService,
      WithMyBatis withMyBatis) {
    super(
        baseMapper,
        tagMapper,
        machineTagMapper,
        identifierMapper,
        commentMapper,
        userAuthService,
        eventManager,
        objectClass,
        withMyBatis);
    this.baseMapper = baseMapper;
    this.addressMapper = addressMapper;
    this.contactableMapper = contactableMapper;
    this.tagMapper = tagMapper;
    this.machineTagMapper = machineTagMapper;
    this.identifierMapper = identifierMapper;
    this.occurrenceMappingMapper = occurrenceMappingMapper;
    this.occurrenceMappeableMapper = occurrenceMappeableMapper;
    this.mergeService = mergeService;
    this.eventManager = eventManager;
    this.objectClass = objectClass;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Trim
  @Transactional
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Override
  public UUID create(@RequestBody T entity) {
    checkArgument(entity.getKey() == null, "Unable to create an entity which already has a key");
    preCreate(entity);

    if (entity.getAddress() != null) {
      addressMapper.create(entity.getAddress());
    }

    if (entity.getMailingAddress() != null) {
      addressMapper.create(entity.getMailingAddress());
    }

    entity.setKey(UUID.randomUUID());
    baseMapper.create(entity);

    if (!entity.getMachineTags().isEmpty()) {
      for (MachineTag machineTag : entity.getMachineTags()) {
        machineTag.setCreatedBy(entity.getCreatedBy());
        machineTagMapper.createMachineTag(machineTag);
        baseMapper.addMachineTag(entity.getKey(), machineTag.getKey());
      }
    }

    if (!entity.getTags().isEmpty()) {
      for (Tag tag : entity.getTags()) {
        tag.setCreatedBy(entity.getCreatedBy());
        tagMapper.createTag(tag);
        baseMapper.addTag(entity.getKey(), tag.getKey());
      }
    }

    if (!entity.getIdentifiers().isEmpty()) {
      for (Identifier identifier : entity.getIdentifiers()) {
        identifier.setCreatedBy(entity.getCreatedBy());
        identifierMapper.createIdentifier(identifier);
        baseMapper.addIdentifier(entity.getKey(), identifier.getKey());
      }
    }

    eventManager.post(CreateCollectionEntityEvent.newInstance(entity, objectClass));
    return entity.getKey();
  }

  @PutMapping(
      value = {"", "{key}"},
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Transactional
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Override
  public void update(@RequestBody @Trim T entity) {
    preUpdate(entity);
    T entityOld = get(entity.getKey());
    checkArgument(entityOld != null, "Entity doesn't exist");
    checkCodeUpdate(entity, entityOld);

    if (entityOld.getDeleted() != null) {
      // if it's deleted we only allow to update it if we undelete it
      checkArgument(
          entity.getDeleted() == null,
          "Unable to update a previously deleted entity unless you clear the deletion timestamp");
    } else {
      // not allowed to delete when updating
      checkArgument(entity.getDeleted() == null, "Can't delete an entity when updating");
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
    eventManager.post(UpdateCollectionEntityEvent.newInstance(newEntity, entityOld, objectClass));
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
      @PathVariable("key") UUID entityKey, @RequestBody OccurrenceMapping occurrenceMapping) {
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
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  public void merge(@PathVariable("key") UUID entityKey, @RequestBody MergeParams params) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    mergeService.merge(entityKey, params.replacementEntityKey, authentication.getName());
  }

  private static class MergeParams {
    private UUID replacementEntityKey;

    public UUID getReplacementEntityKey() {
      return replacementEntityKey;
    }

    public void setReplacementEntityKey(UUID replacementEntityKey) {
      this.replacementEntityKey = replacementEntityKey;
    }
  }

  /**
   * Some iDigBio collections and institutions don't have code and we allow that in the DB but not
   * in the API.
   */
  protected void checkCodeUpdate(T newEntity, T oldEntity) {
    if (newEntity instanceof Institution) {
      Institution newInstitution = (Institution) newEntity;
      Institution oldInstitution = (Institution) oldEntity;

      if (newInstitution.getCode() == null && oldInstitution.getCode() != null) {
        throw new IllegalArgumentException("Not allowed to delete the code of an institution");
      }
    } else if (newEntity instanceof Collection) {
      Collection newCollection = (Collection) newEntity;
      Collection oldCollection = (Collection) oldEntity;

      if (newCollection.getCode() == null && oldCollection.getCode() != null) {
        throw new IllegalArgumentException("Not allowed to delete the code of a collection");
      }
    }
  }
}
