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
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.search.collections.PersonSuggestResult;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JavaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class PersonIT extends BaseTest<Person> {

  private static final String FIRST_NAME = "first name";
  private static final String LAST_NAME = "last name";
  private static final String POSITION = "position";
  private static final String PHONE = "134235435";
  private static final String EMAIL = "dummy@dummy.com";

  private static final String FIRST_NAME_UPDATED = "first name updated";
  private static final String POSITION_UPDATED = "new position";
  private static final String PHONE_UPDATED = "134235433";

  // query params
  private static final String PRIMARY_INSTITUTION_PARAM = "primaryInstitution";
  private static final String PRIMARY_COLLECTION_PARAM = "primaryCollection";

  private static final JavaType LIST_PERSON_SUGGEST_TYPE =
      OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, PersonSuggestResult.class);

  @Autowired
  public PersonIT(
      MockMvc mockMvc,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer,
      IdentityService identityService) {
    super(mockMvc, principalProvider, esServer, identityService, Person.class);
  }

  @Test
  public void createWithAddressTest() throws Exception {
    Person person = newEntity();

    Address mailingAddress = new Address();
    mailingAddress.setAddress("mailing");
    mailingAddress.setCity("city");
    mailingAddress.setCountry(Country.AFGHANISTAN);
    person.setMailingAddress(mailingAddress);

    Identifier identifier = new Identifier();
    identifier.setIdentifier("id");
    identifier.setType(IdentifierType.IH_IRN);
    person.setIdentifiers(Arrays.asList(identifier));

    UUID key = createEntityCall(person);
    Person personSaved = getEntityCall(key);

    assertNewEntity(personSaved);
    assertNotNull(personSaved.getMailingAddress());
    assertEquals("mailing", personSaved.getMailingAddress().getAddress());
    assertEquals("city", personSaved.getMailingAddress().getCity());
    assertEquals(Country.AFGHANISTAN, personSaved.getMailingAddress().getCountry());
    assertEquals(1, personSaved.getIdentifiers().size());
    assertEquals("id", personSaved.getIdentifiers().get(0).getIdentifier());
    assertEquals(IdentifierType.IH_IRN, personSaved.getIdentifiers().get(0).getType());
  }

  @Test
  public void listQueryTest() throws Exception {
    Person person1 = newEntity();
    Address address = new Address();
    address.setAddress("dummy address");
    address.setCity("city");
    person1.setMailingAddress(address);
    UUID key1 = createEntityCall(person1);

    Person person2 = newEntity();
    Address address2 = new Address();
    address2.setAddress("dummy address2");
    address2.setCity("city2");
    person2.setMailingAddress(address2);
    UUID key2 = createEntityCall(person2);

    // query params
    assertEquals(2, listEntitiesCall(DEFAULT_QUERY_PARAMS.get()).getResults().size());
    assertEquals(2, listEntitiesCall(Q_SEARCH_PARAMS.apply("dummy")).getResults().size());

    // empty queries are ignored and return all elements
    assertEquals(2, listEntitiesCall(Q_SEARCH_PARAMS.apply("")).getResults().size());

    List<Person> persons = listEntitiesCall(Q_SEARCH_PARAMS.apply("city")).getResults();
    assertEquals(1, persons.size());
    assertEquals(key1, persons.get(0).getKey());

    persons = listEntitiesCall(Q_SEARCH_PARAMS.apply("city2")).getResults();
    assertEquals(1, persons.size());
    assertEquals(key2, persons.get(0).getKey());

    assertEquals(2, listEntitiesCall(Q_SEARCH_PARAMS.apply("c")).getResults().size());
    assertEquals(2, listEntitiesCall(Q_SEARCH_PARAMS.apply("dum add")).getResults().size());
    assertEquals(0, listEntitiesCall(Q_SEARCH_PARAMS.apply("<")).getResults().size());
    assertEquals(0, listEntitiesCall(Q_SEARCH_PARAMS.apply("\"<\"")).getResults().size());
    assertEquals(2, listEntitiesCall(Q_SEARCH_PARAMS.apply("  ")).getResults().size());

    // update address
    person2 = getEntityCall(key2);
    person2.getMailingAddress().setCity("city3");
    updateEntityCall(person2);
    assertEquals(1, listEntitiesCall(Q_SEARCH_PARAMS.apply("city3")).getResults().size());

    deleteEntityCall(key2);
    assertEquals(0, listEntitiesCall(Q_SEARCH_PARAMS.apply("city3")).getResults().size());
  }

  @Test
  public void listByInstitutionTest() throws Exception {
    // institutions
    Institution institution1 = new Institution();
    institution1.setCode("code1");
    institution1.setName("name1");
    UUID institutionKey1 = createInstitutionCall(institution1);

    Institution institution2 = new Institution();
    institution2.setCode("code2");
    institution2.setName("name2");
    UUID institutionKey2 = createInstitutionCall(institution2);

    // person
    Person person1 = newEntity();
    person1.setPrimaryInstitutionKey(institutionKey1);
    UUID key1 = createEntityCall(person1);

    Person person2 = newEntity();
    person2.setPrimaryInstitutionKey(institutionKey1);
    UUID key2 = createEntityCall(person2);

    Person person3 = newEntity();
    person3.setPrimaryInstitutionKey(institutionKey2);
    UUID key3 = createEntityCall(person3);

    Map<String, List<String>> params = DEFAULT_QUERY_PARAMS.get();
    params.put(PRIMARY_INSTITUTION_PARAM, Collections.singletonList(institutionKey1.toString()));
    assertEquals(2, listEntitiesCall(params).getResults().size());

    params.put(PRIMARY_INSTITUTION_PARAM, Collections.singletonList(institutionKey2.toString()));
    assertEquals(1, listEntitiesCall(params).getResults().size());

    params.put(PRIMARY_INSTITUTION_PARAM, Collections.singletonList(UUID.randomUUID().toString()));
    assertEquals(0, listEntitiesCall(params).getResults().size());
  }

  @Test
  public void listByCollectionTest() throws Exception {
    // collections
    Collection collection1 = new Collection();
    collection1.setCode("code1");
    collection1.setName("name1");
    UUID collectionKey1 = createCollectionCall(collection1);

    Collection collection2 = new Collection();
    collection2.setCode("code2");
    collection2.setName("name2");
    UUID collectionKey2 = createCollectionCall(collection2);

    // person
    Person person1 = newEntity();
    person1.setPrimaryCollectionKey(collectionKey1);
    UUID key1 = createEntityCall(person1);

    Person person2 = newEntity();
    person2.setPrimaryCollectionKey(collectionKey1);
    UUID key2 = createEntityCall(person2);

    Person person3 = newEntity();
    person3.setPrimaryCollectionKey(collectionKey2);
    UUID key3 = createEntityCall(person3);

    Map<String, List<String>> params = DEFAULT_QUERY_PARAMS.get();
    params.put(PRIMARY_COLLECTION_PARAM, Collections.singletonList(collectionKey1.toString()));
    assertEquals(2, listEntitiesCall(params).getResults().size());

    params.put(PRIMARY_COLLECTION_PARAM, Collections.singletonList(collectionKey2.toString()));
    assertEquals(1, listEntitiesCall(params).getResults().size());

    params.put(PRIMARY_COLLECTION_PARAM, Collections.singletonList(UUID.randomUUID().toString()));
    assertEquals(0, listEntitiesCall(params).getResults().size());
  }

  @Test
  public void listMultipleParamsTest() throws Exception {
    // institution
    Institution institution1 = new Institution();
    institution1.setCode("code1");
    institution1.setName("name1");
    UUID institutionKey1 = createInstitutionCall(institution1);

    // collection
    Collection collection1 = new Collection();
    collection1.setCode("code11");
    collection1.setName("name11");
    UUID collectionKey1 = createCollectionCall(collection1);

    // persons
    Person person1 = newEntity();
    person1.setFirstName("person1");
    person1.setPrimaryCollectionKey(collectionKey1);
    UUID key1 = createEntityCall(person1);

    Person person2 = newEntity();
    person2.setFirstName("person2");
    person2.setPrimaryCollectionKey(collectionKey1);
    person2.setPrimaryInstitutionKey(institutionKey1);
    UUID key2 = createEntityCall(person2);

    Map<String, List<String>> params = DEFAULT_QUERY_PARAMS.get();
    params.put(Q_PARAM, Collections.singletonList("person1"));
    params.put(PRIMARY_COLLECTION_PARAM, Collections.singletonList(collectionKey1.toString()));
    assertEquals(1, listEntitiesCall(params).getResults().size());

    params.put(Q_PARAM, Collections.singletonList(LAST_NAME));
    assertEquals(2, listEntitiesCall(params).getResults().size());

    params.put(PRIMARY_INSTITUTION_PARAM, Collections.singletonList(institutionKey1.toString()));
    assertEquals(1, listEntitiesCall(params).getResults().size());

    params.put(Q_PARAM, Collections.singletonList("person2"));
    assertEquals(1, listEntitiesCall(params).getResults().size());

    params.put(Q_PARAM, Collections.singletonList("person unknown"));
    assertEquals(0, listEntitiesCall(params).getResults().size());
  }

  @Test
  public void updateAddressesTest() throws Exception {
    // entities
    Person person = newEntity();
    UUID entityKey = createEntityCall(person);
    assertNewEntity(person);
    person = getEntityCall(entityKey);

    // update adding address
    Address address = new Address();
    address.setAddress("address");
    address.setCountry(Country.AFGHANISTAN);
    address.setCity("city");
    person.setMailingAddress(address);

    updateEntityCall(person);
    person = getEntityCall(entityKey);
    address = person.getMailingAddress();

    assertNotNull(person.getMailingAddress().getKey());
    assertEquals("address", person.getMailingAddress().getAddress());
    assertEquals(Country.AFGHANISTAN, person.getMailingAddress().getCountry());
    assertEquals("city", person.getMailingAddress().getCity());

    // update address
    address.setAddress("address2");

    updateEntityCall(person);
    person = getEntityCall(entityKey);
    assertEquals("address2", person.getMailingAddress().getAddress());

    // delete address
    person.setMailingAddress(null);
    updateEntityCall(person);
    person = getEntityCall(entityKey);
    assertNull(person.getMailingAddress());
  }

  @Test
  public void testSuggest() throws Exception {
    Person person1 = newEntity();
    person1.setFirstName("first");
    person1.setLastName("second");
    UUID key1 = createEntityCall(person1);

    Person person2 = newEntity();
    person2.setFirstName("first");
    person2.setLastName("second2");
    UUID key2 = createEntityCall(person2);

    assertEquals(2, suggestPersonCall("first").size());
    assertEquals(2, suggestPersonCall("sec").size());
    assertEquals(1, suggestPersonCall("second2").size());
    assertEquals(2, suggestPersonCall("first second").size());
    assertEquals(1, suggestPersonCall("first second2").size());
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
    assertNotEquals(person.getCreated(), person.getModified());
  }

  @Override
  protected Person newInvalidEntity() {
    return new Person();
  }

  @Override
  protected String getBasePath() {
    return "/grscicoll/person/";
  }

  protected List<PersonSuggestResult> suggestPersonCall(String query) throws Exception {
    return OBJECT_MAPPER.readValue(
        mockMvc
            .perform(get(getBasePath() + "suggest").queryParam(Q_PARAM, query))
            .andReturn()
            .getResponse()
            .getContentAsString(),
        LIST_PERSON_SUGGEST_TYPE);
  }
}
