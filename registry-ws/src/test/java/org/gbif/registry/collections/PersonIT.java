package org.gbif.registry.collections;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.collections.PersonService;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.ws.resources.collections.CollectionResource;
import org.gbif.registry.ws.resources.collections.InstitutionResource;
import org.gbif.registry.ws.resources.collections.PersonResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.UUID;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.gbif.registry.guice.RegistryTestModules.webservice;
import static org.gbif.registry.guice.RegistryTestModules.webserviceClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class PersonIT extends CrudTest<Person> {

  private static final String FIRST_NAME = "first name";
  private static final String LAST_NAME = "last name";
  private static final String POSITION = "position";
  private static final String PHONE = "134235435";
  private static final String EMAIL = "dummy@dummy.com";

  private static final String FIRST_NAME_UPDATED = "first name updated";
  private static final String POSITION_UPDATED = "new position";
  private static final String PHONE_UPDATED = "134235433";

  private final PersonService personService;
  private final InstitutionService institutionService;
  private final CollectionService collectionService;

  @Parameters
  public static Iterable<Object[]> data() {
    final Injector client = webserviceClient();
    final Injector webservice = webservice();
    return ImmutableList.<Object[]>of(new Object[] {webservice.getInstance(PersonResource.class),
                                        webservice.getInstance(InstitutionResource.class), webservice.getInstance(CollectionResource.class), null},
                                      new Object[] {client.getInstance(PersonService.class),
                                        client.getInstance(InstitutionService.class),
                                        client.getInstance(CollectionService.class),
                                        client.getInstance(SimplePrincipalProvider.class)});
  }

  public PersonIT(
    PersonService personService,
    InstitutionService institutionService,
    CollectionService collectionService,
    @Nullable SimplePrincipalProvider pp
  ) {
    super(personService, pp);
    this.personService = personService;
    this.institutionService = institutionService;
    this.collectionService = collectionService;
  }

  @Test
  public void createWithAddressTest() {
    Person person = newEntity();

    Address mailingAddress = new Address();
    mailingAddress.setAddress("mailing");
    mailingAddress.setCity("city");
    mailingAddress.setCountry(Country.AFGHANISTAN);
    person.setMailingAddress(mailingAddress);

    UUID key = personService.create(person);
    Person personSaved = personService.get(key);

    assertNewEntity(personSaved);
    assertNotNull(personSaved.getMailingAddress());
    assertEquals("mailing", personSaved.getMailingAddress().getAddress());
    assertEquals("city", personSaved.getMailingAddress().getCity());
    assertEquals(Country.AFGHANISTAN, personSaved.getMailingAddress().getCountry());
  }

  @Test
  public void listWithoutParamsTest() {
    Person person1 = newEntity();
    UUID key1 = personService.create(person1);

    Person person2 = newEntity();
    UUID key2 = personService.create(person2);

    Person person3 = newEntity();
    UUID key3 = personService.create(person3);

    PagingResponse<Person> response = personService.list(null, null, null, PAGE.apply(5, 0L));
    assertEquals(3, response.getResults().size());

    personService.delete(key3);

    response = personService.list(null, null, null, PAGE.apply(5, 0L));
    assertEquals(2, response.getResults().size());

    response = personService.list(null, null, null, PAGE.apply(1, 0L));
    assertEquals(1, response.getResults().size());

    response = personService.list(null, null, null, PAGE.apply(0, 0L));
    assertEquals(0, response.getResults().size());
  }

  @Test
  public void listQueryTest() {
    Person person1 = newEntity();
    Address address = new Address();
    address.setAddress("dummy address");
    address.setCity("city");
    person1.setMailingAddress(address);
    UUID key1 = personService.create(person1);

    Person person2 = newEntity();
    Address address2 = new Address();
    address2.setAddress("dummy address2");
    address2.setCity("city2");
    person2.setMailingAddress(address2);
    UUID key2 = personService.create(person2);

    Pageable page = PAGE.apply(5, 0L);
    PagingResponse<Person> response = personService.list("dummy", null, null, page);
    assertEquals(2, response.getResults().size());

    // empty queries are ignored and return all elements
    response = personService.list("", null, null, page);
    assertEquals(2, response.getResults().size());

    response = personService.list("city", null, null, page);
    assertEquals(1, response.getResults().size());
    assertEquals(key1, response.getResults().get(0).getKey());

    response = personService.list("city2", null, null, page);
    assertEquals(1, response.getResults().size());
    assertEquals(key2, response.getResults().get(0).getKey());

    assertEquals(0, personService.list("c", null, null, page).getResults().size());

    // update address
    person2 = personService.get(key2);
    person2.getMailingAddress().setCity("city3");
    personService.update(person2);
    assertEquals(1, personService.list("city3", null, null, page).getResults().size());

    personService.delete(key2);
    assertEquals(0, personService.list("city3", null, null, page).getResults().size());
  }

  @Test
  public void listByInstitutionTest() {
    // institutions
    Institution institution1 = new Institution();
    institution1.setCode("code1");
    institution1.setName("name1");
    UUID institutionKey1 = institutionService.create(institution1);

    Institution institution2 = new Institution();
    institution2.setCode("code2");
    institution2.setName("name2");
    UUID institutionKey2 = institutionService.create(institution2);

    // person
    Person person1 = newEntity();
    person1.setPrimaryInstitutionKey(institutionKey1);
    UUID key1 = personService.create(person1);

    Person person2 = newEntity();
    person2.setPrimaryInstitutionKey(institutionKey1);
    UUID key2 = personService.create(person2);

    Person person3 = newEntity();
    person3.setPrimaryInstitutionKey(institutionKey2);
    UUID key3 = personService.create(person3);

    PagingResponse<Person> response = personService.list(null, institutionKey1, null, PAGE.apply(5, 0L));
    assertEquals(2, response.getResults().size());

    response = personService.list(null, institutionKey2, null, PAGE.apply(2, 0L));
    assertEquals(1, response.getResults().size());

    response = personService.list(null, UUID.randomUUID(), null, PAGE.apply(2, 0L));
    assertEquals(0, response.getResults().size());
  }

  @Test
  public void listByCollectionTest() {
    // collections
    Collection collection1 = new Collection();
    collection1.setCode("code1");
    collection1.setName("name1");
    UUID collectionKey1 = collectionService.create(collection1);

    Collection collection2 = new Collection();
    collection2.setCode("code2");
    collection2.setName("name2");
    UUID collectionKey2 = collectionService.create(collection2);

    // person
    Person person1 = newEntity();
    person1.setPrimaryCollectionKey(collectionKey1);
    UUID key1 = personService.create(person1);

    Person person2 = newEntity();
    person2.setPrimaryCollectionKey(collectionKey1);
    UUID key2 = personService.create(person2);

    Person person3 = newEntity();
    person3.setPrimaryCollectionKey(collectionKey2);
    UUID key3 = personService.create(person3);

    PagingResponse<Person> response = personService.list(null, null, collectionKey1, PAGE.apply(5, 0L));
    assertEquals(2, response.getResults().size());

    response = personService.list(null, null, collectionKey2, PAGE.apply(2, 0L));
    assertEquals(1, response.getResults().size());

    response = personService.list(null, null, UUID.randomUUID(), PAGE.apply(2, 0L));
    assertEquals(0, response.getResults().size());
  }

  @Test
  public void listMultipleParamsTest() {
    // institution
    Institution institution1 = new Institution();
    institution1.setCode("code1");
    institution1.setName("name1");
    UUID institutionKey1 = institutionService.create(institution1);

    // collection
    Collection collection1 = new Collection();
    collection1.setCode("code11");
    collection1.setName("name11");
    UUID collectionKey1 = collectionService.create(collection1);

    // persons
    Person person1 = newEntity();
    person1.setFirstName("person1");
    person1.setPrimaryCollectionKey(collectionKey1);
    UUID key1 = personService.create(person1);

    Person person2 = newEntity();
    person2.setFirstName("person2");
    person2.setPrimaryCollectionKey(collectionKey1);
    person2.setPrimaryInstitutionKey(institutionKey1);
    UUID key2 = personService.create(person2);

    PagingResponse<Person> response = personService.list("person1", null, collectionKey1, PAGE.apply(5, 0L));
    assertEquals(1, response.getResults().size());

    response = personService.list(LAST_NAME, null, collectionKey1, PAGE.apply(5, 0L));
    assertEquals(2, response.getResults().size());

    response = personService.list(LAST_NAME, institutionKey1, collectionKey1, PAGE.apply(5, 0L));
    assertEquals(1, response.getResults().size());

    response = personService.list("person2", institutionKey1, collectionKey1, PAGE.apply(5, 0L));
    assertEquals(1, response.getResults().size());

    response = personService.list("person unknown", institutionKey1, collectionKey1, PAGE.apply(5, 0L));
    assertEquals(0, response.getResults().size());
  }

  @Override
  protected Person newEntity() {
    Person person = new Person();
    person.setFirstName(FIRST_NAME);
    person.setLastName(LAST_NAME);
    person.setPosition(POSITION);
    person.setPhone(PHONE);
    person.setEmail(EMAIL);
    return person;
  }

  @Override
  protected void assertNewEntity(Person person) {
    assertEquals(FIRST_NAME, person.getFirstName());
    assertEquals(LAST_NAME, person.getLastName());
    assertEquals(POSITION, person.getPosition());
    assertEquals(PHONE, person.getPhone());
    assertEquals(EMAIL, person.getEmail());
  }

  @Override
  protected Person updateEntity(Person person) {
    person.setFirstName(FIRST_NAME_UPDATED);
    person.setPosition(POSITION_UPDATED);
    person.setPhone(PHONE_UPDATED);
    return person;
  }

  @Override
  protected void assertUpdatedEntity(Person person) {
    assertEquals(FIRST_NAME_UPDATED, person.getFirstName());
    assertEquals(LAST_NAME, person.getLastName());
    assertEquals(POSITION_UPDATED, person.getPosition());
    assertEquals(PHONE_UPDATED, person.getPhone());
    assertEquals(EMAIL, person.getEmail());
  }

  @Override
  protected Person newInvalidEntity() {
    return new Person();
  }

  @Test
  public void testSuggest() {
    Person person1 = newEntity();
    person1.setFirstName("first");
    person1.setLastName("second");
    UUID key1 = personService.create(person1);

    Person person2 = newEntity();
    person2.setFirstName("first");
    person2.setLastName("second2");
    UUID key2 = personService.create(person2);

    assertEquals(2, personService.suggest("first").size());
    assertEquals(2, personService.suggest("sec").size());
    assertEquals(1, personService.suggest("second2").size());
    assertEquals(2, personService.suggest("first second").size());
    assertEquals(1, personService.suggest("first second2").size());
  }

  @Test
  public void listDeletedTest() {
    Person person1 = newEntity();
    person1.setFirstName("first");
    person1.setLastName("second");
    UUID key1 = personService.create(person1);

    Person person2 = newEntity();
    person2.setFirstName("first2");
    person2.setLastName("second2");
    UUID key2 = personService.create(person2);

    assertEquals(0, personService.listDeleted(PAGE.apply(5, 0L)).getResults().size());

    personService.delete(key1);
    assertEquals(1, personService.listDeleted(PAGE.apply(5, 0L)).getResults().size());

    personService.delete(key2);
    assertEquals(2, personService.listDeleted(PAGE.apply(5, 0L)).getResults().size());
  }

  @Test
  public void updateAddressesTest() {
    // entities
    Person person = newEntity();
    UUID entityKey = personService.create(person);
    assertNewEntity(person);
    person = personService.get(entityKey);

    // update adding address
    Address address = new Address();
    address.setAddress("address");
    address.setCountry(Country.AFGHANISTAN);
    address.setCity("city");
    person.setMailingAddress(address);

    personService.update(person);
    person = personService.get(entityKey);
    address = person.getMailingAddress();

    assertNotNull(person.getMailingAddress().getKey());
    assertEquals("address", person.getMailingAddress().getAddress());
    assertEquals(Country.AFGHANISTAN, person.getMailingAddress().getCountry());
    assertEquals("city", person.getMailingAddress().getCity());

    // update address
    address.setAddress("address2");

    personService.update(person);
    person = personService.get(entityKey);
    assertEquals("address2", person.getMailingAddress().getAddress());

    // delete address
    person.setMailingAddress(null);
    personService.update(person);
    person = personService.get(entityKey);
    assertNull(person.getMailingAddress());
  }
}
