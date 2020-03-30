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
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.DatabaseInitializer;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.gbif.registry.ws.fixtures.TestConstants.TEST_PASSWORD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Base class to test the CRUD operations of {@link CollectionEntity}. */
@SpringBootTest(classes = {RegistryIntegrationTestsConfiguration.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
public abstract class BaseTest<
    T extends CollectionEntity & Identifiable & Taggable & MachineTaggable> {

  protected static final String DEFAULT_OFFSET = "0";
  protected static final String DEFAULT_LIMIT = "5";
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

  @Autowired protected MockMvc mockMvc;
  private final Class<T> clazz;

  @Rule public DatabaseInitializer databaseInitializer = new DatabaseInitializer();

  public BaseTest(Class<T> clazz) {
    this.clazz = clazz;
  }

  @Before
  public void setup() {}

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
                    .with(httpBasic("grscicoll_admin", TEST_PASSWORD)))
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
  public void createFullEntityTest() throws Exception {
    T entity = newEntity();

    Address address = new Address();
    address.setAddress("address");
    address.setCountry(Country.AFGHANISTAN);
    address.setCity("city");

    Address mailingAddress = new Address();
    mailingAddress.setAddress("mailing");

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
                        .with(httpBasic("grscicoll_admin", TEST_PASSWORD)))
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
            .with(httpBasic("grscicoll_admin", TEST_PASSWORD)));
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
                        .with(httpBasic("grscicoll_admin", TEST_PASSWORD)))
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
            .with(httpBasic("grscicoll_admin", TEST_PASSWORD)));
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
                        .with(httpBasic("grscicoll_admin", TEST_PASSWORD)))
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
            .with(httpBasic("grscicoll_admin", TEST_PASSWORD)));
    assertEquals(0, listIdentifiersCall(key).size());
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
                    .with(httpBasic("grscicoll_admin", TEST_PASSWORD)))
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
                .with(httpBasic("grscicoll_admin", TEST_PASSWORD)))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  protected int deleteEntityCall(UUID key) throws Exception {
    return mockMvc
        .perform(
            delete(getBasePath() + key.toString())
                .with(httpBasic("grscicoll_admin", TEST_PASSWORD)))
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
}
