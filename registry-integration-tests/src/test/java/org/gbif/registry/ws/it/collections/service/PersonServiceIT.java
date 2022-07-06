/*
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
package org.gbif.registry.ws.it.collections.service;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.collections.request.PersonSearchRequest;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.collections.PersonService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.UUID;

import javax.validation.ConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.gbif.registry.ws.it.collections.data.TestDataFactory.PersonTestData.LAST_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PersonServiceIT extends BaseCollectionEntityServiceIT<Person> {

  private final PersonService personService;
  private final InstitutionService institutionService;
  private final CollectionService collectionService;

  @Autowired
  public PersonServiceIT(
      PersonService personService,
      InstitutionService institutionService,
      CollectionService collectionService,
      SimplePrincipalProvider principalProvider,
      IdentityService identityService) {
    super(personService, principalProvider, identityService, Person.class);
    this.personService = personService;
    this.institutionService = institutionService;
    this.collectionService = collectionService;
  }

  @Test
  public void createWithAddressTest() {
    Person person = testData.newEntity();

    Address mailingAddress = new Address();
    mailingAddress.setAddress("mailing");
    mailingAddress.setCity("city");
    mailingAddress.setCountry(Country.AFGHANISTAN);
    person.setMailingAddress(mailingAddress);

    UUID key = personService.create(person);
    Person personSaved = personService.get(key);

    assertTrue(personSaved.lenientEquals(person));
    assertNotNull(personSaved.getMailingAddress());
    assertEquals("mailing", personSaved.getMailingAddress().getAddress());
    assertEquals("city", personSaved.getMailingAddress().getCity());
    assertEquals(Country.AFGHANISTAN, personSaved.getMailingAddress().getCountry());
  }

  @Test
  public void listWithoutParamsTest() {
    Person person1 = testData.newEntity();
    personService.create(person1);

    Person person2 = testData.newEntity();
    personService.create(person2);

    Person person3 = testData.newEntity();
    UUID key3 = personService.create(person3);

    PagingResponse<Person> response =
        personService.list(PersonSearchRequest.builder().page(DEFAULT_PAGE).build());
    assertThat(3, greaterThanOrEqualTo(response.getResults().size()));

    personService.delete(key3);

    response = personService.list(PersonSearchRequest.builder().page(DEFAULT_PAGE).build());
    assertThat(2, greaterThanOrEqualTo(response.getResults().size()));

    response =
        personService.list(PersonSearchRequest.builder().page(new PagingRequest(0L, 1)).build());
    assertEquals(1, response.getResults().size());

    response =
        personService.list(PersonSearchRequest.builder().page(new PagingRequest(0L, 0)).build());
    assertEquals(0, response.getResults().size());
  }

  @Test
  public void listQueryTest() {
    Person person1 = testData.newEntity();
    Address address = new Address();
    address.setAddress("dummy address");
    address.setCity("city");
    person1.setMailingAddress(address);
    UUID key1 = personService.create(person1);

    Person person2 = testData.newEntity();
    Address address2 = new Address();
    address2.setAddress("dummy address2");
    address2.setCity("city2");
    person2.setMailingAddress(address2);
    UUID key2 = personService.create(person2);

    // query params
    PagingResponse<Person> response =
        personService.list(PersonSearchRequest.builder().query("dummy").page(DEFAULT_PAGE).build());
    assertEquals(2, response.getResults().size());

    // empty queries are ignored and return all elements
    response =
        personService.list(PersonSearchRequest.builder().query("").page(DEFAULT_PAGE).build());
    assertEquals(2, response.getResults().size());

    response =
        personService.list(PersonSearchRequest.builder().query("city").page(DEFAULT_PAGE).build());
    assertEquals(1, response.getResults().size());
    assertEquals(key1, response.getResults().get(0).getKey());

    response =
        personService.list(PersonSearchRequest.builder().query("city2").page(DEFAULT_PAGE).build());
    assertEquals(1, response.getResults().size());
    assertEquals(key2, response.getResults().get(0).getKey());

    assertEquals(
        2,
        personService
            .list(PersonSearchRequest.builder().query("c").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        2,
        personService
            .list(PersonSearchRequest.builder().query("dum add").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        0,
        personService
            .list(PersonSearchRequest.builder().query("<").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        0,
        personService
            .list(PersonSearchRequest.builder().query("\"<\"").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        2,
        personService
            .list(PersonSearchRequest.builder().query("  ").page(DEFAULT_PAGE).build())
            .getResults()
            .size());

    // update address
    person2 = personService.get(key2);
    person2.getMailingAddress().setCity("city3");
    personService.update(person2);
    response =
        personService.list(PersonSearchRequest.builder().query("city3").page(DEFAULT_PAGE).build());
    assertEquals(1, response.getResults().size());

    personService.delete(key2);
    response =
        personService.list(PersonSearchRequest.builder().query("city3").page(DEFAULT_PAGE).build());
    assertEquals(0, response.getResults().size());
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
    Person person1 = testData.newEntity();
    person1.setPrimaryInstitutionKey(institutionKey1);
    personService.create(person1);

    Person person2 = testData.newEntity();
    person2.setPrimaryInstitutionKey(institutionKey1);
    personService.create(person2);

    Person person3 = testData.newEntity();
    person3.setPrimaryInstitutionKey(institutionKey2);
    personService.create(person3);

    PagingResponse<Person> response =
        personService.list(
            PersonSearchRequest.builder()
                .primaryInstitution(institutionKey1)
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(2, response.getResults().size());

    response =
        personService.list(
            PersonSearchRequest.builder()
                .primaryInstitution(institutionKey2)
                .page(new PagingRequest(0L, 2))
                .build());
    assertEquals(1, response.getResults().size());

    response =
        personService.list(
            PersonSearchRequest.builder()
                .primaryInstitution(UUID.randomUUID())
                .page(new PagingRequest(0L, 2))
                .build());
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
    Person person1 = testData.newEntity();
    person1.setPrimaryCollectionKey(collectionKey1);
    personService.create(person1);

    Person person2 = testData.newEntity();
    person2.setPrimaryCollectionKey(collectionKey1);
    personService.create(person2);

    Person person3 = testData.newEntity();
    person3.setPrimaryCollectionKey(collectionKey2);
    personService.create(person3);

    PagingResponse<Person> response =
        personService.list(
            PersonSearchRequest.builder()
                .primaryCollection(collectionKey1)
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(2, response.getResults().size());

    response =
        personService.list(
            PersonSearchRequest.builder()
                .primaryCollection(collectionKey2)
                .page(new PagingRequest(0L, 2))
                .build());
    assertEquals(1, response.getResults().size());

    response =
        personService.list(
            PersonSearchRequest.builder()
                .primaryCollection(UUID.randomUUID())
                .page(new PagingRequest(0L, 2))
                .build());
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
    Person person1 = testData.newEntity();
    person1.setFirstName("person1");
    person1.setPrimaryCollectionKey(collectionKey1);
    UUID personKey1 = personService.create(person1);

    Identifier id1 = new Identifier(IdentifierType.IH_IRN, "test");
    personService.addIdentifier(personKey1, id1);

    Person person2 = testData.newEntity();
    person2.setFirstName("person2");
    person2.setPrimaryCollectionKey(collectionKey1);
    person2.setPrimaryInstitutionKey(institutionKey1);
    UUID personKey2 = personService.create(person2);

    MachineTag mt1 = new MachineTag("test", "foo", "test");
    personService.addMachineTag(personKey2, mt1);

    PagingResponse<Person> response =
        personService.list(
            PersonSearchRequest.builder()
                .query("person1")
                .primaryCollection(collectionKey1)
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(1, response.getResults().size());

    response =
        personService.list(
            PersonSearchRequest.builder()
                .query(LAST_NAME)
                .primaryCollection(collectionKey1)
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(2, response.getResults().size());

    response =
        personService.list(
            PersonSearchRequest.builder()
                .query(LAST_NAME)
                .primaryInstitution(institutionKey1)
                .primaryCollection(collectionKey1)
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(1, response.getResults().size());

    response =
        personService.list(
            PersonSearchRequest.builder()
                .query("person2")
                .primaryInstitution(institutionKey1)
                .primaryCollection(collectionKey1)
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(1, response.getResults().size());

    response =
        personService.list(
            PersonSearchRequest.builder()
                .query("person unknown")
                .primaryInstitution(institutionKey1)
                .primaryCollection(collectionKey1)
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(0, response.getResults().size());

    response =
        personService.list(
            PersonSearchRequest.builder()
                .identifierType(IdentifierType.IH_IRN)
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(1, response.getResults().size());

    response =
        personService.list(
            PersonSearchRequest.builder()
                .identifier(id1.getIdentifier())
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(1, response.getResults().size());

    response =
        personService.list(
            PersonSearchRequest.builder().machineTagName(mt1.getName()).page(DEFAULT_PAGE).build());
    assertEquals(1, response.getResults().size());

    response =
        personService.list(
            PersonSearchRequest.builder()
                .machineTagNamespace(mt1.getNamespace())
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(1, response.getResults().size());

    response =
        personService.list(
            PersonSearchRequest.builder()
                .machineTagValue(mt1.getValue())
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(1, response.getResults().size());
  }

  @Test
  public void updateAddressesTest() {
    // entities
    Person newPerson = testData.newEntity();
    UUID entityKey = personService.create(newPerson);
    Person person = personService.get(entityKey);
    assertTrue(newPerson.lenientEquals(person));

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

  @Test
  public void testSuggest() {
    Person person1 = testData.newEntity();
    person1.setFirstName("first");
    person1.setLastName("second");
    personService.create(person1);

    Person person2 = testData.newEntity();
    person2.setFirstName("first");
    person2.setLastName("second2");
    personService.create(person2);

    Person person3 = testData.newEntity();
    person3.setFirstName("first");
    person3.setLastName("second2");
    UUID key3 = personService.create(person3);
    personService.delete(key3);

    assertEquals(2, personService.suggest("first").size());
    assertEquals(2, personService.suggest("sec").size());
    assertEquals(1, personService.suggest("second2").size());
    assertEquals(2, personService.suggest("first second").size());
    assertEquals(1, personService.suggest("first second2").size());
  }

  @Test
  public void listDeletedTest() {
    Person person1 = testData.newEntity();
    person1.setFirstName("first");
    person1.setLastName("second");
    UUID key1 = personService.create(person1);

    Person person2 = testData.newEntity();
    person2.setFirstName("first2");
    person2.setLastName("second2");
    UUID key2 = personService.create(person2);

    assertEquals(0, personService.listDeleted(DEFAULT_PAGE).getResults().size());

    personService.delete(key1);
    assertEquals(1, personService.listDeleted(DEFAULT_PAGE).getResults().size());

    personService.delete(key2);
    assertEquals(2, personService.listDeleted(DEFAULT_PAGE).getResults().size());
  }

  @Test
  public void invalidEmailsTest() {
    Person person = new Person();
    person.setFirstName("asfsafas");
    person.setEmail("asfs");

    assertThrows(ConstraintViolationException.class, () -> personService.create(person));

    person.setEmail("aa@aa.com");
    UUID key = personService.create(person);
    Person personCreated = personService.get(key);

    personCreated.setEmail("asfs");
    assertThrows(ConstraintViolationException.class, () -> personService.update(personCreated));
  }
}
