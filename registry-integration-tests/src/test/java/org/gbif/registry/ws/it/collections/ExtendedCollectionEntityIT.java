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
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.JavaType;

import static org.gbif.registry.ws.it.fixtures.TestConstants.TEST_GRSCICOLL_ADMIN;
import static org.gbif.registry.ws.it.fixtures.TestConstants.TEST_PASSWORD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public abstract class ExtendedCollectionEntityIT<
        T extends CollectionEntity & Taggable & MachineTaggable & Identifiable & Contactable>
    extends BaseCollectionEntityIT<T> {

  // query params
  private static final String CONTACT_PARAM = "contact";

  private static final JavaType LIST_KEY_CODE_NAME_TYPE =
      OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, KeyCodeNameResult.class);

  public ExtendedCollectionEntityIT(
      MockMvc mockMvc,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer,
      IdentityService identityService,
      Class<T> clazz) {
    super(mockMvc, principalProvider, esServer, identityService, clazz);
  }

  @Test
  public void createWithAddressTest() throws Exception {
    T entity = newEntity();

    Address address = new Address();
    address.setAddress("address");
    address.setCountry(Country.AFGHANISTAN);
    address.setCity("city");
    entity.setAddress(address);

    Address mailingAddress = new Address();
    mailingAddress.setAddress("mailing");
    entity.setMailingAddress(mailingAddress);

    UUID key = createEntityCall(entity);
    T entitySaved = getEntityCall(key);

    assertNotNull(entitySaved.getAddress());
    assertEquals("address", entitySaved.getAddress().getAddress());
    assertEquals(Country.AFGHANISTAN, entitySaved.getAddress().getCountry());
    assertNotNull(entitySaved.getMailingAddress());
    assertEquals("mailing", entitySaved.getMailingAddress().getAddress());
  }

  @Test
  public void listByContactTest() throws Exception {
    // persons
    Person person1 = new Person();
    person1.setFirstName("first name");
    UUID personKey1 = createPersonCall(person1);

    Person person2 = new Person();
    person2.setFirstName("first name2");
    UUID personKey2 = createPersonCall(person2);

    // entities
    T entity1 = newEntity();
    UUID entityKey1 = createEntityCall(entity1);

    T entity2 = newEntity();
    UUID entityKey2 = createEntityCall(entity2);

    // add contacts
    addContactCall(entityKey1, personKey1);
    addContactCall(entityKey1, personKey1);
    addContactCall(entityKey1, personKey2);
    addContactCall(entityKey2, personKey2);

    Map<String, List<String>> params = DEFAULT_QUERY_PARAMS.get();
    params.put(CONTACT_PARAM, Collections.singletonList(personKey1.toString()));
    assertEquals(1, listEntitiesCall(params).getResults().size());
    params.put(CONTACT_PARAM, Collections.singletonList(personKey2.toString()));
    assertEquals(2, listEntitiesCall(params).getResults().size());
    params.put(CONTACT_PARAM, Collections.singletonList(UUID.randomUUID().toString()));
    assertEquals(0, listEntitiesCall(params).getResults().size());

    // remove contact
    mockMvc.perform(
        delete(getBasePath() + entityKey1.toString() + "/contact/" + personKey2.toString()));
    params.put(CONTACT_PARAM, Collections.singletonList(personKey1.toString()));
    assertEquals(1, listEntitiesCall(params).getResults().size());
  }

  @Test
  public void contactsTest() throws Exception {
    // entities
    UUID entityKey1 = createEntityCall(newEntity());
    UUID entityKey2 = createEntityCall(newEntity());
    UUID entityKey3 = createEntityCall(newEntity());

    // contacts
    Person person1 = new Person();
    person1.setFirstName("name1");
    UUID personKey1 = createPersonCall(person1);

    Person person2 = new Person();
    person2.setFirstName("name2");
    UUID personKey2 = createPersonCall(person2);

    // add contacts
    addContactCall(entityKey1, personKey1);
    addContactCall(entityKey1, personKey2);
    addContactCall(entityKey2, personKey2);

    // list contacts
    List<Person> contactsEntity1 = listContactsCall(entityKey1);
    assertEquals(2, contactsEntity1.size());

    List<Person> contactsEntity2 = listContactsCall(entityKey2);
    assertEquals(1, contactsEntity2.size());
    assertEquals("name2", contactsEntity2.get(0).getFirstName());

    assertEquals(0, listContactsCall(entityKey3).size());

    // remove contacts
    removeContactCall(entityKey1, personKey2);
    contactsEntity1 = listContactsCall(entityKey1);
    assertEquals(1, contactsEntity1.size());
    assertEquals("name1", contactsEntity1.get(0).getFirstName());

    removeContactCall(entityKey2, personKey2);
    assertEquals(0, listContactsCall(entityKey2).size());
  }

  @Test
  public void duplicateContactTest() throws Exception {
    // entities
    UUID entityKey1 = createEntityCall(newEntity());

    // contacts
    Person person1 = new Person();
    person1.setFirstName("name1");
    UUID personKey1 = createPersonCall(person1);

    // add one contact
    addContactCall(entityKey1, personKey1);
    // add duplicate contact
    assertEquals(HttpStatus.CONFLICT.value(), addContactCall(entityKey1, personKey1));
  }

  @Test
  public void updateAddressesTest() throws Exception {
    // entities
    T entity = newEntity();
    UUID entityKey = createEntityCall(entity);
    assertNewEntity(entity);
    entity = getEntityCall(entityKey);

    // update adding address
    Address address = new Address();
    address.setAddress("address");
    address.setCountry(Country.AFGHANISTAN);
    address.setCity("city");
    entity.setAddress(address);

    Address mailingAddress = new Address();
    mailingAddress.setAddress("mailing address");
    mailingAddress.setCountry(Country.AFGHANISTAN);
    mailingAddress.setCity("city mailing");
    entity.setMailingAddress(mailingAddress);

    updateEntityCall(entity);
    entity = getEntityCall(entityKey);
    address = entity.getAddress();
    mailingAddress = entity.getMailingAddress();

    assertNotNull(entity.getAddress().getKey());
    assertEquals("address", entity.getAddress().getAddress());
    assertEquals(Country.AFGHANISTAN, entity.getAddress().getCountry());
    assertEquals("city", entity.getAddress().getCity());
    assertNotNull(entity.getMailingAddress().getKey());
    assertEquals("mailing address", entity.getMailingAddress().getAddress());
    assertEquals(Country.AFGHANISTAN, entity.getMailingAddress().getCountry());
    assertEquals("city mailing", entity.getMailingAddress().getCity());

    // update address
    address.setAddress("address2");
    mailingAddress.setAddress("mailing address2");

    updateEntityCall(entity);
    entity = getEntityCall(entityKey);
    assertEquals("address2", entity.getAddress().getAddress());
    assertEquals("mailing address2", entity.getMailingAddress().getAddress());

    // delete address
    entity.setAddress(null);
    entity.setMailingAddress(null);
    updateEntityCall(entity);
    entity = getEntityCall(entityKey);
    assertNull(entity.getAddress());
    assertNull(entity.getMailingAddress());
  }

  protected List<Person> listContactsCall(UUID entityKey) throws Exception {
    return OBJECT_MAPPER.readValue(
        mockMvc
            .perform(MockMvcRequestBuilders.get(getBasePath() + entityKey.toString() + "/contact"))
            .andReturn()
            .getResponse()
            .getContentAsString(),
        LIST_PERSON_TYPE);
  }

  protected int addContactCall(UUID entityKey, UUID personKey) throws Exception {
    return mockMvc
        .perform(
            post(getBasePath() + entityKey.toString() + "/contact")
                .contentType(MediaType.TEXT_PLAIN)
                .content(personKey.toString())
                .with(httpBasic(TEST_GRSCICOLL_ADMIN, TEST_PASSWORD)))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  protected void removeContactCall(UUID entityKey, UUID personKey) throws Exception {
    mockMvc.perform(
        delete(getBasePath() + entityKey.toString() + "/contact/" + personKey.toString())
            .with(httpBasic(TEST_GRSCICOLL_ADMIN, TEST_PASSWORD)));
  }

  protected List<KeyCodeNameResult> suggestCall(String query) throws Exception {
    return OBJECT_MAPPER.readValue(
        mockMvc
            .perform(get(getBasePath() + "suggest").queryParam(Q_PARAM, query))
            .andReturn()
            .getResponse()
            .getContentAsString(),
        LIST_KEY_CODE_NAME_TYPE);
  }
}
