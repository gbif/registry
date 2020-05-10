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

import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.collections.CrudService;
import org.gbif.api.service.registry.IdentifierService;
import org.gbif.api.service.registry.MachineTagService;
import org.gbif.api.service.registry.TagService;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.NotFoundException;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.validation.ValidationException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Base class to test the CRUD operations of {@link CollectionEntity}. */
public abstract class BaseCollectionEntityIT<
        T extends CollectionEntity & Identifiable & Taggable & MachineTaggable>
    extends BaseItTest {

  private final CrudService<T> resource;
  private final CrudService<T> client;

  protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static final Pageable DEFAULT_PAGE = new PagingRequest(0L, 5);

  protected MockMvc mockMvc;
  protected final JavaType pagingResponseType;

  @RegisterExtension public CollectionsDatabaseInitializer collectionsDatabaseInitializer;

  public BaseCollectionEntityIT(
      CrudService<T> resource,
      Class<? extends CrudService<T>> cls,
      MockMvc mockMvc,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer,
      IdentityService identityService,
      Class<T> clazz,
      int localServerPort,
      KeyStore keyStore) {
    super(principalProvider, esServer);
    this.resource = resource;
    this.client = prepareClient(localServerPort, keyStore, cls);
    this.mockMvc = mockMvc;
    this.pagingResponseType =
        OBJECT_MAPPER.getTypeFactory().constructParametricType(PagingResponse.class, clazz);
    collectionsDatabaseInitializer = new CollectionsDatabaseInitializer(identityService);
  }

  protected abstract T newEntity();

  protected abstract void assertNewEntity(T entity);

  protected abstract T updateEntity(T entity);

  protected abstract void assertUpdatedEntity(T entity);

  protected abstract T newInvalidEntity();

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void crudTest(ServiceType serviceType) {
    CrudService<T> service = getService(serviceType, resource, client);

    // create
    T entity = newEntity();
    UUID key = service.create(entity);

    assertNotNull(key);

    T entitySaved = service.get(key);
    assertEquals(key, entitySaved.getKey());
    assertNewEntity(entitySaved);
    assertNotNull(entitySaved.getCreatedBy());
    assertNotNull(entitySaved.getCreated());
    assertNotNull(entitySaved.getModifiedBy());
    assertNotNull(entitySaved.getModified());

    // update
    entity = updateEntity(entitySaved);
    service.update(entity);

    entitySaved = service.get(key);
    assertUpdatedEntity(entitySaved);

    // delete
    service.delete(key);
    entitySaved = service.get(key);
    assertNotNull(entitySaved.getDeleted());
  }

  // TODO: 10/05/2020 client exception
  @ParameterizedTest
  @EnumSource(
      value = ServiceType.class,
      names = {"RESOURCE"})
  public void createInvalidEntityTest(ServiceType serviceType) {
    CrudService<T> service = getService(serviceType, resource, client);

    assertThrows(ValidationException.class, () -> service.create(newInvalidEntity()));
  }

  // TODO: 09/05/2020 client exception, should throw IllegalStateException
  @Disabled
  @ParameterizedTest
  @EnumSource(
      value = ServiceType.class,
      names = {"RESOURCE"})
  public void deleteMissingEntityTest(ServiceType serviceType) {
    CrudService<T> service = getService(serviceType, resource, client);

    assertThrows(IllegalStateException.class, () -> service.delete(UUID.randomUUID()));
  }

  // TODO: 10/05/2020 client exception
  @ParameterizedTest
  @EnumSource(
      value = ServiceType.class,
      names = {"RESOURCE"})
  public void updateDeletedEntityTest(ServiceType serviceType) {
    CrudService<T> service = getService(serviceType, resource, client);

    T entity = newEntity();
    UUID key = service.create(entity);
    entity.setKey(key);
    service.delete(key);

    T entity2 = service.get(key);
    assertNotNull(entity2.getDeleted());
    assertThrows(IllegalArgumentException.class, () -> service.update(entity2));
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void restoreDeletedEntityTest(ServiceType serviceType) {
    CrudService<T> service = getService(serviceType, resource, client);

    T entity = newEntity();
    UUID key = service.create(entity);
    entity.setKey(key);
    service.delete(key);
    entity = service.get(key);
    assertNotNull(entity.getDeleted());

    // restore it
    entity.setDeleted(null);
    service.update(entity);
    entity = service.get(key);
    assertNull(entity.getDeleted());
  }

  // TODO: 10/05/2020 client exception
  @ParameterizedTest
  @EnumSource(
      value = ServiceType.class,
      names = {"RESOURCE"})
  public void updateInvalidEntityTest(ServiceType serviceType) {
    CrudService<T> service = getService(serviceType, resource, client);

    T entity = newEntity();
    UUID key = service.create(entity);

    T newEntity = newInvalidEntity();
    newEntity.setKey(key);
    assertThrows(ValidationException.class, () -> service.update(newEntity));
  }

  // TODO: 10/05/2020 client exception
  @ParameterizedTest
  @EnumSource(
      value = ServiceType.class,
      names = {"RESOURCE"})
  public void getMissingEntity(ServiceType serviceType) {
    CrudService<T> service = getService(serviceType, resource, client);

    assertThrows(NotFoundException.class, () -> service.get(UUID.randomUUID()));
  }

  // TODO: 10/05/2020 add listWithoutParametersTest

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void createFullEntityTest(ServiceType serviceType) {
    CrudService<T> service = getService(serviceType, resource, client);

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

    UUID key = service.create(entity);
    T entitySaved = service.get(key);

    assertEquals(1, entitySaved.getMachineTags().size());
    assertEquals("value", entitySaved.getMachineTags().get(0).getValue());
    assertEquals(1, entitySaved.getTags().size());
    assertEquals("value", entitySaved.getTags().get(0).getValue());
    assertEquals(1, entitySaved.getIdentifiers().size());
    assertEquals("id", entitySaved.getIdentifiers().get(0).getIdentifier());
    assertEquals(IdentifierType.LSID, entitySaved.getIdentifiers().get(0).getType());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void tagsTest(ServiceType serviceType) {
    CrudService<T> service = getService(serviceType, resource, client);
    TagService tagService = (TagService) service;

    UUID key = service.create(newEntity());

    Tag tag = new Tag();
    tag.setValue("value");
    int tagKey = tagService.addTag(key, tag);

    List<Tag> tags = tagService.listTags(key, null);
    assertEquals(1, tags.size());
    assertEquals(tagKey, tags.get(0).getKey());
    assertEquals("value", tags.get(0).getValue());

    tagService.deleteTag(key, tagKey);
    assertEquals(0, tagService.listTags(key, null).size());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void machineTagsTest(ServiceType serviceType) {
    CrudService<T> service = getService(serviceType, resource, client);
    MachineTagService machineTagService = (MachineTagService) service;

    T entity = newEntity();
    UUID key = service.create(entity);

    MachineTag machineTag = new MachineTag("ns", "name", "value");
    int machineTagKey = machineTagService.addMachineTag(key, machineTag);

    List<MachineTag> machineTags = machineTagService.listMachineTags(key);
    assertEquals(1, machineTags.size());
    assertEquals(machineTagKey, machineTags.get(0).getKey());
    assertEquals("value", machineTags.get(0).getValue());

    machineTagService.deleteMachineTag(key, machineTagKey);
    assertEquals(0, machineTagService.listMachineTags(key).size());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void identifiersTest(ServiceType serviceType) {
    CrudService<T> service = getService(serviceType, resource, client);
    IdentifierService identifierService = (IdentifierService) service;

    T entity = newEntity();
    UUID key = service.create(entity);

    Identifier identifier = new Identifier();
    identifier.setIdentifier("identifier");
    identifier.setType(IdentifierType.LSID);

    int identifierKey = identifierService.addIdentifier(key, identifier);

    List<Identifier> identifiers = identifierService.listIdentifiers(key);
    assertEquals(1, identifiers.size());
    assertEquals(identifierKey, identifiers.get(0).getKey());
    assertEquals("identifier", identifiers.get(0).getIdentifier());
    assertEquals(IdentifierType.LSID, identifiers.get(0).getType());

    identifierService.deleteIdentifier(key, identifierKey);
    assertEquals(0, identifierService.listIdentifiers(key).size());
  }

  protected CrudService<T> getService(ServiceType param) {
    switch (param) {
      case CLIENT:
        return client;
      case RESOURCE:
        return resource;
      default:
        throw new IllegalStateException("Must be resource or client");
    }
  }
}
