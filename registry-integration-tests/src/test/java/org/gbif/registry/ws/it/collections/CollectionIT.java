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
import org.gbif.api.model.collections.request.CollectionSearchRequest;
import org.gbif.api.model.collections.view.CollectionView;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.collections.PersonService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.collections.AccessionStatus;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.client.collections.CollectionClient;
import org.gbif.registry.ws.client.collections.InstitutionClient;
import org.gbif.registry.ws.resources.collections.CollectionResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.validation.ValidationException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
      DatasetService datasetService,
      NodeService nodeService,
      OrganizationService organizationService,
      InstallationService installationService,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer,
      IdentityService identityService,
      @LocalServerPort int localServerPort,
      KeyStore keyStore) {
    super(
        collectionResource,
        CollectionClient.class,
        personResource,
        datasetService,
        nodeService,
        organizationService,
        installationService,
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
    address.setCountry(Country.DENMARK);
    collection1.setAddress(address);
    collection1.setAlternativeCodes(Collections.singletonList(new AlternativeCode("alt", "test")));
    UUID key1 = service.create(collection1);

    Collection collection2 = newEntity();
    collection2.setCode("c2");
    collection2.setName("n2");
    Address address2 = new Address();
    address2.setAddress("dummy address2");
    address2.setCity("city2");
    address2.setCountry(Country.SPAIN);
    collection2.setAddress(address2);
    UUID key2 = service.create(collection2);

    // query param
    PagingResponse<CollectionView> response =
        service.list(CollectionSearchRequest.builder().query("dummy").page(DEFAULT_PAGE).build());
    assertEquals(2, response.getResults().size());

    // empty queries are ignored and return all elements
    response = service.list(CollectionSearchRequest.builder().query("").page(DEFAULT_PAGE).build());
    assertEquals(2, response.getResults().size());

    response =
        service.list(CollectionSearchRequest.builder().query("city").page(DEFAULT_PAGE).build());
    assertEquals(1, response.getResults().size());
    assertEquals(key1, response.getResults().get(0).getCollection().getKey());

    response =
        service.list(CollectionSearchRequest.builder().query("city2").page(DEFAULT_PAGE).build());
    assertEquals(1, response.getResults().size());
    assertEquals(key2, response.getResults().get(0).getCollection().getKey());

    assertEquals(
        2,
        service
            .list(CollectionSearchRequest.builder().query("c").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        2,
        service
            .list(CollectionSearchRequest.builder().query("dum add").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        0,
        service
            .list(CollectionSearchRequest.builder().query("<").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        0,
        service
            .list(CollectionSearchRequest.builder().query("\"<\"").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        2,
        service
            .list(CollectionSearchRequest.builder().page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        2,
        service
            .list(CollectionSearchRequest.builder().query("  ").page(DEFAULT_PAGE).build())
            .getResults()
            .size());

    // code and name params
    assertEquals(
        1,
        service
            .list(CollectionSearchRequest.builder().code("c1").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        1,
        service
            .list(CollectionSearchRequest.builder().name("n2").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        1,
        service
            .list(
                CollectionSearchRequest.builder().code("c1").name("n1").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        0,
        service
            .list(
                CollectionSearchRequest.builder().code("c2").name("n1").page(DEFAULT_PAGE).build())
            .getResults()
            .size());

    // alternative code
    assertEquals(
        1,
        service
            .list(
                CollectionSearchRequest.builder().alternativeCode("alt").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        0,
        service
            .list(
                CollectionSearchRequest.builder().alternativeCode("foo").page(DEFAULT_PAGE).build())
            .getResults()
            .size());

    // country
    List<CollectionView> results =
        service
            .list(
                CollectionSearchRequest.builder().country(Country.SPAIN).page(DEFAULT_PAGE).build())
            .getResults();
    assertEquals(1, results.size());
    assertEquals(key2, results.get(0).getCollection().getKey());
    assertEquals(
        0,
        service
            .list(
                CollectionSearchRequest.builder()
                    .country(Country.AFGHANISTAN)
                    .page(DEFAULT_PAGE)
                    .build())
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
            .list(CollectionSearchRequest.builder().query("city3").page(DEFAULT_PAGE).build())
            .getResults()
            .size());

    service.delete(key2);
    assertEquals(
        0,
        service
            .list(CollectionSearchRequest.builder().query("city3").page(DEFAULT_PAGE).build())
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

    PagingResponse<CollectionView> response =
        service.list(
            CollectionSearchRequest.builder()
                .institution(institutionKey1)
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(2, response.getResults().size());

    response =
        service.list(
            CollectionSearchRequest.builder()
                .institution(institutionKey2)
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(1, response.getResults().size());

    response =
        service.list(
            CollectionSearchRequest.builder()
                .institution(UUID.randomUUID())
                .page(DEFAULT_PAGE)
                .build());
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

    PagingResponse<CollectionView> response =
        service.list(
            CollectionSearchRequest.builder()
                .query("code1")
                .institution(institutionKey1)
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(1, response.getResults().size());

    response =
        service.list(
            CollectionSearchRequest.builder()
                .query("foo")
                .institution(institutionKey1)
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(0, response.getResults().size());

    response =
        service.list(
            CollectionSearchRequest.builder()
                .query("code2")
                .institution(institutionKey2)
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(0, response.getResults().size());

    response =
        service.list(
            CollectionSearchRequest.builder()
                .query("code2")
                .institution(institutionKey1)
                .page(DEFAULT_PAGE)
                .build());
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

    PagingResponse<CollectionView> response =
        service.list(CollectionSearchRequest.builder().page(DEFAULT_PAGE).build());
    assertEquals(3, response.getResults().size());

    service.delete(key3);

    response = service.list(CollectionSearchRequest.builder().page(DEFAULT_PAGE).build());
    assertEquals(2, response.getResults().size());

    response =
        service.list(CollectionSearchRequest.builder().page(new PagingRequest(0L, 1)).build());
    assertEquals(1, response.getResults().size());

    response =
        service.list(CollectionSearchRequest.builder().page(new PagingRequest(0L, 0)).build());
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
            .list(CollectionSearchRequest.builder().contact(personKey1).page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        2,
        service
            .list(CollectionSearchRequest.builder().contact(personKey2).page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        0,
        service
            .list(
                CollectionSearchRequest.builder()
                    .contact(UUID.randomUUID())
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());

    service.removeContact(collectionKey1, personKey2);
    assertEquals(
        1,
        service
            .list(CollectionSearchRequest.builder().contact(personKey1).page(DEFAULT_PAGE).build())
            .getResults()
            .size());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void createCollectionWithoutCodeTest(ServiceType serviceType) {
    CollectionService service = (CollectionService) getService(serviceType);
    Collection c = newEntity();
    c.setCode(null);
    assertThrows(ValidationException.class, () -> service.create(c));
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void updateCollectionWithoutCodeTest(ServiceType serviceType) {
    CollectionService service = (CollectionService) getService(serviceType);
    Collection c = newEntity();
    service.create(c);

    c.setCode(null);
    assertThrows(IllegalArgumentException.class, () -> service.update(c));
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void updateAndReplaceTest(ServiceType serviceType) {
    CollectionService service = (CollectionService) getService(serviceType);
    Collection c = newEntity();
    service.create(c);

    c.setReplacedBy(UUID.randomUUID());
    assertThrows(IllegalArgumentException.class, () -> service.update(c));
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void listPossibleDuplicates(ServiceType serviceType) {
    CollectionService service = (CollectionService) getService(serviceType);

    Collection collection1 = newEntity();
    UUID key1 = service.create(collection1);
    collection1.setKey(key1);

    Collection collection2 = newEntity();
    collection2.setCode(collection1.getCode());
    UUID key2 = service.create(collection2);
    collection2.setKey(key2);

    Collection collection3 = newEntity();
    collection3.setName(UUID.randomUUID().toString());
    UUID key3 = service.create(collection3);
    collection3.setKey(key3);

    List<Collection> duplicates = service.listPossibleDuplicates(collection1);
    assertEquals(1, duplicates.size());

    duplicates = service.listPossibleDuplicates(collection3);
    assertEquals(0, duplicates.size());
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
