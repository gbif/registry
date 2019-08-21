package org.gbif.registry.ws.resources.collections;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.PersonSuggestResult;
import org.gbif.api.service.collections.PersonService;
import org.gbif.registry.events.collections.CreateCollectionEntityEvent;
import org.gbif.registry.events.collections.UpdateCollectionEntityEvent;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.PersonMapper;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Context;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.GRSCICOLL_ADMIN_ROLE;

@RestController
@RequestMapping("/grscicoll/person")
public class PersonResource extends BaseCrudResource<Person> implements PersonService {

  private final PersonMapper personMapper;
  private final AddressMapper addressMapper;
  private final EventBus eventBus;

  public PersonResource(PersonMapper personMapper, AddressMapper addressMapper, EventBus eventBus) {
    super(personMapper, eventBus, Person.class);
    this.personMapper = personMapper;
    this.addressMapper = addressMapper;
    this.eventBus = eventBus;
  }

  // TODO: 2019-08-21 implement validation
  // TODO: 2019-08-21 check Context annotation

  @Override
  @RequestMapping(method = RequestMethod.POST)
  @Transactional
//  @Validate(groups = {PrePersist.class, Default.class})
  @Secured({ADMIN_ROLE, GRSCICOLL_ADMIN_ROLE})
  public UUID create(@Valid @NotNull @RequestBody Person person) {
    checkArgument(person.getKey() == null, "Unable to create an entity which already has a key");

    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    person.setCreatedBy(authentication.getName());
    person.setModifiedBy(authentication.getName());

    if (person.getMailingAddress() != null) {
      checkArgument(person.getMailingAddress().getKey() == null, "Unable to create an address which already has a key");
      addressMapper.create(person.getMailingAddress());
    }

    person.setKey(UUID.randomUUID());
    personMapper.create(person);

    eventBus.post(CreateCollectionEntityEvent.newInstance(person, Person.class));
    return person.getKey();
  }

  @Transactional
//  @Validate
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

  @RequestMapping(method = RequestMethod.GET)
  @Override
  public PagingResponse<Person> list(@Nullable @RequestParam("q") String query,
                                     @Nullable @RequestParam("primaryInstitution") UUID institutionKey,
                                     @Nullable @RequestParam("primaryCollection") UUID collectionKey,
                                     @Nullable @Context Pageable page) {
    page = page == null ? new PagingRequest() : page;
    query = query != null ? Strings.emptyToNull(CharMatcher.WHITESPACE.trimFrom(query)) : query;
    long total = personMapper.count(institutionKey, collectionKey, query);
    return new PagingResponse<>(page, total, personMapper.list(institutionKey, collectionKey, query, page));
  }

  @GetMapping("deleted")
  @Override
  public PagingResponse<Person> listDeleted(@Nullable @Context Pageable page) {
    page = page == null ? new PagingRequest() : page;
    return new PagingResponse<>(page, personMapper.countDeleted(), personMapper.deleted(page));
  }

  @GetMapping("suggest")
  @Override
  public List<PersonSuggestResult> suggest(@Nullable @RequestParam("q") String q) {
    return personMapper.suggest(q);
  }
}
