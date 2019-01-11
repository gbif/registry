package org.gbif.registry.ws.resources.collections;

import org.gbif.api.model.collections.Person;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.model.registry.search.collections.PersonSuggestResult;
import org.gbif.api.service.collections.PersonService;
import org.gbif.registry.events.collections.CreateCollectionEntityEvent;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.PersonMapper;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.bval.guice.Validate;
import org.mybatis.guice.transactional.Transactional;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Class that acts both as the WS endpoint for {@link Person} entities and also provides an *
 * implementation of {@link PersonService}.
 */
@Singleton
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("grbio/person")
public class PersonResource extends BaseCrudResource<Person> implements PersonService {

  private final PersonMapper personMapper;
  private final AddressMapper addressMapper;
  private final EventBus eventBus;

  @Inject
  public PersonResource(PersonMapper personMapper, AddressMapper addressMapper, EventBus eventBus) {
    super(personMapper, eventBus, Person.class);
    this.personMapper = personMapper;
    this.addressMapper = addressMapper;
    this.eventBus = eventBus;
  }

  @GET
  public PagingResponse<Person> list(@Nullable @QueryParam("q") String query,
                                     @Nullable @QueryParam("primaryInstitution") UUID institutionKey,
                                     @Nullable @QueryParam("primaryCollection") UUID collectionKey,
                                     @Nullable @Context Pageable page) {
    page = page == null ? new PagingRequest() : page;
    query = Strings.emptyToNull(query);
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
      checkArgument(
          person.getMailingAddress().getKey() == null,
          "Unable to create an address which already has a key");
      addressMapper.create(person.getMailingAddress());
    }

    person.setKey(UUID.randomUUID());
    personMapper.create(person);

    eventBus.post(CreateCollectionEntityEvent.newInstance(person, Person.class));
    return person.getKey();
  }

  @GET
  @Path("suggest")
  @Override
  public List<PersonSuggestResult> suggest(@QueryParam("q") String q) {
    return personMapper.suggest(q);
  }
}
