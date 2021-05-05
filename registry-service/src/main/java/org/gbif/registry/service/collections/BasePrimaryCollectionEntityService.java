package org.gbif.registry.service.collections;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.OccurrenceMappeable;
import org.gbif.api.model.collections.OccurrenceMapping;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.collections.PrimaryCollectionEntity;
import org.gbif.api.model.registry.Commentable;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.PostPersist;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.collections.ContactService;
import org.gbif.api.service.collections.OccurrenceMappingService;
import org.gbif.api.service.collections.PrimaryCollectionEntityService;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.collections.ChangedCollectionEntityComponentEvent;
import org.gbif.registry.events.collections.CreateCollectionEntityEvent;
import org.gbif.registry.events.collections.UpdateCollectionEntityEvent;
import org.gbif.registry.persistence.ContactableMapper;
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.BaseMapper;
import org.gbif.registry.persistence.mapper.collections.OccurrenceMappeableMapper;
import org.gbif.registry.persistence.mapper.collections.OccurrenceMappingMapper;
import org.gbif.registry.service.WithMyBatis;
import org.gbif.ws.WebApplicationException;

import java.util.List;
import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_EDITOR_ROLE;

@Validated
public abstract class BasePrimaryCollectionEntityService<
        T extends
            PrimaryCollectionEntity & Taggable & Identifiable & MachineTaggable & Contactable
                & Commentable & OccurrenceMappeable>
    extends BaseCollectionEntityService<T> implements PrimaryCollectionEntityService<T> {

  private final ContactableMapper contactableMapper;
  private final OccurrenceMappingMapper occurrenceMappingMapper;
  private final OccurrenceMappeableMapper occurrenceMappeableMapper;
  private final AddressMapper addressMapper;

  protected BasePrimaryCollectionEntityService(
      Class<T> objectClass,
      BaseMapper<T> baseMapper,
      AddressMapper addressMapper,
      MachineTagMapper machineTagMapper,
      TagMapper tagMapper,
      IdentifierMapper identifierMapper,
      ContactableMapper contactableMapper,
      CommentMapper commentMapper,
      OccurrenceMappingMapper occurrenceMappingMapper,
      OccurrenceMappeableMapper occurrenceMappeableMapper,
      EventManager eventManager,
      WithMyBatis withMyBatis) {
    super(
        baseMapper,
        tagMapper,
        machineTagMapper,
        identifierMapper,
        commentMapper,
        objectClass,
        eventManager,
        withMyBatis);
    this.addressMapper = addressMapper;
    this.contactableMapper = contactableMapper;
    this.occurrenceMappingMapper = occurrenceMappingMapper;
    this.occurrenceMappeableMapper = occurrenceMappeableMapper;
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
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

  @Validated({PostPersist.class, Default.class})
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Transactional
  @Override
  public void update(T entity) {
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

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Transactional
  @Override
  public void addContact(@NotNull UUID entityKey, @NotNull UUID personKey) {
    // check if the contact exists
    List<Person> contacts = contactableMapper.listContacts(entityKey);

    if (contacts != null && contacts.stream().anyMatch(p -> p.getKey().equals(personKey))) {
      throw new WebApplicationException("Duplicate contact", HttpStatus.CONFLICT);
    }

    contactableMapper.addContact(entityKey, personKey);
    eventManager.post(
        ChangedCollectionEntityComponentEvent.newInstance(entityKey, objectClass, Person.class));
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Transactional
  @Override
  public void removeContact(@NotNull UUID entityKey, @NotNull UUID personKey) {
    contactableMapper.removeContact(entityKey, personKey);
    eventManager.post(
        ChangedCollectionEntityComponentEvent.newInstance(entityKey, objectClass, Person.class));
  }

  @Override
  public List<Person> listContacts(@PathVariable UUID key) {
    return contactableMapper.listContacts(key);
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Transactional
  @Validated({PrePersist.class, Default.class})
  public int addOccurrenceMapping(UUID entityKey, OccurrenceMapping occurrenceMapping) {
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

  @Override
  public List<OccurrenceMapping> listOccurrenceMappings(@NotNull UUID uuid) {
    return occurrenceMappeableMapper.listOccurrenceMappings(uuid);
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Transactional
  @Override
  public void deleteOccurrenceMapping(UUID entityKey, int occurrenceMappingKey) {
    occurrenceMappeableMapper.deleteOccurrenceMapping(entityKey, occurrenceMappingKey);
    eventManager.post(
        ChangedCollectionEntityComponentEvent.newInstance(
            entityKey, objectClass, OccurrenceMapping.class));
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
}
