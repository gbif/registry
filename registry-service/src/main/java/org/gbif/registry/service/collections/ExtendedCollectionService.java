package org.gbif.registry.service.collections;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.OccurrenceMappeable;
import org.gbif.api.model.registry.Commentable;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.Taggable;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.BaseMapper;

import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;

// TODO: implement services
public class ExtendedCollectionService<
        T extends
            CollectionEntity & Taggable & Identifiable & MachineTaggable & Contactable & Commentable
                & OccurrenceMappeable>
    extends BaseCollectionsService<T> {

  private final AddressMapper addressMapper;
  private final BaseMapper<T> baseMapper;
  private final MachineTagMapper machineTagMapper;
  private final TagMapper tagMapper;
  private final IdentifierMapper identifierMapper;

  protected ExtendedCollectionService(
      BaseMapper<T> baseMapper,
      AddressMapper addressMapper,
      MachineTagMapper machineTagMapper,
      TagMapper tagMapper,
      IdentifierMapper identifierMapper) {
    super(baseMapper);
    this.addressMapper = addressMapper;
    this.baseMapper = baseMapper;
    this.machineTagMapper = machineTagMapper;
    this.tagMapper = tagMapper;
    this.identifierMapper = identifierMapper;
  }

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

    return entity.getKey();
  }

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
}
