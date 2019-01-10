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
import org.gbif.api.vocabulary.collections.AccessionStatus;
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
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class CollectionIT extends BaseCollectionTest<Collection> {

  private static final String CODE = "code";
  private static final String NAME = "name";
  private static final String DESCRIPTION = "dummy description";
  private static final AccessionStatus ACCESSION_STATUS = AccessionStatus.INSTITUTIONAL;
  private static final String CODE_UPDATED = "code2";
  private static final String NAME_UPDATED = "name2";
  private static final String DESCRIPTION_UPDATED = "dummy description updated";
  private static final AccessionStatus ACCESSION_STATUS_UPDATED = AccessionStatus.PROJECT;

  private final CollectionService collectionService;
  private final InstitutionService institutionService;
  private final PersonService personService;

  @Parameterized.Parameters
  public static Iterable<Object[]> data() {
    final Injector client = webserviceClient();
    final Injector webservice = webservice();
    return ImmutableList.<Object[]>of(new Object[] {webservice.getInstance(CollectionResource.class),
                                        webservice.getInstance(InstitutionResource.class), webservice.getInstance(PersonResource.class), null},
                                      new Object[] {client.getInstance(CollectionService.class),
                                        client.getInstance(InstitutionService.class),
                                        client.getInstance(PersonService.class),
                                        client.getInstance(SimplePrincipalProvider.class)});
  }

  public CollectionIT(
    CollectionService collectionService,
    InstitutionService institutionService,
    PersonService personService,
    @Nullable SimplePrincipalProvider pp
  ) {
    super(collectionService, collectionService, collectionService, collectionService, personService, pp);
    this.collectionService = collectionService;
    this.institutionService = institutionService;
    this.personService = personService;
  }

  @Test
  public void listWithoutParametersTest() {
    Collection collection1 = newEntity();
    UUID key1 = collectionService.create(collection1);

    Collection collection2 = newEntity();
    UUID key2 = collectionService.create(collection2);

    Collection collection3 = newEntity();
    UUID key3 = collectionService.create(collection3);

    PagingResponse<Collection> response = collectionService.list(null, null, null, PAGE.apply(5, 0L));
    assertEquals(3, response.getResults().size());

    collectionService.delete(key3);

    response = collectionService.list(null, null, null, PAGE.apply(5, 0L));
    assertEquals(2, response.getResults().size());

    response = collectionService.list(null, null, null, PAGE.apply(1, 0L));
    assertEquals(1, response.getResults().size());

    response = collectionService.list(null, null, null, PAGE.apply(0, 0L));
    assertEquals(0, response.getResults().size());
  }

  @Test
  public void listQueryTest() {
    Collection collection1 = newEntity();
    Address address = new Address();
    address.setAddress("dummy address");
    address.setCity("city");
    collection1.setAddress(address);
    UUID key1 = collectionService.create(collection1);

    Collection collection2 = newEntity();
    Address address2 = new Address();
    address2.setAddress("dummy address2");
    address2.setCity("city2");
    collection2.setAddress(address2);
    UUID key2 = collectionService.create(collection2);

    Pageable page = PAGE.apply(5, 0L);
    PagingResponse<Collection> response = collectionService.list("dummy", null, null, page);
    assertEquals(2, response.getResults().size());

    // empty queries are ignored and return all elements
    response = collectionService.list("", null, null, page);
    assertEquals(2, response.getResults().size());

    response = collectionService.list("city", null, null, page);
    assertEquals(1, response.getResults().size());
    assertEquals(key1, response.getResults().get(0).getKey());

    response = collectionService.list("city2", null, null, page);
    assertEquals(1, response.getResults().size());
    assertEquals(key2, response.getResults().get(0).getKey());

    assertEquals(0, collectionService.list("c", null, null, page).getResults().size());

    collectionService.delete(key2);
    assertEquals(0, collectionService.list("city2", null, null, page).getResults().size());
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

    Collection collection1 = newEntity();
    collection1.setInstitutionKey(institutionKey1);
    collectionService.create(collection1);

    Collection collection2 = newEntity();
    collection2.setInstitutionKey(institutionKey1);
    collectionService.create(collection2);

    Collection collection3 = newEntity();
    collection3.setInstitutionKey(institutionKey2);
    collectionService.create(collection3);

    PagingResponse<Collection> response = collectionService.list(null, institutionKey1, null, PAGE.apply(5, 0L));
    assertEquals(2, response.getResults().size());

    response = collectionService.list(null, institutionKey2, null, PAGE.apply(2, 0L));
    assertEquals(1, response.getResults().size());

    response = collectionService.list(null, UUID.randomUUID(), null, PAGE.apply(2, 0L));
    assertEquals(0, response.getResults().size());
  }

  @Test
  public void listMultipleParamsTest() {
    // institutions
    Institution institution1 = new Institution();
    institution1.setCode("code1");
    institution1.setName("name1");
    UUID institutionKey1 = institutionService.create(institution1);

    Institution institution2 = new Institution();
    institution2.setCode("code2");
    institution2.setName("name2");
    UUID institutionKey2 = institutionService.create(institution2);

    Collection collection1 = newEntity();
    collection1.setCode("code1");
    collection1.setInstitutionKey(institutionKey1);
    collectionService.create(collection1);

    Collection collection2 = newEntity();
    collection2.setCode("code2");
    collection2.setInstitutionKey(institutionKey1);
    collectionService.create(collection2);

    Collection collection3 = newEntity();
    collection3.setInstitutionKey(institutionKey2);
    collectionService.create(collection3);

    PagingResponse<Collection> response = collectionService.list("code1", institutionKey1, null, PAGE.apply(5, 0L));
    assertEquals(1, response.getResults().size());

    response = collectionService.list("foo", institutionKey1, null, PAGE.apply(5, 0L));
    assertEquals(0, response.getResults().size());

    response = collectionService.list("code2", institutionKey2, null, PAGE.apply(5, 0L));
    assertEquals(0, response.getResults().size());

    response = collectionService.list("code2", institutionKey1, null, PAGE.apply(5, 0L));
    assertEquals(1, response.getResults().size());
  }

  @Test
  public void listByContactTest() {
    // persons
    Person person1 = new Person();
    person1.setFirstName("first name");
    UUID personKey1 = personService.create(person1);

    Person person2 = new Person();
    person2.setFirstName("first name2");
    UUID personKey2 = personService.create(person2);

    // collections
    Collection collection1 = newEntity();
    UUID collectionKey1 = collectionService.create(collection1);

    Collection collection2 = newEntity();
    UUID collectionKey2 = collectionService.create(collection2);

    // add contacts
    collectionService.addContact(collectionKey1, personKey1);
    collectionService.addContact(collectionKey1, personKey2);
    collectionService.addContact(collectionKey2, personKey2);

    assertEquals(1, collectionService.list(null, null, personKey1, PAGE.apply(5, 0L)).getResults().size());
    assertEquals(2, collectionService.list(null, null, personKey2, PAGE.apply(5, 0L)).getResults().size());
    assertEquals(0, collectionService.list(null, null, UUID.randomUUID(), PAGE.apply(5, 0L)).getResults().size());

    collectionService.removeContact(collectionKey1, personKey2);
    assertEquals(1, collectionService.list(null, null, personKey1, PAGE.apply(5, 0L)).getResults().size());
  }

  @Override
  protected Collection newEntity() {
    Collection collection = new Collection();
    collection.setCode(CODE);
    collection.setName(NAME);
    collection.setDescription(DESCRIPTION);
    collection.setActive(true);
    collection.setAccessionStatus(ACCESSION_STATUS);
    return collection;
  }

  @Override
  protected void assertNewEntity(Collection collection) {
    assertEquals(CODE, collection.getCode());
    assertEquals(NAME, collection.getName());
    assertEquals(DESCRIPTION, collection.getDescription());
    assertEquals(ACCESSION_STATUS, collection.getAccessionStatus());
    assertTrue(collection.isActive());
  }

  @Override
  protected Collection updateEntity(Collection collection) {
    collection.setCode(CODE_UPDATED);
    collection.setName(NAME_UPDATED);
    collection.setDescription(DESCRIPTION_UPDATED);
    collection.setAccessionStatus(ACCESSION_STATUS_UPDATED);
    return collection;
  }

  @Override
  protected void assertUpdatedEntity(Collection collection) {
    assertEquals(CODE_UPDATED, collection.getCode());
    assertEquals(NAME_UPDATED, collection.getName());
    assertEquals(DESCRIPTION_UPDATED, collection.getDescription());
    assertEquals(ACCESSION_STATUS_UPDATED, collection.getAccessionStatus());
  }

  @Override
  protected Collection newInvalidEntity() {
    return new Collection();
  }

  @Test
  public void testSuggest() {
    Collection collection1 = newEntity();
    collection1.setCode("CC");
    collection1.setName("Collection name");
    UUID key1 = collectionService.create(collection1);

    Collection collection2 = newEntity();
    collection2.setCode("CC2");
    collection2.setName("Collection name2");
    UUID key2 = collectionService.create(collection2);

    assertEquals(2, collectionService.suggest("collection").size());
    assertEquals(2, collectionService.suggest("CC").size());
    assertEquals(1, collectionService.suggest("CC2").size());
    assertEquals(1, collectionService.suggest("name2").size());
  }
}
