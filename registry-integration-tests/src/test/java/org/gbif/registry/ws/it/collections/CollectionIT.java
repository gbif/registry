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
import org.gbif.api.model.collections.AlternativeCode;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.collections.PersonService;
import org.gbif.api.vocabulary.collections.AccessionStatus;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.client.collections.CollectionClient;
import org.gbif.registry.ws.client.collections.InstitutionClient;
import org.gbif.registry.ws.resources.collections.CollectionResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the {@link CollectionResource}. */
public class CollectionIT extends ExtendedCollectionEntityIT<Collection> {

  private final InstitutionService institutionResource;
  private final InstitutionService institutionClient;

  private static final String NAME = "name";
  private static final String DESCRIPTION = "dummy description";
  private static final AccessionStatus ACCESSION_STATUS = AccessionStatus.INSTITUTIONAL;
  private static final String CODE_UPDATED = "code2";
  private static final String NAME_UPDATED = "name2";
  private static final String DESCRIPTION_UPDATED = "dummy description updated";
  private static final AccessionStatus ACCESSION_STATUS_UPDATED = AccessionStatus.PROJECT;

  @Autowired
  public CollectionIT(
      InstitutionService institutionResource,
      CollectionService collectionResource,
      PersonService personResource,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer,
      IdentityService identityService,
      @LocalServerPort int localServerPort,
      KeyStore keyStore) {
    super(
        collectionResource,
        CollectionClient.class,
        personResource,
        principalProvider,
        esServer,
        identityService,
        localServerPort,
        keyStore);
    this.institutionResource = institutionResource;
    this.institutionClient = prepareClient(localServerPort, keyStore, InstitutionClient.class);
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void listTest(ServiceType serviceType) {
    CollectionService service = ((CollectionService) getService(serviceType));

    Collection collection1 = newEntity();
    collection1.setCode("c1");
    collection1.setName("n1");
    Address address = new Address();
    address.setAddress("dummy address");
    address.setCity("city");
    collection1.setAddress(address);
    collection1.setAlternativeCodes(Collections.singletonList(new AlternativeCode("alt", "test")));
    UUID key1 = service.create(collection1);

    Collection collection2 = newEntity();
    collection2.setCode("c2");
    collection2.setName("n2");
    Address address2 = new Address();
    address2.setAddress("dummy address2");
    address2.setCity("city2");
    collection2.setAddress(address2);
    UUID key2 = service.create(collection2);

    // query param
    PagingResponse<Collection> response =
        service.list(
            "dummy", null, null, null, null, null, null, null, null, null, null, DEFAULT_PAGE);
    assertEquals(2, response.getResults().size());

    // empty queries are ignored and return all elements
    response =
        service.list("", null, null, null, null, null, null, null, null, null, null, DEFAULT_PAGE);
    assertEquals(2, response.getResults().size());

    response =
        service.list(
            "city", null, null, null, null, null, null, null, null, null, null, DEFAULT_PAGE);
    assertEquals(1, response.getResults().size());
    assertEquals(key1, response.getResults().get(0).getKey());

    response =
        service.list(
            "city2", null, null, null, null, null, null, null, null, null, null, DEFAULT_PAGE);
    assertEquals(1, response.getResults().size());
    assertEquals(key2, response.getResults().get(0).getKey());

    assertEquals(
        2,
        service
            .list("c", null, null, null, null, null, null, null, null, null, null, DEFAULT_PAGE)
            .getResults()
            .size());
    assertEquals(
        2,
        service
            .list(
                "dum add", null, null, null, null, null, null, null, null, null, null, DEFAULT_PAGE)
            .getResults()
            .size());
    assertEquals(
        0,
        service
            .list("<", null, null, null, null, null, null, null, null, null, null, DEFAULT_PAGE)
            .getResults()
            .size());
    assertEquals(
        0,
        service
            .list("\"<\"", null, null, null, null, null, null, null, null, null, null, DEFAULT_PAGE)
            .getResults()
            .size());
    assertEquals(
        2,
        service
            .list(null, null, null, null, null, null, null, null, null, null, null, DEFAULT_PAGE)
            .getResults()
            .size());
    assertEquals(
        2,
        service
            .list("  ", null, null, null, null, null, null, null, null, null, null, DEFAULT_PAGE)
            .getResults()
            .size());

    // code and name params
    assertEquals(
        1,
        service
            .list(null, null, null, "c1", null, null, null, null, null, null, null, DEFAULT_PAGE)
            .getResults()
            .size());
    assertEquals(
        1,
        service
            .list(null, null, null, null, "n2", null, null, null, null, null, null, DEFAULT_PAGE)
            .getResults()
            .size());
    assertEquals(
        1,
        service
            .list(null, null, null, "c1", "n1", null, null, null, null, null, null, DEFAULT_PAGE)
            .getResults()
            .size());
    assertEquals(
        0,
        service
            .list(null, null, null, "c2", "n1", null, null, null, null, null, null, DEFAULT_PAGE)
            .getResults()
            .size());

    // alternative code
    assertEquals(
        1,
        service
            .list(null, null, null, null, null, "alt", null, null, null, null, null, DEFAULT_PAGE)
            .getResults()
            .size());
    assertEquals(
        0,
        service
            .list(null, null, null, null, null, "foo", null, null, null, null, null, DEFAULT_PAGE)
            .getResults()
            .size());

    // update address
    collection2 = service.get(key2);
    assertNotNull(collection2.getAddress());
    collection2.getAddress().setCity("city3");
    service.update(collection2);
    assertEquals(
        1,
        service
            .list("city3", null, null, null, null, null, null, null, null, null, null, DEFAULT_PAGE)
            .getResults()
            .size());

    service.delete(key2);
    assertEquals(
        0,
        service
            .list("city3", null, null, null, null, null, null, null, null, null, null, DEFAULT_PAGE)
            .getResults()
            .size());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void listByInstitutionTest(ServiceType serviceType) {
    CollectionService service = ((CollectionService) getService(serviceType));
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

    Collection collection1 = newEntity();
    collection1.setInstitutionKey(institutionKey1);
    service.create(collection1);

    Collection collection2 = newEntity();
    collection2.setInstitutionKey(institutionKey1);
    service.create(collection2);

    Collection collection3 = newEntity();
    collection3.setInstitutionKey(institutionKey2);
    service.create(collection3);

    PagingResponse<Collection> response =
        service.list(
            null,
            institutionKey1,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            DEFAULT_PAGE);
    assertEquals(2, response.getResults().size());

    response =
        service.list(
            null,
            institutionKey2,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            DEFAULT_PAGE);
    assertEquals(1, response.getResults().size());

    response =
        service.list(
            null,
            UUID.randomUUID(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            DEFAULT_PAGE);
    assertEquals(0, response.getResults().size());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void listMultipleParamsTest(ServiceType serviceType) {
    CollectionService service = ((CollectionService) getService(serviceType));
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

    Collection collection1 = newEntity();
    collection1.setCode("code1");
    collection1.setInstitutionKey(institutionKey1);
    service.create(collection1);

    Collection collection2 = newEntity();
    collection2.setCode("code2");
    collection2.setInstitutionKey(institutionKey1);
    service.create(collection2);

    Collection collection3 = newEntity();
    collection3.setInstitutionKey(institutionKey2);
    service.create(collection3);

    PagingResponse<Collection> response =
        service.list(
            "code1",
            institutionKey1,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            DEFAULT_PAGE);
    assertEquals(1, response.getResults().size());

    response =
        service.list(
            "foo",
            institutionKey1,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            DEFAULT_PAGE);
    assertEquals(0, response.getResults().size());

    response =
        service.list(
            "code2",
            institutionKey2,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            DEFAULT_PAGE);
    assertEquals(0, response.getResults().size());

    response =
        service.list(
            "code2",
            institutionKey1,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            DEFAULT_PAGE);
    assertEquals(1, response.getResults().size());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testSuggest(ServiceType serviceType) {
    CollectionService service = ((CollectionService) getService(serviceType));

    Collection collection1 = newEntity();
    collection1.setCode("CC");
    collection1.setName("Collection name");
    service.create(collection1);

    Collection collection2 = newEntity();
    collection2.setCode("CC2");
    collection2.setName("Collection name2");
    service.create(collection2);

    assertEquals(2, service.suggest("collection").size());
    assertEquals(2, service.suggest("CC").size());
    assertEquals(1, service.suggest("CC2").size());
    assertEquals(1, service.suggest("name2").size());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void listDeletedTest(ServiceType serviceType) {
    CollectionService service = ((CollectionService) getService(serviceType));

    Collection collection1 = newEntity();
    collection1.setCode("code1");
    collection1.setName("Collection name");
    UUID key1 = service.create(collection1);

    Collection collection2 = newEntity();
    collection2.setCode("code2");
    collection2.setName("Collection name2");
    UUID key2 = service.create(collection2);

    assertEquals(0, service.listDeleted(DEFAULT_PAGE).getResults().size());

    service.delete(key1);
    assertEquals(1, service.listDeleted(DEFAULT_PAGE).getResults().size());

    service.delete(key2);
    assertEquals(2, service.listDeleted(DEFAULT_PAGE).getResults().size());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void listWithoutParametersTest(ServiceType serviceType) {
    CollectionService service = (CollectionService) getService(serviceType);

    service.create(newEntity());
    service.create(newEntity());

    Collection collection3 = newEntity();
    UUID key3 = service.create(collection3);

    PagingResponse<Collection> response =
        service.list(
            null, null, null, null, null, null, null, null, null, null, null, DEFAULT_PAGE);
    assertEquals(3, response.getResults().size());

    service.delete(key3);

    response =
        service.list(
            null, null, null, null, null, null, null, null, null, null, null, DEFAULT_PAGE);
    assertEquals(2, response.getResults().size());

    response =
        service.list(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new PagingRequest(0L, 1));
    assertEquals(1, response.getResults().size());

    response =
        service.list(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new PagingRequest(0L, 0));
    assertEquals(0, response.getResults().size());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void listByContactTest(ServiceType serviceType) {
    CollectionService service = (CollectionService) getService(serviceType);
    PersonService personService = getService(serviceType, personResource, personClient);

    // persons
    Person person1 = new Person();
    person1.setFirstName("first name");
    UUID personKey1 = personService.create(person1);

    Person person2 = new Person();
    person2.setFirstName("first name2");
    UUID personKey2 = personService.create(person2);

    // collections
    Collection collection1 = newEntity();
    UUID collectionKey1 = service.create(collection1);

    Collection collection2 = newEntity();
    UUID collectionKey2 = service.create(collection2);

    // add contacts
    service.addContact(collectionKey1, personKey1);
    service.addContact(collectionKey1, personKey2);
    service.addContact(collectionKey2, personKey2);

    assertEquals(
        1,
        service
            .list(
                null,
                null,
                personKey1,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                DEFAULT_PAGE)
            .getResults()
            .size());
    assertEquals(
        2,
        service
            .list(
                null,
                null,
                personKey2,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                DEFAULT_PAGE)
            .getResults()
            .size());
    assertEquals(
        0,
        service
            .list(
                null,
                null,
                UUID.randomUUID(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                DEFAULT_PAGE)
            .getResults()
            .size());

    service.removeContact(collectionKey1, personKey2);
    assertEquals(
        1,
        service
            .list(
                null,
                null,
                personKey1,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                DEFAULT_PAGE)
            .getResults()
            .size());
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
}
