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

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.gbif.registry.ws.it.fixtures.TestConstants.TEST_GRSCICOLL_ADMIN;
import static org.gbif.registry.ws.it.fixtures.TestConstants.TEST_PASSWORD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Base class to test the CRUD operations of {@link CollectionEntity}. */
public abstract class BaseCollectionEntityIT<
        T extends CollectionEntity & Identifiable & Taggable & MachineTaggable>
    extends BaseItTest {

  protected static final int DEFAULT_OFFSET = 0;
  protected static final int DEFAULT_LIMIT = 5;
  protected static final String OFFSET_PARAM = "offset";
  protected static final String LIMIT_PARAM = "limit";
  protected static final String Q_PARAM = "q";

  protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  protected static final JavaType LIST_TAG_TYPE =
      OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, Tag.class);
  protected static final JavaType LIST_MACHINE_TAG_TYPE =
      OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, MachineTag.class);
  protected static final JavaType LIST_IDENTIFIER_TYPE =
      OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, Identifier.class);
  protected static final JavaType LIST_PERSON_TYPE =
      OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, Person.class);

  protected static final BiFunction<Integer, Integer, Map<String, List<String>>> PAGE_PARAMS =
      (offset, limit) -> {
        Map<String, List<String>> params = new HashMap<>();
        params.put(OFFSET_PARAM, Collections.singletonList(String.valueOf(offset)));
        params.put(LIMIT_PARAM, Collections.singletonList(String.valueOf(limit)));
        return params;
      };

  protected static final Function<String, Map<String, List<String>>> Q_SEARCH_PARAMS =
      q -> {
        Map<String, List<String>> params = PAGE_PARAMS.apply(DEFAULT_OFFSET, DEFAULT_LIMIT);
        params.put(Q_PARAM, Collections.singletonList(q));
        return params;
      };

  protected static final Supplier<Map<String, List<String>>> DEFAULT_QUERY_PARAMS =
      () -> PAGE_PARAMS.apply(DEFAULT_OFFSET, DEFAULT_LIMIT);

  public static final Pageable DEFAULT_PAGE = new PagingRequest(0L, 5);

  protected MockMvc mockMvc;
  private final Class<T> clazz;
  protected final JavaType pagingResponseType;

  @RegisterExtension public CollectionsDatabaseInitializer collectionsDatabaseInitializer;

  public BaseCollectionEntityIT(
      MockMvc mockMvc,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer,
      IdentityService identityService,
      Class<T> clazz) {
    super(principalProvider, esServer);
    this.mockMvc = mockMvc;
    this.clazz = clazz;
    this.pagingResponseType =
        OBJECT_MAPPER.getTypeFactory().constructParametricType(PagingResponse.class, clazz);
    collectionsDatabaseInitializer = new CollectionsDatabaseInitializer(identityService);
  }

  protected abstract T newEntity();

  protected abstract void assertNewEntity(T entity);

  protected abstract T updateEntity(T entity);

  protected abstract void assertUpdatedEntity(T entity);

  protected abstract T newInvalidEntity();

  protected abstract String getBasePath();

  @Test
  public void crudTest() throws Exception {
    // create
    T entity = newEntity();
    UUID key = createEntityCall(entity);

    assertNotNull(key);

    T entitySaved = getEntityCall(key);
    assertEquals(key, entitySaved.getKey());
    assertNewEntity(entitySaved);
    assertNotNull(entitySaved.getCreatedBy());
    assertNotNull(entitySaved.getCreated());
    assertNotNull(entitySaved.getModifiedBy());
    assertNotNull(entitySaved.getModified());

    // update
    entity = updateEntity(entitySaved);
    updateEntityCall(entity);

    entitySaved = getEntityCall(key);
    assertUpdatedEntity(entitySaved);

    // delete
    deleteEntityCall(key);
    entitySaved = getEntityCall(key);
    assertNotNull(entitySaved.getDeleted());
  }

  @Test
  public void createInvalidEntityTest() throws Exception {
    int status =
        mockMvc
            .perform(
                post(getBasePath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(newInvalidEntity()))
                    .with(httpBasic(TEST_GRSCICOLL_ADMIN, TEST_PASSWORD)))
            .andReturn()
            .getResponse()
            .getStatus();
    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), status);
  }

  @Test
  public void deleteMissingEntityTest() throws Exception {
    assertEquals(HttpStatus.BAD_REQUEST.value(), deleteEntityCall(UUID.randomUUID()));
  }

  @Test
  public void updateDeletedEntityTest() throws Exception {
    T entity = newEntity();
    UUID key = createEntityCall(entity);
    entity.setKey(key);
    deleteEntityCall(key);
    entity = getEntityCall(key);
    assertNotNull(entity.getDeleted());
    assertEquals(HttpStatus.BAD_REQUEST.value(), updateEntityCall(entity));
  }

  @Test
  public void restoreDeletedEntityTest() throws Exception {
    T entity = newEntity();
    UUID key = createEntityCall(entity);
    entity.setKey(key);
    deleteEntityCall(key);
    entity = getEntityCall(key);
    assertNotNull(entity.getDeleted());

    // restore it
    entity.setDeleted(null);
    updateEntityCall(entity);
    entity = getEntityCall(key);
    assertNull(entity.getDeleted());
  }

  @Test
  public void updateInvalidEntityTest() throws Exception {
    T entity = newEntity();
    UUID key = createEntityCall(entity);
    entity = newInvalidEntity();
    entity.setKey(key);
    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), updateEntityCall(entity));
  }

  @Test
  public void getMissingEntity() throws Exception {
    assertEquals(
        HttpStatus.NOT_FOUND.value(),
        mockMvc
            .perform(get(getBasePath() + UUID.randomUUID().toString()))
            .andReturn()
            .getResponse()
            .getStatus());
  }

  @Test
  public void listWithoutParametersTest() throws Exception {
    T entity1 = newEntity();
    UUID key1 = createEntityCall(entity1);

    T entity2 = newEntity();
    UUID key2 = createEntityCall(entity2);

    T entity3 = newEntity();
    UUID key3 = createEntityCall(entity3);

    // list
    assertEquals(3, listEntitiesCall(DEFAULT_QUERY_PARAMS.get()).getResults().size());

    // delete and list
    deleteEntityCall(key3);
    assertEquals(2, listEntitiesCall(DEFAULT_QUERY_PARAMS.get()).getResults().size());

    // paging tests
    assertEquals(1, listEntitiesCall(PAGE_PARAMS.apply(0, 1)).getResults().size());
    assertEquals(0, listEntitiesCall(PAGE_PARAMS.apply(0, 0)).getResults().size());
  }

  @Test
  public void createFullEntityTest() throws Exception {
    T entity = newEntity();

    MachineTag machineTag = new MachineTag("ns", "name", "value");
    entity.setMachineTags(Collections.singletonList(machineTag));

    Tag tag = new Tag();
    tag.setValue("value");
    entity.setTags(Collections.singletonList(tag));

    Identifier identifier = new Identifier();
    identifier.setIdentifier("id");
    identifier.setType(IdentifierType.LSID);
    entity.setIdentifiers(Collections.singletonList(identifier));

    UUID key = createEntityCall(entity);
    T entitySaved = getEntityCall(key);

    assertEquals(1, entitySaved.getMachineTags().size());
    assertEquals("value", entitySaved.getMachineTags().get(0).getValue());
    assertEquals(1, entitySaved.getTags().size());
    assertEquals("value", entitySaved.getTags().get(0).getValue());
    assertEquals(1, entitySaved.getIdentifiers().size());
    assertEquals("id", entitySaved.getIdentifiers().get(0).getIdentifier());
    assertEquals(IdentifierType.LSID, entitySaved.getIdentifiers().get(0).getType());
  }

  @Test
  public void tagsTest() throws Exception {
    UUID key = createEntityCall(newEntity());

    Tag tag = new Tag();
    tag.setValue("value");

    Integer tagKey =
        OBJECT_MAPPER.readValue(
            mockMvc
                .perform(
                    post(getBasePath() + key.toString() + "/tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(tag))
                        .with(httpBasic(TEST_GRSCICOLL_ADMIN, TEST_PASSWORD)))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            Integer.class);

    List<Tag> tags = listTagsCall(key);
    assertEquals(1, tags.size());

    assertEquals(tagKey, tags.get(0).getKey());
    assertEquals("value", tags.get(0).getValue());

    // delete the tag
    mockMvc.perform(
        delete(getBasePath() + key.toString() + "/tag/" + tagKey)
            .with(httpBasic(TEST_GRSCICOLL_ADMIN, TEST_PASSWORD)));
    assertEquals(0, listTagsCall(key).size());
  }

  @Test
  public void machineTagsTest() throws Exception {
    UUID key = createEntityCall(newEntity());

    MachineTag machineTag = new MachineTag("ns", "name", "value");

    Integer machineTagKey =
        OBJECT_MAPPER.readValue(
            mockMvc
                .perform(
                    post(getBasePath() + key.toString() + "/machineTag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(machineTag))
                        .with(httpBasic(TEST_GRSCICOLL_ADMIN, TEST_PASSWORD)))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            Integer.class);

    List<MachineTag> machineTags = listMachineTagsCall(key);
    assertEquals(1, machineTags.size());
    assertEquals(machineTagKey, machineTags.get(0).getKey());
    assertEquals("value", machineTags.get(0).getValue());

    // delete the machine tag
    mockMvc.perform(
        delete(getBasePath() + key.toString() + "/machineTag/" + machineTagKey)
            .with(httpBasic(TEST_GRSCICOLL_ADMIN, TEST_PASSWORD)));
    assertEquals(0, listMachineTagsCall(key).size());
  }

  @Test
  public void identifiersTest() throws Exception {
    UUID key = createEntityCall(newEntity());

    Identifier identifier = new Identifier();
    identifier.setIdentifier("identifier");
    identifier.setType(IdentifierType.LSID);

    Integer identifierKey =
        OBJECT_MAPPER.readValue(
            mockMvc
                .perform(
                    post(getBasePath() + key.toString() + "/identifier")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(identifier))
                        .with(httpBasic(TEST_GRSCICOLL_ADMIN, TEST_PASSWORD)))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            Integer.class);

    List<Identifier> identifiers = listIdentifiersCall(key);
    assertEquals(1, identifiers.size());
    assertEquals(identifierKey, identifiers.get(0).getKey());
    assertEquals("identifier", identifiers.get(0).getIdentifier());
    assertEquals(IdentifierType.LSID, identifiers.get(0).getType());

    // delete the identifier
    mockMvc.perform(
        delete(getBasePath() + key.toString() + "/identifier/" + identifierKey)
            .with(httpBasic(TEST_GRSCICOLL_ADMIN, TEST_PASSWORD)));
    assertEquals(0, listIdentifiersCall(key).size());
  }

  @Test
  public void listDeletedTest() throws Exception {
    UUID key1 = createEntityCall(newEntity());
    UUID key2 = createEntityCall(newEntity());

    Map<String, List<String>> params = DEFAULT_QUERY_PARAMS.get();
    assertEquals(0, listDeletedCall(params).getResults().size());

    deleteEntityCall(key1);
    assertEquals(1, listDeletedCall(params).getResults().size());

    deleteEntityCall(key2);
    assertEquals(2, listDeletedCall(params).getResults().size());
  }

  protected T getEntityCall(UUID key) throws Exception {
    return OBJECT_MAPPER.readValue(
        mockMvc
            .perform(get(getBasePath() + key.toString()))
            .andReturn()
            .getResponse()
            .getContentAsString(),
        clazz);
  }

  protected UUID createEntityCall(T entity) throws Exception {
    return OBJECT_MAPPER.readValue(
        mockMvc
            .perform(
                post(getBasePath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(entity))
                    .with(httpBasic(TEST_GRSCICOLL_ADMIN, TEST_PASSWORD)))
            .andReturn()
            .getResponse()
            .getContentAsString(),
        UUID.class);
  }

  protected int updateEntityCall(T entity) throws Exception {
    return mockMvc
        .perform(
            put(getBasePath() + entity.getKey().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(entity))
                .with(httpBasic(TEST_GRSCICOLL_ADMIN, TEST_PASSWORD)))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  protected int deleteEntityCall(UUID key) throws Exception {
    return mockMvc
        .perform(
            delete(getBasePath() + key.toString())
                .with(httpBasic(TEST_GRSCICOLL_ADMIN, TEST_PASSWORD)))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  protected List<Tag> listTagsCall(UUID entityKey) throws Exception {
    return OBJECT_MAPPER.readValue(
        mockMvc
            .perform(get(getBasePath() + entityKey.toString() + "/tag"))
            .andReturn()
            .getResponse()
            .getContentAsString(),
        LIST_TAG_TYPE);
  }

  protected List<MachineTag> listMachineTagsCall(UUID entityKey) throws Exception {
    return OBJECT_MAPPER.readValue(
        mockMvc
            .perform(get(getBasePath() + entityKey.toString() + "/machineTag"))
            .andReturn()
            .getResponse()
            .getContentAsString(),
        LIST_MACHINE_TAG_TYPE);
  }

  protected List<Identifier> listIdentifiersCall(UUID entityKey) throws Exception {
    return OBJECT_MAPPER.readValue(
        mockMvc
            .perform(get(getBasePath() + entityKey.toString() + "/identifier"))
            .andReturn()
            .getResponse()
            .getContentAsString(),
        LIST_IDENTIFIER_TYPE);
  }

  protected PagingResponse<T> listEntitiesCall(Map<String, List<String>> queryParams)
      throws Exception {
    return OBJECT_MAPPER.readValue(
        mockMvc
            .perform(
                MockMvcRequestBuilders.get(getBasePath())
                    .queryParams(CollectionUtils.toMultiValueMap(queryParams)))
            .andReturn()
            .getResponse()
            .getContentAsString(),
        pagingResponseType);
  }

  protected PagingResponse<T> listDeletedCall(Map<String, List<String>> queryParams)
      throws Exception {
    return OBJECT_MAPPER.readValue(
        mockMvc
            .perform(
                MockMvcRequestBuilders.get(getBasePath() + "deleted")
                    .queryParams(CollectionUtils.toMultiValueMap(queryParams)))
            .andReturn()
            .getResponse()
            .getContentAsString(),
        pagingResponseType);
  }

  protected UUID createInstitutionCall(Institution entity) throws Exception {
    return OBJECT_MAPPER.readValue(
        mockMvc
            .perform(
                post("/grscicoll/institution")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(entity))
                    .with(httpBasic(TEST_GRSCICOLL_ADMIN, TEST_PASSWORD)))
            .andReturn()
            .getResponse()
            .getContentAsString(),
        UUID.class);
  }

  protected UUID createCollectionCall(Collection entity) throws Exception {
    return OBJECT_MAPPER.readValue(
        mockMvc
            .perform(
                post("/grscicoll/collection")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(entity))
                    .with(httpBasic(TEST_GRSCICOLL_ADMIN, TEST_PASSWORD)))
            .andReturn()
            .getResponse()
            .getContentAsString(),
        UUID.class);
  }

  protected UUID createPersonCall(Person entity) throws Exception {
    return OBJECT_MAPPER.readValue(
        mockMvc
            .perform(
                post("/grscicoll/person")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(entity))
                    .with(httpBasic(TEST_GRSCICOLL_ADMIN, TEST_PASSWORD)))
            .andReturn()
            .getResponse()
            .getContentAsString(),
        UUID.class);
  }
}
