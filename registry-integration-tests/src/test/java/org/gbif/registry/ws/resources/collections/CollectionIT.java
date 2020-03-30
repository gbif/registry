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
package org.gbif.registry.ws.resources.collections;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.vocabulary.collections.AccessionStatus;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;

import static org.gbif.registry.ws.fixtures.TestConstants.TEST_PASSWORD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Tests the {@link CollectionResource}. */
public class CollectionIT extends BaseTest<Collection> {

  private static final String NAME = "name";
  private static final String DESCRIPTION = "dummy description";
  private static final AccessionStatus ACCESSION_STATUS = AccessionStatus.INSTITUTIONAL;
  private static final String CODE_UPDATED = "code2";
  private static final String NAME_UPDATED = "name2";
  private static final String DESCRIPTION_UPDATED = "dummy description updated";
  private static final AccessionStatus ACCESSION_STATUS_UPDATED = AccessionStatus.PROJECT;

  // query params
  private static final String CODE_PARAM = "code";
  private static final String NAME_PARAM = "name";
  private static final String INSTITUTION_PARAM = "institution";
  private static final String CONTACT_PARAM = "contact";

  private static final TypeReference<PagingResponse<Collection>> PAGING_RESPONSE_COLLECTIONS_TYPE =
      new TypeReference<PagingResponse<Collection>>() {};
  private static final JavaType LIST_KEY_CODE_NAME_TYPE =
      OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, KeyCodeNameResult.class);

  Supplier<Map<String, List<String>>> DEFAULT_QUERY_PARAMS =
      () -> {
        Map<String, List<String>> params = new HashMap<>();
        params.put(OFFSET_PARAM, Collections.singletonList(DEFAULT_OFFSET));
        params.put(LIMIT_PARAM, Collections.singletonList(DEFAULT_LIMIT));
        return params;
      };

  BiFunction<Integer, Integer, Map<String, List<String>>> PAGE_PARAMS =
      (offset, limit) -> {
        Map<String, List<String>> params = new HashMap<>();
        params.put(OFFSET_PARAM, Collections.singletonList(String.valueOf(offset)));
        params.put(LIMIT_PARAM, Collections.singletonList(String.valueOf(limit)));
        return params;
      };

  public CollectionIT() {
    super(Collection.class);
  }

  @Test
  public void listWithoutParametersTest() throws Exception {
    // create some collections
    createEntityCall(newEntity());
    createEntityCall(newEntity());
    UUID key3 = createEntityCall(newEntity());

    // list
    assertEquals(3, listCollectionsCall(DEFAULT_QUERY_PARAMS.get()).size());

    // delete and list
    deleteEntityCall(key3);
    assertEquals(2, listCollectionsCall(DEFAULT_QUERY_PARAMS.get()).size());

    // paging tests
    assertEquals(1, listCollectionsCall(PAGE_PARAMS.apply(0, 1)).size());
    assertEquals(0, listCollectionsCall(PAGE_PARAMS.apply(0, 0)).size());
  }

  @Test
  public void listTest() throws Exception {
    Collection collection1 = newEntity();
    collection1.setCode("c1");
    collection1.setName("n1");
    Address address = new Address();
    address.setAddress("dummy address");
    address.setCity("city");
    collection1.setAddress(address);
    UUID key1 = createEntityCall(collection1);

    Collection collection2 = newEntity();
    collection2.setCode("c2");
    collection2.setName("n2");
    Address address2 = new Address();
    address2.setAddress("dummy address2");
    address2.setCity("city2");
    collection2.setAddress(address2);
    UUID key2 = createEntityCall(collection2);

    // query param
    Map<String, List<String>> params = DEFAULT_QUERY_PARAMS.get();
    assertEquals(2, listCollectionsCall(params).size());

    params.put(Q_PARAM, Collections.singletonList("dummy"));
    assertEquals(2, listCollectionsCall(params).size());

    // empty queries are ignored and return all elements
    params.put(Q_PARAM, Collections.singletonList(""));
    assertEquals(2, listCollectionsCall(params).size());

    params.put(Q_PARAM, Collections.singletonList("city"));
    List<Collection> collections = listCollectionsCall(params);
    assertEquals(1, collections.size());
    assertEquals(key1, collections.get(0).getKey());

    params.put(Q_PARAM, Collections.singletonList("city2"));
    collections = listCollectionsCall(params);
    assertEquals(1, collections.size());
    assertEquals(key2, collections.get(0).getKey());

    params.put(Q_PARAM, Collections.singletonList("c"));
    assertEquals(2, listCollectionsCall(params).size());
    params.put(Q_PARAM, Collections.singletonList("dum add"));
    assertEquals(2, listCollectionsCall(params).size());
    params.put(Q_PARAM, Collections.singletonList("<"));
    assertEquals(0, listCollectionsCall(params).size());
    params.put(Q_PARAM, Collections.singletonList("\"<\""));
    assertEquals(0, listCollectionsCall(params).size());
    params.put(Q_PARAM, Collections.singletonList(" "));
    assertEquals(2, listCollectionsCall(params).size());

    // code and name params
    params = DEFAULT_QUERY_PARAMS.get();
    params.put(CODE_PARAM, Collections.singletonList("c1"));
    assertEquals(1, listCollectionsCall(params).size());

    params = DEFAULT_QUERY_PARAMS.get();
    params.put(NAME_PARAM, Collections.singletonList("n2"));
    assertEquals(1, listCollectionsCall(params).size());

    params = DEFAULT_QUERY_PARAMS.get();
    params.put(CODE_PARAM, Collections.singletonList("c1"));
    params.put(NAME_PARAM, Collections.singletonList("n1"));
    assertEquals(1, listCollectionsCall(params).size());

    params.put(CODE_PARAM, Collections.singletonList("c2"));
    assertEquals(0, listCollectionsCall(params).size());

    // update address
    collection2 = getEntityCall(key2);
    collection2.getAddress().setCity("city3");
    updateEntityCall(collection2);

    params = DEFAULT_QUERY_PARAMS.get();
    params.put(Q_PARAM, Collections.singletonList("city3"));
    assertEquals(1, listCollectionsCall(params).size());

    deleteEntityCall(key2);
    assertEquals(0, listCollectionsCall(params).size());
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

    Collection collection1 = newEntity();
    collection1.setInstitutionKey(institutionKey1);
    createEntityCall(collection1);

    Collection collection2 = newEntity();
    collection2.setInstitutionKey(institutionKey1);
    createEntityCall(collection2);

    Collection collection3 = newEntity();
    collection3.setInstitutionKey(institutionKey2);
    createEntityCall(collection3);

    Map<String, List<String>> params = DEFAULT_QUERY_PARAMS.get();
    params.put(INSTITUTION_PARAM, Collections.singletonList(institutionKey1.toString()));
    assertEquals(2, listCollectionsCall(params).size());

    params.put(INSTITUTION_PARAM, Collections.singletonList(institutionKey2.toString()));
    assertEquals(1, listCollectionsCall(params).size());

    params.put(INSTITUTION_PARAM, Collections.singletonList(UUID.randomUUID().toString()));
    assertEquals(0, listCollectionsCall(params).size());
  }

  @Test
  public void listMultipleParamsTest() throws Exception {
    // institutions
    Institution institution1 = new Institution();
    institution1.setCode("code1");
    institution1.setName("name1");
    UUID institutionKey1 = createInstitutionCall(institution1);

    Institution institution2 = new Institution();
    institution2.setCode("code2");
    institution2.setName("name2");
    UUID institutionKey2 = createInstitutionCall(institution2);

    Collection collection1 = newEntity();
    collection1.setCode("code1");
    collection1.setInstitutionKey(institutionKey1);
    createEntityCall(collection1);

    Collection collection2 = newEntity();
    collection2.setCode("code2");
    collection2.setInstitutionKey(institutionKey1);
    createEntityCall(collection2);

    Collection collection3 = newEntity();
    collection3.setInstitutionKey(institutionKey2);
    createEntityCall(collection3);

    Map<String, List<String>> params = DEFAULT_QUERY_PARAMS.get();
    params.put(Q_PARAM, Collections.singletonList("code1"));
    params.put(INSTITUTION_PARAM, Collections.singletonList(institutionKey1.toString()));
    assertEquals(1, listCollectionsCall(params).size());

    params.put(Q_PARAM, Collections.singletonList("foo"));
    assertEquals(0, listCollectionsCall(params).size());

    params.put(Q_PARAM, Collections.singletonList("code2"));
    assertEquals(1, listCollectionsCall(params).size());

    params.put(INSTITUTION_PARAM, Collections.singletonList(institutionKey2.toString()));
    assertEquals(0, listCollectionsCall(params).size());
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

    // collections
    Collection collection1 = newEntity();
    UUID collectionKey1 = createEntityCall(collection1);

    Collection collection2 = newEntity();
    UUID collectionKey2 = createEntityCall(collection2);

    // add contacts
    addContactCall(collectionKey1, personKey1);
    addContactCall(collectionKey1, personKey1);
    addContactCall(collectionKey1, personKey2);
    addContactCall(collectionKey2, personKey2);

    Map<String, List<String>> params = DEFAULT_QUERY_PARAMS.get();
    params.put(CONTACT_PARAM, Collections.singletonList(personKey1.toString()));
    assertEquals(1, listCollectionsCall(params).size());
    params.put(CONTACT_PARAM, Collections.singletonList(personKey2.toString()));
    assertEquals(2, listCollectionsCall(params).size());
    params.put(CONTACT_PARAM, Collections.singletonList(UUID.randomUUID().toString()));
    assertEquals(0, listCollectionsCall(params).size());

    // remove contact
    mockMvc.perform(
        delete(getBasePath() + collectionKey1.toString() + "/contact/" + personKey2.toString()));
    params.put(CONTACT_PARAM, Collections.singletonList(personKey1.toString()));
    assertEquals(1, listCollectionsCall(params).size());
  }

  @Test
  public void testSuggest() throws Exception {
    Collection collection1 = newEntity();
    collection1.setCode("CC");
    collection1.setName("Collection name");
    UUID key1 = createEntityCall(collection1);

    Collection collection2 = newEntity();
    collection2.setCode("CC2");
    collection2.setName("Collection name2");
    UUID key2 = createEntityCall(collection2);

    assertEquals(2, suggestCall("collection").size());
    assertEquals(2, suggestCall("CC").size());
    assertEquals(1, suggestCall("CC2").size());
    assertEquals(1, suggestCall("name2").size());
  }

  @Test
  public void listDeletedTest() throws Exception {
    Collection collection1 = newEntity();
    collection1.setCode("code1");
    collection1.setName("Collection name");
    UUID key1 = createEntityCall(collection1);

    Collection collection2 = newEntity();
    collection2.setCode("code2");
    collection2.setName("Collection name2");
    UUID key2 = createEntityCall(collection2);

    Map<String, List<String>> params = DEFAULT_QUERY_PARAMS.get();
    assertEquals(0, listDeletedCall(params).size());

    deleteEntityCall(key1);
    assertEquals(1, listDeletedCall(params).size());

    deleteEntityCall(key2);
    assertEquals(2, listDeletedCall(params).size());
  }

  @Override
  protected Collection newEntity() {
    Collection collection = new Collection();
    collection.setCode(UUID.randomUUID().toString());
    collection.setName(NAME);
    collection.setDescription(DESCRIPTION);
    collection.setActive(true);
    collection.setAccessionStatus(ACCESSION_STATUS);
    return collection;
  }

  @Override
  protected void assertNewEntity(Collection collection) {
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
    assertNotEquals(collection.getCreated(), collection.getModified());
  }

  @Override
  protected Collection newInvalidEntity() {
    return new Collection();
  }

  @Override
  protected String getBasePath() {
    return "/grscicoll/collection/";
  }

  private List<Collection> listCollectionsCall(Map<String, List<String>> queryParams)
      throws Exception {
    return OBJECT_MAPPER
        .readValue(
            mockMvc
                .perform(
                    MockMvcRequestBuilders.get(getBasePath())
                        .queryParams(CollectionUtils.toMultiValueMap(queryParams)))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            PAGING_RESPONSE_COLLECTIONS_TYPE)
        .getResults();
  }

  private List<Collection> listDeletedCall(Map<String, List<String>> queryParams) throws Exception {
    return OBJECT_MAPPER
        .readValue(
            mockMvc
                .perform(
                    MockMvcRequestBuilders.get(getBasePath() + "deleted")
                        .queryParams(CollectionUtils.toMultiValueMap(queryParams)))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            PAGING_RESPONSE_COLLECTIONS_TYPE)
        .getResults();
  }

  private UUID createInstitutionCall(Institution entity) throws Exception {
    return OBJECT_MAPPER.readValue(
        mockMvc
            .perform(
                post("/grscicoll/institution")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(entity))
                    .with(httpBasic("grscicoll_admin", TEST_PASSWORD)))
            .andReturn()
            .getResponse()
            .getContentAsString(),
        UUID.class);
  }

  private UUID createPersonCall(Person entity) throws Exception {
    return OBJECT_MAPPER.readValue(
        mockMvc
            .perform(
                post("/grscicoll/person")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(entity))
                    .with(httpBasic("grscicoll_admin", TEST_PASSWORD)))
            .andReturn()
            .getResponse()
            .getContentAsString(),
        UUID.class);
  }

  private void addContactCall(UUID collectionKey, UUID personKey) throws Exception {
    mockMvc.perform(
        post(getBasePath() + collectionKey.toString() + "/contact")
            .contentType(MediaType.TEXT_PLAIN)
            .content(personKey.toString())
            .with(httpBasic("grscicoll_admin", TEST_PASSWORD)));
  }

  private List<KeyCodeNameResult> suggestCall(String query) throws Exception {
    return OBJECT_MAPPER.readValue(
        mockMvc
            .perform(get(getBasePath() + "suggest").queryParam(Q_PARAM, query))
            .andReturn()
            .getResponse()
            .getContentAsString(),
        LIST_KEY_CODE_NAME_TYPE);
  }
}
