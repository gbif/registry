package org.gbif.registry.ws.resources.collections;

import org.gbif.api.model.collections.Person;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.model.registry.search.collections.PersonSuggestResult;
import org.gbif.api.service.collections.PersonService;
import org.gbif.api.service.registry.IdentifierService;
import org.gbif.registry.events.ChangedComponentEvent;
import org.gbif.registry.events.collections.CreateCollectionEntityEvent;
import org.gbif.registry.events.collections.UpdateCollectionEntityEvent;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.PersonMapper;
import org.gbif.registry.ws.guice.Trim;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.bval.guice.Validate;
import org.mybatis.guice.transactional.Transactional;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.GRSCICOLL_ADMIN_ROLE;
import static org.gbif.registry.ws.util.GrscicollUtils.GRSCICOLL_PATH;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Class that acts both as the WS endpoint for {@link Person} entities and also provides an *
 * implementation of {@link PersonService}.
 */
@Singleton
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(GRSCICOLL_PATH + "/person")
public class PersonResource extends BaseCrudResource<Person> implements PersonService, IdentifierService {

  private final PersonMapper personMapper;
  private final AddressMapper addressMapper;
  private final IdentifierMapper identifierMapper;
  private final EventBus eventBus;

  @Inject
  public PersonResource(
      PersonMapper personMapper,
      AddressMapper addressMapper,
      IdentifierMapper identifierMapper,
      EventBus eventBus) {
    super(personMapper, eventBus, Person.class);
    this.personMapper = personMapper;
    this.addressMapper = addressMapper;
    this.identifierMapper = identifierMapper;
    this.eventBus = eventBus;
  }

  @GET
  public PagingResponse<Person> list(@Nullable @QueryParam("q") String query,
                                     @Nullable @QueryParam("primaryInstitution") UUID institutionKey,
                                     @Nullable @QueryParam("primaryCollection") UUID collectionKey,
                                     @Nullable @Context Pageable page) {
    page = page == null ? new PagingRequest() : page;
    query = query != null ? Strings.emptyToNull(CharMatcher.WHITESPACE.trimFrom(query)) : query;
    long total = personMapper.count(institutionKey, collectionKey, query);
    return new PagingResponse<>(page, total, personMapper.list(institutionKey, collectionKey, query, page));
  }

  @GET
  @Path("deleted")
  @Override
  public PagingResponse<Person> listDeleted(@Context Pageable page) {
    page = page == null ? new PagingRequest() : page;
    return new PagingResponse<>(page, personMapper.countDeleted(), personMapper.deleted(page));
  }

  @Transactional
  @Validate(groups = {PrePersist.class, Default.class})
  @Override
  public UUID create(@Valid @NotNull Person person) {
    checkArgument(person.getKey() == null, "Unable to create an entity which already has a key");

    if (person.getMailingAddress() != null) {
      checkArgument(person.getMailingAddress().getKey() == null, "Unable to create an address which already has a key");
      addressMapper.create(person.getMailingAddress());
    }

    person.setKey(UUID.randomUUID());
    personMapper.create(person);

    if (!person.getIdentifiers().isEmpty()) {
      for (Identifier identifier : person.getIdentifiers()) {
        checkArgument(
            identifier.getKey() == null, "Unable to create an identifier which already has a key");
        identifier.setCreatedBy(person.getCreatedBy());
        identifierMapper.createIdentifier(identifier);
        personMapper.addIdentifier(person.getKey(), identifier.getKey());
      }
    }

    eventBus.post(CreateCollectionEntityEvent.newInstance(person, Person.class));
    return person.getKey();
  }

  @Transactional
  @Validate
  @Override
  public void update(@Valid @NotNull Person person) {
    Person oldPerson = get(person.getKey());
    checkArgument(oldPerson != null, "Entity doesn't exist");

    if (oldPerson.getDeleted() != null) {
      // if it's deleted we only allow to update it if we undelete it
      checkArgument(person.getDeleted() == null,
                    "Unable to update a previously deleted entity unless you clear the deletion timestamp");
    } else {
      // not allowed to delete when updating
      checkArgument(person.getDeleted() == null, "Can't delete an entity when updating");
    }

    // update mailing address
    if (person.getMailingAddress() != null) {
      if (oldPerson.getMailingAddress() == null) {
        checkArgument(person.getMailingAddress().getKey() == null,
                      "Unable to create an address which already has a key");
        addressMapper.create(person.getMailingAddress());
      } else {
        addressMapper.update(person.getMailingAddress());
      }
    }

    // update entity
    personMapper.update(person);

    // check if we have to delete the mailing address
    if (person.getMailingAddress() == null && oldPerson.getMailingAddress() != null) {
      addressMapper.delete(oldPerson.getMailingAddress().getKey());
    }

    // check if we have to delete the address
    Person newPerson = get(person.getKey());
    eventBus.post(UpdateCollectionEntityEvent.newInstance(newPerson, oldPerson, Person.class));
  }

  @GET
  @Path("suggest")
  @Override
  public List<PersonSuggestResult> suggest(@QueryParam("q") String q) {
    return personMapper.suggest(q);
  }

  @POST
  @Path("{key}/identifier")
  @Trim
  @RolesAllowed({ADMIN_ROLE, GRSCICOLL_ADMIN_ROLE})
  public int addIdentifier(
    @PathParam("key") @NotNull UUID entityKey, @NotNull Identifier identifier, @Context SecurityContext security
  ) {
    identifier.setCreatedBy(security.getUserPrincipal().getName());
    return addIdentifier(entityKey, identifier);
  }

  @Validate(groups = {PrePersist.class, Default.class})
  @Override
  public int addIdentifier(@NotNull UUID entityKey, @Valid @NotNull Identifier identifier) {
    int identifierKey = WithMyBatis.addIdentifier(identifierMapper, personMapper, entityKey, identifier);
    eventBus.post(ChangedComponentEvent.newInstance(entityKey, Person.class, Identifier.class));
    return identifierKey;
  }

  @DELETE
  @Path("{key}/identifier/{identifierKey}")
  @RolesAllowed({ADMIN_ROLE, GRSCICOLL_ADMIN_ROLE})
  @Transactional
  @Override
  public void deleteIdentifier(
    @PathParam("key") @NotNull UUID entityKey,
    @PathParam("identifierKey") int identifierKey
  ) {
    WithMyBatis.deleteIdentifier(personMapper, entityKey, identifierKey);
    eventBus.post(ChangedComponentEvent.newInstance(entityKey, Person.class, Identifier.class));
  }

  @GET
  @Path("{key}/identifier")
  @Nullable
  @Validate(validateReturnedValue = true)
  @Override
  public List<Identifier> listIdentifiers(@PathParam("key") @NotNull UUID key) {
    return WithMyBatis.listIdentifiers(personMapper, key);
  }
}
