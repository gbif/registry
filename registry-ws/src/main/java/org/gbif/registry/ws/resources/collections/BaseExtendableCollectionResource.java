package org.gbif.registry.ws.resources.collections;

import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.collections.ContactService;
import org.gbif.api.service.registry.IdentifierService;
import org.gbif.api.service.registry.TagService;
import org.gbif.registry.events.ChangedComponentEvent;
import org.gbif.registry.events.CreateEvent;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.IdentifiableMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.TaggableMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.ContactableMapper;
import org.gbif.registry.persistence.mapper.collections.CrudMapper;
import org.gbif.registry.ws.guice.Trim;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.google.common.eventbus.EventBus;
import org.apache.bval.guice.Validate;
import org.mybatis.guice.transactional.Transactional;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.EDITOR_ROLE;

import static com.google.common.base.Preconditions.checkArgument;

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
  private final EventBus eventBus;
  private final Class<T> objectClass;

  protected BaseExtendableCollectionResource(CrudMapper<T> crudMapper, AddressMapper addressMapper,
                                             TaggableMapper taggableMapper, TagMapper tagMapper,
                                             IdentifiableMapper identifiableMapper, IdentifierMapper identifierMapper,
                                             ContactableMapper contactableMapper, EventBus eventBus, Class<T> objectClass) {
    super(crudMapper, eventBus, objectClass);
    this.crudMapper = crudMapper;
    this.addressMapper = addressMapper;
    this.taggableMapper = taggableMapper;
    this.tagMapper = tagMapper;
    this.identifiableMapper = identifiableMapper;
    this.identifierMapper = identifierMapper;
    this.contactableMapper = contactableMapper;
    this.eventBus = eventBus;
    this.objectClass = objectClass;
  }

  @Transactional
  @Validate(groups = {PrePersist.class, Default.class})
  @Override
  public UUID create(@Valid @NotNull T entity) {
    checkArgument(entity.getKey() == null, "Unable to create an entity which already has a key");

    if (entity.getAddress() != null) {
      checkArgument(
          entity.getAddress().getKey() == null,
          "Unable to create an address which already has a key");
      addressMapper.create(entity.getAddress());
    }

    if (entity.getMailingAddress() != null) {
      checkArgument(
          entity.getMailingAddress().getKey() == null,
          "Unable to create an address which already has a key");
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
        checkArgument(
            identifier.getKey() == null, "Unable to create an identifier which already has a key");
        identifier.setCreatedBy(entity.getCreatedBy());
        identifierMapper.createIdentifier(identifier);
        identifiableMapper.addIdentifier(entity.getKey(), identifier.getKey());
      }
    }

    eventBus.post(CreateEvent.newInstance(entity, objectClass));
    return entity.getKey();
  }

  @POST
  @Path("{key}/contact")
  @Validate
  @Transactional
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
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
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public void removeContact(
      @PathParam("key") @NotNull UUID entityKey, @PathParam("personKey") @NotNull UUID personKey) {
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

  @POST
  @Path("{key}/identifier")
  @Trim
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
  public int addIdentifier(@PathParam("key") @NotNull UUID entityKey, @NotNull Identifier identifier,
                           @Context SecurityContext security) {
    identifier.setCreatedBy(security.getUserPrincipal().getName());
    return addIdentifier(entityKey, identifier);
  }

  @Validate(groups = {PrePersist.class, Default.class})
  @Override
  public int addIdentifier(@NotNull UUID entityKey, @Valid @NotNull Identifier identifier) {
    int identifierKey = WithMyBatis.addIdentifier(identifierMapper, identifiableMapper, entityKey, identifier);
    eventBus.post(ChangedComponentEvent.newInstance(entityKey, objectClass, Identifier.class));
    return identifierKey;
  }

  @DELETE
  @Path("{key}/identifier/{identifierKey}")
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
  @Transactional
  @Override
  public void deleteIdentifier(@PathParam("key") @NotNull UUID entityKey, @PathParam("identifierKey") int identifierKey) {
    WithMyBatis.deleteIdentifier(identifiableMapper, entityKey, identifierKey);
    eventBus.post(ChangedComponentEvent.newInstance(entityKey, objectClass, Identifier.class));
  }

  @GET
  @Path("{key}/identifier")
  @Nullable
  @Validate(validateReturnedValue = true)
  @Override
  public List<Identifier> listIdentifiers(@PathParam("key") @NotNull UUID key) {
    return WithMyBatis.listIdentifiers(identifiableMapper, key);
  }

  @POST
  @Path("{key}/tag")
  @Trim
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
  public int addTag(@PathParam("key") @NotNull UUID entityKey, @NotNull Tag tag, @Context SecurityContext security) {
    tag.setCreatedBy(security.getUserPrincipal().getName());
    return addTag(entityKey, tag);
  }

  @Override
  public int addTag(@NotNull UUID key, @NotNull String value) {
    Tag tag = new Tag();
    tag.setValue(value);
    return addTag(key, tag);
  }

  @Validate(groups = {PrePersist.class, Default.class})
  @Override
  public int addTag(@NotNull UUID entityKey, @Valid @NotNull Tag tag) {
    int tagKey = WithMyBatis.addTag(tagMapper, taggableMapper, entityKey, tag);
    eventBus.post(ChangedComponentEvent.newInstance(entityKey, objectClass, Tag.class));
    return tagKey;
  }

  @DELETE
  @Path("{key}/tag/{tagKey}")
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
  @Transactional
  @Override
  public void deleteTag(@PathParam("key") @NotNull UUID entityKey, @PathParam("tagKey") int tagKey) {
    WithMyBatis.deleteTag(taggableMapper, entityKey, tagKey);
    eventBus.post(ChangedComponentEvent.newInstance(entityKey, objectClass, Tag.class));
  }

  @GET
  @Path("{key}/tag")
  @Nullable
  @Validate(validateReturnedValue = true)
  @Override
  public List<Tag> listTags(@PathParam("key") @NotNull UUID key, @QueryParam("owner") @Nullable String owner) {
    return WithMyBatis.listTags(taggableMapper, key, owner);
  }
}
