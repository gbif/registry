/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.ws.it.collections;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.collections.PersonService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.client.collections.CollectionClient;
import org.gbif.registry.ws.client.collections.InstitutionClient;
import org.gbif.registry.ws.client.collections.PersonClient;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.web.servlet.MockMvc;

import static org.gbif.registry.ws.it.fixtures.TestConstants.WS_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PersonIT extends BaseCollectionEntityIT<Person> {

  private final InstitutionService institutionResource;
  private final InstitutionService institutionClient;
  private final CollectionService collectionResource;
  private final CollectionService collectionClient;

  private static final String FIRST_NAME = "first name";
  private static final String LAST_NAME = "last name";
  private static final String POSITION = "position";
  private static final String PHONE = "134235435";
  private static final String EMAIL = "dummy@dummy.com";

  private static final String FIRST_NAME_UPDATED = "first name updated";
  private static final String POSITION_UPDATED = "new position";
  private static final String PHONE_UPDATED = "134235433";

  @Autowired
  public PersonIT(
      PersonService personResource,
      InstitutionService institutionResource,
      CollectionService collectionResource,
      MockMvc mockMvc,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer,
      IdentityService identityService,
      @LocalServerPort int localServerPort,
      KeyStore keyStore) {
    super(
        personResource,
        PersonClient.class,
        mockMvc,
        principalProvider,
        esServer,
        identityService,
        Person.class,
        localServerPort,
        keyStore);
    this.institutionResource = institutionResource;
    this.institutionClient = prepareClient(localServerPort, keyStore, InstitutionClient.class);
    this.collectionResource = collectionResource;
    this.collectionClient = prepareClient(localServerPort, keyStore, CollectionClient.class);
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void createWithAddressTest(ServiceType serviceType) {
    PersonService service = ((PersonService) getService(serviceType));
    Person person = newEntity();

    Address mailingAddress = new Address();
    mailingAddress.setAddress("mailing");
    mailingAddress.setCity("city");
    mailingAddress.setCountry(Country.AFGHANISTAN);
    person.setMailingAddress(mailingAddress);

    Identifier identifier = new Identifier();
    identifier.setIdentifier("id");
    identifier.setType(IdentifierType.IH_IRN);
    person.setIdentifiers(Collections.singletonList(identifier));

    UUID key = service.create(person);
    Person personSaved = service.get(key);

    assertNewEntity(personSaved);
    assertNotNull(personSaved.getMailingAddress());
    assertEquals("mailing", personSaved.getMailingAddress().getAddress());
    assertEquals("city", personSaved.getMailingAddress().getCity());
    assertEquals(Country.AFGHANISTAN, personSaved.getMailingAddress().getCountry());
    assertEquals(1, personSaved.getIdentifiers().size());
    assertEquals("id", personSaved.getIdentifiers().get(0).getIdentifier());
    assertEquals(IdentifierType.IH_IRN, personSaved.getIdentifiers().get(0).getType());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void listWithoutParamsTest(ServiceType serviceType) {
    PersonService service = ((PersonService) getService(serviceType));

    Person person1 = newEntity();
    service.create(person1);

    Person person2 = newEntity();
    service.create(person2);

    Person person3 = newEntity();
    UUID key3 = service.create(person3);

    PagingResponse<Person> response = service.list(null, null, null, DEFAULT_PAGE);
    assertEquals(3, response.getResults().size());

    service.delete(key3);

    response = service.list(null, null, null, DEFAULT_PAGE);
    assertEquals(2, response.getResults().size());

    response = service.list(null, null, null, new PagingRequest(0L, 1));
    assertEquals(1, response.getResults().size());

    response = service.list(null, null, null, new PagingRequest(0L, 0));
    assertEquals(0, response.getResults().size());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void listQueryTest(ServiceType serviceType) {
    PersonService service = ((PersonService) getService(serviceType));

    Person person1 = newEntity();
    Address address = new Address();
    address.setAddress("dummy address");
    address.setCity("city");
    person1.setMailingAddress(address);
    UUID key1 = service.create(person1);

    Person person2 = newEntity();
    Address address2 = new Address();
    address2.setAddress("dummy address2");
    address2.setCity("city2");
    person2.setMailingAddress(address2);
    UUID key2 = service.create(person2);

    // query params
    PagingResponse<Person> response = service.list("dummy", null, null, DEFAULT_PAGE);
    assertEquals(2, response.getResults().size());

    // empty queries are ignored and return all elements
    response = service.list("", null, null, DEFAULT_PAGE);
    assertEquals(2, response.getResults().size());

    response = service.list("city", null, null, DEFAULT_PAGE);
    assertEquals(1, response.getResults().size());
    assertEquals(key1, response.getResults().get(0).getKey());

    response = service.list("city2", null, null, DEFAULT_PAGE);
    assertEquals(1, response.getResults().size());
    assertEquals(key2, response.getResults().get(0).getKey());

    assertEquals(2, service.list("c", null, null, DEFAULT_PAGE).getResults().size());
    assertEquals(2, service.list("dum add", null, null, DEFAULT_PAGE).getResults().size());
    assertEquals(0, service.list("<", null, null, DEFAULT_PAGE).getResults().size());
    assertEquals(0, service.list("\"<\"", null, null, DEFAULT_PAGE).getResults().size());
    assertEquals(2, service.list("  ", null, null, DEFAULT_PAGE).getResults().size());

    // update address
    person2 = service.get(key2);
    person2.getMailingAddress().setCity("city3");
    service.update(person2);
    response = service.list("city3", null, null, DEFAULT_PAGE);
    assertEquals(1, response.getResults().size());

    service.delete(key2);
    response = service.list("city3", null, null, DEFAULT_PAGE);
    assertEquals(0, response.getResults().size());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void listByInstitutionTest(ServiceType serviceType) {
    PersonService service = ((PersonService) getService(serviceType));
    InstitutionService institutionService =
        getService(serviceType, institutionResource, institutionClient);

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
    service.create(person1);

    Person person2 = newEntity();
    person2.setPrimaryInstitutionKey(institutionKey1);
    service.create(person2);

    Person person3 = newEntity();
    person3.setPrimaryInstitutionKey(institutionKey2);
    service.create(person3);

    PagingResponse<Person> response = service.list(null, institutionKey1, null, DEFAULT_PAGE);
    assertEquals(2, response.getResults().size());

    response = service.list(null, institutionKey2, null, new PagingRequest(0L, 2));
    assertEquals(1, response.getResults().size());

    response = service.list(null, UUID.randomUUID(), null, new PagingRequest(0L, 2));
    assertEquals(0, response.getResults().size());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void listByCollectionTest(ServiceType serviceType) {
    PersonService service = ((PersonService) getService(serviceType));
    CollectionService collectionService =
        getService(serviceType, collectionResource, collectionClient);

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
    service.create(person1);

    Person person2 = newEntity();
    person2.setPrimaryCollectionKey(collectionKey1);
    service.create(person2);

    Person person3 = newEntity();
    person3.setPrimaryCollectionKey(collectionKey2);
    service.create(person3);

    PagingResponse<Person> response = service.list(null, null, collectionKey1, DEFAULT_PAGE);
    assertEquals(2, response.getResults().size());

    response = service.list(null, null, collectionKey2, new PagingRequest(0L, 2));
    assertEquals(1, response.getResults().size());

    response = service.list(null, null, UUID.randomUUID(), new PagingRequest(0L, 2));
    assertEquals(0, response.getResults().size());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void listMultipleParamsTest(ServiceType serviceType) {
    PersonService service = ((PersonService) getService(serviceType));
    InstitutionService institutionService =
        getService(serviceType, institutionResource, institutionClient);
    CollectionService collectionService =
        getService(serviceType, collectionResource, collectionClient);

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
    service.create(person1);

    Person person2 = newEntity();
    person2.setFirstName("person2");
    person2.setPrimaryCollectionKey(collectionKey1);
    person2.setPrimaryInstitutionKey(institutionKey1);
    service.create(person2);

    PagingResponse<Person> response = service.list("person1", null, collectionKey1, DEFAULT_PAGE);
    assertEquals(1, response.getResults().size());

    response = service.list(LAST_NAME, null, collectionKey1, DEFAULT_PAGE);
    assertEquals(2, response.getResults().size());

    response = service.list(LAST_NAME, institutionKey1, collectionKey1, DEFAULT_PAGE);
    assertEquals(1, response.getResults().size());

    response = service.list("person2", institutionKey1, collectionKey1, DEFAULT_PAGE);
    assertEquals(1, response.getResults().size());

    response = service.list("person unknown", institutionKey1, collectionKey1, DEFAULT_PAGE);
    assertEquals(0, response.getResults().size());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void updateAddressesTest(ServiceType serviceType) {
    PersonService service = ((PersonService) getService(serviceType));

    // entities
    Person person = newEntity();
    UUID entityKey = service.create(person);
    assertNewEntity(person);
    person = service.get(entityKey);

    // update adding address
    Address address = new Address();
    address.setAddress("address");
    address.setCountry(Country.AFGHANISTAN);
    address.setCity("city");
    person.setMailingAddress(address);

    service.update(person);
    person = service.get(entityKey);
    address = person.getMailingAddress();

    assertNotNull(person.getMailingAddress().getKey());
    assertEquals("address", person.getMailingAddress().getAddress());
    assertEquals(Country.AFGHANISTAN, person.getMailingAddress().getCountry());
    assertEquals("city", person.getMailingAddress().getCity());

    // update address
    address.setAddress("address2");

    service.update(person);
    person = service.get(entityKey);
    assertEquals("address2", person.getMailingAddress().getAddress());

    // delete address
    person.setMailingAddress(null);
    service.update(person);
    person = service.get(entityKey);
    assertNull(person.getMailingAddress());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testSuggest(ServiceType serviceType) {
    PersonService service = ((PersonService) getService(serviceType));

    Person person1 = newEntity();
    person1.setFirstName("first");
    person1.setLastName("second");
    service.create(person1);

    Person person2 = newEntity();
    person2.setFirstName("first");
    person2.setLastName("second2");
    service.create(person2);

    assertEquals(2, service.suggest("first").size());
    assertEquals(2, service.suggest("sec").size());
    assertEquals(1, service.suggest("second2").size());
    assertEquals(2, service.suggest("first second").size());
    assertEquals(1, service.suggest("first second2").size());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void listDeletedTest(ServiceType serviceType) {
    PersonService service = (PersonService) getService(serviceType);

    Person person1 = newEntity();
    person1.setFirstName("first");
    person1.setLastName("second");
    UUID key1 = service.create(person1);

    Person person2 = newEntity();
    person2.setFirstName("first2");
    person2.setLastName("second2");
    UUID key2 = service.create(person2);

    assertEquals(0, service.listDeleted(DEFAULT_PAGE).getResults().size());

    service.delete(key1);
    assertEquals(1, service.listDeleted(DEFAULT_PAGE).getResults().size());

    service.delete(key2);
    assertEquals(2, service.listDeleted(DEFAULT_PAGE).getResults().size());
  }

  @Override
  protected Person newEntity() {
    Person person = new Person();
    person.setFirstName(FIRST_NAME);
    person.setLastName(LAST_NAME);
    person.setPosition(POSITION);
    person.setPhone(PHONE);
    person.setEmail(EMAIL);
    person.setCreatedBy(WS_TEST);
    person.setModifiedBy(WS_TEST);
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
    assertNotEquals(person.getCreated(), person.getModified());
  }

  @Override
  protected Person newInvalidEntity() {
    return new Person();
  }
}
