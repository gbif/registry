package org.gbif.registry.ws.resources.collections;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.collections.ContactService;
import org.gbif.api.service.registry.IdentifierService;
import org.gbif.api.service.registry.TagService;
import org.gbif.registry.events.ChangedComponentEvent;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.collections.CreateCollectionEntityEvent;
import org.gbif.registry.events.collections.UpdateCollectionEntityEvent;
import org.gbif.registry.persistence.ContactableMapper;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.IdentifiableMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.TaggableMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.CrudMapper;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.annotation.Trim;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.GRSCICOLL_ADMIN_ROLE;

/**
 * Base class to implement the main methods of {@link CollectionEntity} that are also @link *
 * Taggable}, {@link Identifiable} and {@link Contactable}. * *
 *
 * <p>It inherits from {@link BaseCrudResource} to test the CRUD operations.
 */
public abstract class BaseExtendableCollectionResource<T extends CollectionEntity & Taggable & Identifiable & Contactable>
    extends BaseCrudResource<T> implements TagService, IdentifierService, ContactService {

  private final CrudMapper<T> crudMapper;
  private final AddressMapper addressMapper;
  private final TaggableMapper taggableMapper;
  private final TagMapper tagMapper;
  private final IdentifiableMapper identifiableMapper;
  private final IdentifierMapper identifierMapper;
  private final ContactableMapper contactableMapper;
  private final EventManager eventManager;
  private final Class<T> objectClass;
  private final WithMyBatis withMyBatis;

  protected BaseExtendableCollectionResource(CrudMapper<T> crudMapper, AddressMapper addressMapper,
                                             TaggableMapper taggableMapper, TagMapper tagMapper,
                                             IdentifiableMapper identifiableMapper, IdentifierMapper identifierMapper,
                                             ContactableMapper contactableMapper, EventManager eventManager,
                                             Class<T> objectClass, WithMyBatis withMyBatis) {
    super(crudMapper, eventManager, objectClass);
    this.crudMapper = crudMapper;
    this.addressMapper = addressMapper;
    this.taggableMapper = taggableMapper;
    this.tagMapper = tagMapper;
    this.identifiableMapper = identifiableMapper;
    this.identifierMapper = identifierMapper;
    this.contactableMapper = contactableMapper;
    this.eventManager = eventManager;
    this.objectClass = objectClass;
    this.withMyBatis = withMyBatis;
  }

  @Transactional
//  @Validate(groups = {PrePersist.class, Default.class})
  @Override
  public UUID create(@Valid @NotNull T entity) {
    checkArgument(entity.getKey() == null, "Unable to create an entity which already has a key");

    if (entity.getAddress() != null) {
      checkArgument(entity.getAddress().getKey() == null, "Unable to create an address which already has a key");
      addressMapper.create(entity.getAddress());
    }

    if (entity.getMailingAddress() != null) {
      checkArgument(entity.getMailingAddress().getKey() == null, "Unable to create an address which already has a key");
      addressMapper.create(entity.getMailingAddress());
    }

    entity.setKey(UUID.randomUUID());
    crudMapper.create(entity);

    if (!entity.getTags().isEmpty()) {
      for (Tag tag : entity.getTags()) {
        checkArgument(tag.getKey() == null, "Unable to create a tag which already has a key");
        tag.setCreatedBy(entity.getCreatedBy());
        tagMapper.createTag(tag);
        taggableMapper.addTag(entity.getKey(), tag.getKey());
      }
    }

    if (!entity.getIdentifiers().isEmpty()) {
      for (Identifier identifier : entity.getIdentifiers()) {
        checkArgument(identifier.getKey() == null, "Unable to create an identifier which already has a key");
        identifier.setCreatedBy(entity.getCreatedBy());
        identifierMapper.createIdentifier(identifier);
        identifiableMapper.addIdentifier(entity.getKey(), identifier.getKey());
      }
    }

    eventManager.post(CreateCollectionEntityEvent.newInstance(entity, objectClass));
    return entity.getKey();
  }

  @Transactional
//  @Validate
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

    // update mailing address
    updateAddress(entity.getMailingAddress(), entityOld.getMailingAddress());

    // update address
    updateAddress(entity.getAddress(), entityOld.getAddress());

    // update entity
    crudMapper.update(entity);

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
        checkArgument(newAddress.getKey() == null, "Unable to create an address which already has a key");
        addressMapper.create(newAddress);
      } else {
        addressMapper.update(newAddress);
      }
    }
  }

  @PostMapping(value = "{key}/contact", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE})
//  @Validate
  @Transactional
  @Secured({ADMIN_ROLE, GRSCICOLL_ADMIN_ROLE})
  @Override
  public void addContact(@PathVariable("key") @NotNull UUID entityKey, @RequestBody @NotNull UUID personKey) {
    // check if the contact exists
    List<Person> contacts = contactableMapper.listContacts(entityKey);

    if (contacts != null && contacts.stream().anyMatch(p -> p.getKey().equals(personKey))) {
      throw new WebApplicationException(ResponseEntity.status(HttpStatus.CONFLICT).body("Duplicate contact"));
    }

    contactableMapper.addContact(entityKey, personKey);
    eventManager.post(ChangedComponentEvent.newInstance(entityKey, objectClass, Person.class));
  }

  @DeleteMapping("{key}/contact/{personKey}")
//  @Validate
  @Transactional
  @Secured({ADMIN_ROLE, GRSCICOLL_ADMIN_ROLE})
  @Override
  public void removeContact(@PathVariable("key") @NotNull UUID entityKey, @PathVariable @NotNull UUID personKey) {
    contactableMapper.removeContact(entityKey, personKey);
    eventManager.post(ChangedComponentEvent.newInstance(entityKey, objectClass, Person.class));
  }

  @GetMapping("{key}/contact")
  @Nullable
//  @Validate(validateReturnedValue = true)
  @Override
  public List<Person> listContacts(@PathVariable @NotNull UUID key) {
    return contactableMapper.listContacts(key);
  }

  @PostMapping("{key}/identifier")
  @Trim
  @Secured({ADMIN_ROLE, GRSCICOLL_ADMIN_ROLE})
  public int addIdentifierBase(@PathVariable("key") @NotNull UUID entityKey, @RequestBody @NotNull Identifier identifier) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final UserDetails principal = (UserDetails) authentication.getPrincipal();
    identifier.setCreatedBy(principal.getUsername());
    return addIdentifier(entityKey, identifier);
  }

//  @Validate(groups = {PrePersist.class, Default.class})
  @Override
  public int addIdentifier(@NotNull UUID entityKey, @Valid @NotNull Identifier identifier) {
    int identifierKey = withMyBatis.addIdentifier(identifierMapper, identifiableMapper, entityKey, identifier);
    eventManager.post(ChangedComponentEvent.newInstance(entityKey, objectClass, Identifier.class));
    return identifierKey;
  }

  @DeleteMapping("{key}/identifier/{identifierKey}")
  @Secured({ADMIN_ROLE, GRSCICOLL_ADMIN_ROLE})
  @Transactional
  @Override
  public void deleteIdentifier(@PathVariable("key") @NotNull UUID entityKey, @PathVariable int identifierKey) {
    withMyBatis.deleteIdentifier(identifiableMapper, entityKey, identifierKey);
    eventManager.post(ChangedComponentEvent.newInstance(entityKey, objectClass, Identifier.class));
  }

  @GetMapping("{key}/identifier")
  @Nullable
//  @Validate(validateReturnedValue = true)
  @Override
  public List<Identifier> listIdentifiers(@PathVariable @NotNull UUID key) {
    return withMyBatis.listIdentifiers(identifiableMapper, key);
  }

  @PostMapping("{key}/tag")
  @Trim
  @Secured({ADMIN_ROLE, GRSCICOLL_ADMIN_ROLE})
  public int addTagBase(@PathVariable("key") @NotNull UUID entityKey, @RequestBody @NotNull Tag tag) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final UserDetails principal = (UserDetails) authentication.getPrincipal();
    tag.setCreatedBy(principal.getUsername());
    return addTag(entityKey, tag);
  }

  @Override
  public int addTag(@NotNull UUID key, @NotNull String value) {
    Tag tag = new Tag();
    tag.setValue(value);
    return addTag(key, tag);
  }

//  @Validate(groups = {PrePersist.class, Default.class})
  @Override
  public int addTag(@NotNull UUID entityKey, @Valid @NotNull Tag tag) {
    int tagKey = withMyBatis.addTag(tagMapper, taggableMapper, entityKey, tag);
    eventManager.post(ChangedComponentEvent.newInstance(entityKey, objectClass, Tag.class));
    return tagKey;
  }

  @DeleteMapping("{key}/tag/{tagKey}")
  @Secured({ADMIN_ROLE, GRSCICOLL_ADMIN_ROLE})
  @Transactional
  @Override
  public void deleteTag(@PathVariable("key") @NotNull UUID entityKey, @PathVariable int tagKey) {
    withMyBatis.deleteTag(taggableMapper, entityKey, tagKey);
    eventManager.post(ChangedComponentEvent.newInstance(entityKey, objectClass, Tag.class));
  }

  @GetMapping("{key}/tag")
  @Nullable
//  @Validate(validateReturnedValue = true)
  @Override
  public List<Tag> listTags(@PathVariable("key") @NotNull UUID key, @RequestParam(value = "owner", required = false) @Nullable String owner) {
    return withMyBatis.listTags(taggableMapper, key, owner);
  }
}
