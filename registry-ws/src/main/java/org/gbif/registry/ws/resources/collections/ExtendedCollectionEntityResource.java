package org.gbif.registry.ws.resources.collections;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.registry.*;
import org.gbif.api.service.collections.ContactService;
import org.gbif.registry.events.ChangedComponentEvent;
import org.gbif.registry.events.collections.CreateCollectionEntityEvent;
import org.gbif.registry.events.collections.UpdateCollectionEntityEvent;
import org.gbif.registry.persistence.mapper.*;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.ContactableMapper;
import org.gbif.registry.persistence.mapper.collections.BaseMapper;
import org.gbif.registry.ws.security.EditorAuthorizationService;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.eventbus.EventBus;
import org.apache.bval.guice.Validate;
import org.mybatis.guice.transactional.Transactional;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.GRSCICOLL_ADMIN_ROLE;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Base class to implement the main methods of {@link CollectionEntity} that are also @link *
 * Taggable}, {@link Identifiable} and {@link Contactable}. * *
 *
 * <p>It inherits from {@link BaseCollectionEntityResource} to test the CRUD operations.
 */
public abstract class ExtendedCollectionEntityResource<
        T extends CollectionEntity & Taggable & Identifiable & MachineTaggable & Contactable>
    extends BaseCollectionEntityResource<T> implements ContactService {

  private final BaseMapper<T> baseMapper;
  private final AddressMapper addressMapper;
  private final ContactableMapper contactableMapper;
  private final TagMapper tagMapper;
  private final MachineTagMapper machineTagMapper;
  private final IdentifierMapper identifierMapper;
  private final EventBus eventBus;
  private final Class<T> objectClass;

  protected ExtendedCollectionEntityResource(
      BaseMapper<T> baseMapper,
      AddressMapper addressMapper,
      TagMapper tagMapper,
      IdentifierMapper identifierMapper,
      ContactableMapper contactableMapper,
      MachineTagMapper machineTagMapper,
      EventBus eventBus,
      Class<T> objectClass,
      EditorAuthorizationService userAuthService) {
    super(
        baseMapper,
        tagMapper,
        machineTagMapper,
        identifierMapper,
        userAuthService,
        eventBus,
        objectClass);
    this.baseMapper = baseMapper;
    this.addressMapper = addressMapper;
    this.contactableMapper = contactableMapper;
    this.tagMapper = tagMapper;
    this.machineTagMapper = machineTagMapper;
    this.identifierMapper = identifierMapper;
    this.eventBus = eventBus;
    this.objectClass = objectClass;
  }

  @Transactional
  @Validate(groups = {PrePersist.class, Default.class})
  @Override
  public UUID create(@Valid @NotNull T entity) {
    checkArgument(entity.getKey() == null, "Unable to create an entity which already has a key");

    checkUniqueness(entity);

    if (entity.getAddress() != null) {
      checkArgument(entity.getAddress().getKey() == null, "Unable to create an address which already has a key");
      addressMapper.create(entity.getAddress());
    }

    if (entity.getMailingAddress() != null) {
      checkArgument(entity.getMailingAddress().getKey() == null, "Unable to create an address which already has a key");
      addressMapper.create(entity.getMailingAddress());
    }

    entity.setKey(UUID.randomUUID());
    baseMapper.create(entity);

    if (!entity.getMachineTags().isEmpty()) {
      for (MachineTag machineTag : entity.getMachineTags()) {
        checkArgument(machineTag.getKey() == null, "Unable to create a machine tag which already has a key");
        machineTag.setCreatedBy(entity.getCreatedBy());
        machineTagMapper.createMachineTag(machineTag);
        baseMapper.addMachineTag(entity.getKey(), machineTag.getKey());
      }
    }

    if (!entity.getTags().isEmpty()) {
      for (Tag tag : entity.getTags()) {
        checkArgument(tag.getKey() == null, "Unable to create a tag which already has a key");
        tag.setCreatedBy(entity.getCreatedBy());
        tagMapper.createTag(tag);
        baseMapper.addTag(entity.getKey(), tag.getKey());
      }
    }

    if (!entity.getIdentifiers().isEmpty()) {
      for (Identifier identifier : entity.getIdentifiers()) {
        checkArgument(identifier.getKey() == null, "Unable to create an identifier which already has a key");
        identifier.setCreatedBy(entity.getCreatedBy());
        identifierMapper.createIdentifier(identifier);
        baseMapper.addIdentifier(entity.getKey(), identifier.getKey());
      }
    }

    eventBus.post(CreateCollectionEntityEvent.newInstance(entity, objectClass));
    return entity.getKey();
  }

  @Transactional
  @Validate
  @Override
  public void update(@Valid @NotNull T entity) {
    T entityOld = get(entity.getKey());
    checkArgument(entityOld != null, "Entity doesn't exist");

    if (entityOld.getDeleted() != null) {
      // if it's deleted we only allow to update it if we undelete it
      checkArgument(entity.getDeleted() == null,
                    "Unable to update a previously deleted entity unless you clear the deletion timestamp");
    } else {
      // not allowed to delete when updating
      checkArgument(entity.getDeleted() == null, "Can't delete an entity when updating");
    }

    checkUniquenessInUpdate(entityOld, entity);

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
    eventBus.post(UpdateCollectionEntityEvent.newInstance(newEntity, entityOld, objectClass));
  }

  private void updateAddress(Address newAddress, Address oldAddress) {
    if (newAddress != null) {
      if (oldAddress == null) {
        checkArgument(newAddress.getKey() == null, "Unable to create an address which already has a key");
        addressMapper.create(newAddress);
      } else {
        addressMapper.update(newAddress);
      }
    }
  }

  @POST
  @Path("{key}/contact")
  @Validate
  @Transactional
  @RolesAllowed({ADMIN_ROLE, GRSCICOLL_ADMIN_ROLE})
  @Consumes({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
  @Override
  public void addContact(@PathParam("key") @NotNull UUID entityKey, @NotNull UUID personKey) {
    // check if the contact exists
    List<Person> contacts = contactableMapper.listContacts(entityKey);

    if (contacts != null && contacts.stream().anyMatch(p -> p.getKey().equals(personKey))) {
      throw new WebApplicationException(Response.status(Response.Status.CONFLICT).entity("Duplicate contact").build());
    }

    contactableMapper.addContact(entityKey, personKey);
    eventBus.post(ChangedComponentEvent.newInstance(entityKey, objectClass, Person.class));
  }

  @DELETE
  @Path("{key}/contact/{personKey}")
  @Validate
  @Transactional
  @RolesAllowed({ADMIN_ROLE, GRSCICOLL_ADMIN_ROLE})
  @Override
  public void removeContact(
    @PathParam("key") @NotNull UUID entityKey, @PathParam("personKey") @NotNull UUID personKey
  ) {
    contactableMapper.removeContact(entityKey, personKey);
    eventBus.post(ChangedComponentEvent.newInstance(entityKey, objectClass, Person.class));
  }

  @GET
  @Path("{key}/contact")
  @Nullable
  @Validate(validateReturnedValue = true)
  @Override
  public List<Person> listContacts(@PathParam("key") @NotNull UUID key) {
    return contactableMapper.listContacts(key);
  }

}
