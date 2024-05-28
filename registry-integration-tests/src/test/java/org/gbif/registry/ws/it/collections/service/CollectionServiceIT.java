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
import org.gbif.api.model.collections.*;
import org.gbif.api.model.collections.duplicates.Duplicate;
import org.gbif.api.model.collections.duplicates.DuplicatesResult;
import org.gbif.api.model.collections.latimercore.*;
import org.gbif.api.model.collections.request.CollectionSearchRequest;
import org.gbif.api.model.collections.view.CollectionView;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.*;
import org.gbif.api.vocabulary.collections.IdType;
import org.gbif.api.vocabulary.collections.MasterSourceType;
import org.gbif.api.vocabulary.collections.Source;
import org.gbif.registry.persistence.mapper.collections.params.DuplicatesSearchParams;
import org.gbif.registry.service.collections.duplicates.CollectionDuplicatesService;
import org.gbif.registry.service.collections.utils.LatimerCoreConverter;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Tests the {@link CollectionService}. */
public class CollectionServiceIT extends BaseCollectionEntityServiceIT<Collection> {

  private final CollectionService collectionService;
  private final CollectionDuplicatesService duplicatesService;
  private final InstitutionService institutionService;

  @Autowired
  public CollectionServiceIT(
      InstitutionService institutionService,
      CollectionService collectionService,
      DatasetService datasetService,
      NodeService nodeService,
      OrganizationService organizationService,
      InstallationService installationService,
      SimplePrincipalProvider principalProvider,
      CollectionDuplicatesService duplicatesService) {
    super(
        collectionService,
        datasetService,
        nodeService,
        organizationService,
        installationService,
        principalProvider,
        duplicatesService,
        Collection.class);
    this.collectionService = collectionService;
    this.duplicatesService = duplicatesService;
    this.institutionService = institutionService;
  }

  @Test
  public void listTest() {
    Collection collection1 = testData.newEntity();
    collection1.setCode("c1");
    collection1.setName("n1");
    collection1.setActive(true);
    collection1.setAccessionStatus(null);
    collection1.setPersonalCollection(true);
    collection1.setContentTypes(Arrays.asList("Archaeological", "Biological"));
    collection1.setPreservationTypes(Arrays.asList("SampleDried", "SampleCryopreserved"));
    Address address = new Address();
    address.setAddress("dummy address");
    address.setCity("city");
    address.setCountry(Country.DENMARK);
    collection1.setAddress(address);
    collection1.setAlternativeCodes(Collections.singletonList(new AlternativeCode("alt", "test")));
    collection1.setNumberSpecimens(100);
    collection1.setDisplayOnNHCPortal(false);
    UUID key1 = collectionService.create(collection1);

    Collection collection2 = testData.newEntity();
    collection2.setCode("c2");
    collection2.setName("n2");
    collection2.setActive(false);
    collection2.setContentTypes(Collections.singletonList("Archaeological"));
    collection2.setPreservationTypes(Collections.singletonList("SampleDried"));
    collection2.setAccessionStatus("Institutional");
    collection2.setPersonalCollection(false);
    collection2.setNumberSpecimens(200);
    Address address2 = new Address();
    address2.setAddress("dummy address2");
    address2.setCity("city2");
    address2.setCountry(Country.SPAIN);
    collection2.setAddress(address2);
    UUID key2 = collectionService.create(collection2);

    Collection collection3 = testData.newEntity();
    collection3.setCode("c3");
    collection3.setName("n3");
    MasterSourceMetadata sourceMetadata = new MasterSourceMetadata();
    sourceMetadata.setCreatedBy("test");
    sourceMetadata.setSourceId("test-123");
    sourceMetadata.setSource(Source.IH_IRN);
    collection3.setMasterSourceMetadata(sourceMetadata);
    UUID key3 = collectionService.create(collection3);
    collectionService.addMasterSourceMetadata(key3,sourceMetadata);

    // query param
    PagingResponse<CollectionView> response =
        collectionService.list(
            CollectionSearchRequest.builder().query("dummy").page(DEFAULT_PAGE).build());
    assertEquals(3, response.getResults().size());

    response = collectionService.list(CollectionSearchRequest.builder().source(Source.IH_IRN).sourceId("test-123").build());
    assertEquals(1,response.getResults().size());

    // empty queries are ignored and return all elements
    response =
        collectionService.list(
            CollectionSearchRequest.builder().query("").page(DEFAULT_PAGE).build());
    assertEquals(3, response.getResults().size());

    response =
        collectionService.list(
            CollectionSearchRequest.builder().query("city").page(DEFAULT_PAGE).build());
    assertEquals(1, response.getResults().size());
    assertEquals(key1, response.getResults().get(0).getCollection().getKey());

    response =
        collectionService.list(
            CollectionSearchRequest.builder().query("city2").page(DEFAULT_PAGE).build());
    assertEquals(1, response.getResults().size());
    assertEquals(key2, response.getResults().get(0).getCollection().getKey());

    assertEquals(
        3,
        collectionService
            .list(CollectionSearchRequest.builder().query("c").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        2,
        collectionService
            .list(CollectionSearchRequest.builder().query("dum add").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        0,
        collectionService
            .list(CollectionSearchRequest.builder().query("<").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        0,
        collectionService
            .list(CollectionSearchRequest.builder().query("\"<\"").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        3,
        collectionService
            .list(CollectionSearchRequest.builder().page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        3,
        collectionService
            .list(CollectionSearchRequest.builder().query("  ").page(DEFAULT_PAGE).build())
            .getResults()
            .size());

    // code and name params
    assertEquals(
        1,
        collectionService
            .list(CollectionSearchRequest.builder().code("c1").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        1,
        collectionService
            .list(CollectionSearchRequest.builder().name("n2").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        1,
        collectionService
            .list(
                CollectionSearchRequest.builder().code("c1").name("n1").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        0,
        collectionService
            .list(
                CollectionSearchRequest.builder().code("c2").name("n1").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        2,
        collectionService
            .list(CollectionSearchRequest.builder().active(true).page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        2,
        collectionService
            .list(
                CollectionSearchRequest.builder()
                    .accessionStatus("Institutional")
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());
    assertEquals(
        0,
        collectionService
            .list(
                CollectionSearchRequest.builder()
                    .accessionStatus("Project")
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());
    assertEquals(
        2,
        collectionService
            .list(
                CollectionSearchRequest.builder()
                    .contentTypes(Collections.singletonList("Archaeological"))
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());
    assertEquals(
        2,
        collectionService
            .list(
                CollectionSearchRequest.builder()
                    .contentTypes(Arrays.asList("Archaeological", "Biological"))
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());
    assertEquals(
        2,
        collectionService
            .list(
                CollectionSearchRequest.builder()
                    .preservationTypes(Collections.singletonList("SampleDried"))
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());
    assertEquals(
        1,
        collectionService
            .list(
                CollectionSearchRequest.builder()
                    .personalCollection(true)
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());

    // alternative code
    assertEquals(
        1,
        collectionService
            .list(
                CollectionSearchRequest.builder().alternativeCode("alt").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        0,
        collectionService
            .list(
                CollectionSearchRequest.builder().alternativeCode("foo").page(DEFAULT_PAGE).build())
            .getResults()
            .size());

    // country
    List<CollectionView> results =
        collectionService
            .list(
                CollectionSearchRequest.builder()
                    .country(Collections.singletonList(Country.SPAIN))
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults();
    assertEquals(1, results.size());
    assertEquals(key2, results.get(0).getCollection().getKey());
    assertEquals(
        0,
        collectionService
            .list(
                CollectionSearchRequest.builder()
                    .country(Collections.singletonList(Country.AFGHANISTAN))
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());
    assertEquals(
        2,
        collectionService
            .list(
                CollectionSearchRequest.builder()
                    .country(Arrays.asList(Country.SPAIN, Country.DENMARK))
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());
    assertEquals(
        2,
        collectionService
            .list(
                CollectionSearchRequest.builder()
                    .gbifRegion(Collections.singletonList(GbifRegion.EUROPE))
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());
    assertEquals(
        0,
        collectionService
            .list(
                CollectionSearchRequest.builder()
                    .country(Collections.singletonList(Country.SPAIN))
                    .gbifRegion(Collections.singletonList(GbifRegion.ASIA))
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());

    // city
    results =
        collectionService
            .list(CollectionSearchRequest.builder().city("city2").page(DEFAULT_PAGE).build())
            .getResults();
    assertEquals(1, results.size());
    assertEquals(key2, results.get(0).getCollection().getKey());
    assertEquals(
        0,
        collectionService
            .list(CollectionSearchRequest.builder().city("foo").page(DEFAULT_PAGE).build())
            .getResults()
            .size());

    // update address
    collection2 = collectionService.get(key2);
    assertNotNull(collection2.getAddress());
    collection2.getAddress().setCity("city3");
    collectionService.update(collection2);
    assertEquals(
        1,
        collectionService
            .list(CollectionSearchRequest.builder().query("city3").page(DEFAULT_PAGE).build())
            .getResults()
            .size());

    assertEquals(
        2,
        collectionService
            .list(
                CollectionSearchRequest.builder()
                    .displayOnNHCPortal(true)
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());

    // test numberSpecimens
    assertEquals(
        1,
        collectionService
            .list(
                CollectionSearchRequest.builder().numberSpecimens("100").page(DEFAULT_PAGE).build())
            .getResults()
            .size());

    assertEquals(
        0,
        collectionService
            .list(
                CollectionSearchRequest.builder().numberSpecimens("98").page(DEFAULT_PAGE).build())
            .getResults()
            .size());

    assertEquals(
        1,
        collectionService
            .list(
                CollectionSearchRequest.builder()
                    .numberSpecimens("* , 100")
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());

    assertEquals(
        2,
        collectionService
            .list(
                CollectionSearchRequest.builder()
                    .numberSpecimens("97,300")
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());

    assertEquals(
        collection1.getKey(),
        collectionService
            .list(
                CollectionSearchRequest.builder()
                    .sortBy(CollectionsSortField.NUMBER_SPECIMENS)
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .get(0)
            .getCollection()
            .getKey());

    assertEquals(
        collection2.getKey(),
        collectionService
            .list(
                CollectionSearchRequest.builder()
                    .sortBy(CollectionsSortField.NUMBER_SPECIMENS)
                    .sortOrder(SortOrder.DESC)
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .get(0)
            .getCollection()
            .getKey());

    collectionService.delete(key2);
    assertEquals(
        0,
        collectionService
            .list(CollectionSearchRequest.builder().query("city3").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
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

    Collection collection1 = testData.newEntity();
    collection1.setInstitutionKey(institutionKey1);
    collectionService.create(collection1);

    Collection collection2 = testData.newEntity();
    collection2.setInstitutionKey(institutionKey1);
    collectionService.create(collection2);

    Collection collection3 = testData.newEntity();
    collection3.setInstitutionKey(institutionKey2);
    collectionService.create(collection3);

    PagingResponse<CollectionView> response =
        collectionService.list(
            CollectionSearchRequest.builder()
                .institution(institutionKey1)
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(2, response.getResults().size());

    response =
        collectionService.list(
            CollectionSearchRequest.builder()
                .institution(institutionKey2)
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(1, response.getResults().size());

    response =
        collectionService.list(
            CollectionSearchRequest.builder()
                .institution(UUID.randomUUID())
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(0, response.getResults().size());

    response =
        collectionService.list(
            CollectionSearchRequest.builder()
                .institutionKeys(Collections.singletonList(institutionKey1))
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(2, response.getResults().size());

    response =
        collectionService.list(
            CollectionSearchRequest.builder()
                .institutionKeys(Arrays.asList(institutionKey1, institutionKey2))
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(3, response.getResults().size());

    response =
        collectionService.list(
            CollectionSearchRequest.builder()
                .query(collection1.getCode())
                .institutionKeys(Arrays.asList(institutionKey1, institutionKey2))
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(1, response.getResults().size());
  }

  @Test
  public void listAndGetAsLatimerCoreTest() {
    Collection collection1 = testData.newEntity();
    collection1.setCode("c1");
    collection1.setName("n1");
    collection1.setActive(true);
    collection1.setAccessionStatus(null);
    collection1.setPersonalCollection(true);
    collection1.setContentTypes(Arrays.asList("Archaeological", "Biological"));
    collection1.setPreservationTypes(Arrays.asList("SampleDried", "SampleFluidPreserved"));
    Address address = new Address();
    address.setAddress("dummy address");
    address.setCity("city");
    address.setCountry(Country.DENMARK);
    collection1.setAddress(address);
    collection1.setAlternativeCodes(Collections.singletonList(new AlternativeCode("alt", "test")));
    collection1.setNumberSpecimens(100);
    collection1.setDisplayOnNHCPortal(false);
    collection1.getApiUrls().add(URI.create("http://aa.com"));
    UUID key1 = collectionService.create(collection1);

    Collection collection2 = testData.newEntity();
    collection2.setCode("c2");
    collection2.setName("n2");
    collection2.setActive(false);
    collection2.setContentTypes(Collections.singletonList("Archaeological"));
    collection2.setPreservationTypes(Collections.singletonList("SampleDried"));
    collection2.setAccessionStatus("Institutional");
    collection2.setPersonalCollection(false);
    collection2.setNumberSpecimens(200);
    Address address2 = new Address();
    address2.setAddress("dummy address2");
    address2.setCity("city2");
    address2.setCountry(Country.SPAIN);
    collection2.setAddress(address2);
    UUID key2 = collectionService.create(collection2);

    PagingResponse<ObjectGroup> objectGroups =
        collectionService.listAsLatimerCore(CollectionSearchRequest.builder().build());
    assertEquals(2, objectGroups.getResults().size());

    ObjectGroup objectGroup = collectionService.getAsLatimerCore(key1);
    assertEquals(collection1.getName(), objectGroup.getCollectionName());
    assertTrue(
        objectGroup.getIdentifier().stream()
            .anyMatch(
                i ->
                    collection1.getCode().equals(i.getIdentifierValue())
                        && i.getIdentifierType()
                            .equals(LatimerCoreConverter.IdentifierTypes.COLLECTION_CODE)));
    assertEquals(
        objectGroup.getAddress().get(0).getAddressCountry(), collection1.getAddress().getCountry());
    assertTrue(
        objectGroup.getReference().stream()
            .anyMatch(
                r ->
                    r.getResourceIRI().equals(collection1.getApiUrls().get(0))
                        && r.getReferenceType().equals(LatimerCoreConverter.References.API)
                        && r.getReferenceName()
                            .equals(LatimerCoreConverter.References.COLLECTION_API)));
  }

  @Test
  public void createAndUpdateLatimerCoreTest() {
    ObjectGroup objectGroup = new ObjectGroup();
    objectGroup.setCollectionName("coll name");

    org.gbif.api.model.collections.latimercore.Address address =
        new org.gbif.api.model.collections.latimercore.Address();
    address.setStreetAddress("street");
    address.setAddressCountry(Country.SPAIN);
    address.setAddressType(LatimerCoreConverter.PHYSICAL);
    objectGroup.getAddress().add(address);

    org.gbif.api.model.collections.latimercore.Address mailingAddress =
        new org.gbif.api.model.collections.latimercore.Address();
    mailingAddress.setStreetAddress("st.");
    mailingAddress.setAddressCountry(Country.DENMARK);
    mailingAddress.setAddressType(LatimerCoreConverter.MAILING);
    objectGroup.getAddress().add(mailingAddress);

    org.gbif.api.model.collections.latimercore.Identifier identifier =
        new org.gbif.api.model.collections.latimercore.Identifier();
    identifier.setIdentifierType(LatimerCoreConverter.IdentifierTypes.COLLECTION_CODE);
    identifier.setIdentifierValue("C1");
    objectGroup.getIdentifier().add(identifier);

    ContactDetail contactDetail = new ContactDetail();
    contactDetail.setContactDetailCategory(LatimerCoreConverter.EMAIL);
    contactDetail.setContactDetailValue("aa@aa.com");
    objectGroup.getContactDetail().add(contactDetail);

    MeasurementOrFact measurementOrFact = new MeasurementOrFact();
    measurementOrFact.setMeasurementType(
        LatimerCoreConverter.MeasurementOrFactTypes.NUMBER_SPECIMENS);
    measurementOrFact.setMeasurementValue("100");
    objectGroup.getMeasurementOrFact().add(measurementOrFact);

    PersonRole personRole = new PersonRole();
    Person person = new Person();
    personRole.getPerson().add(person);
    person.setFamilyName("fam");
    person.setGivenName("giv");
    objectGroup.getPersonRole().add(personRole);

    UUID key1 = collectionService.createFromLatimerCore(objectGroup);
    Collection collection = collectionService.get(key1);
    assertEquals(objectGroup.getCollectionName(), collection.getName());
    assertEquals(address.getAddressCountry(), collection.getAddress().getCountry());
    assertEquals(address.getStreetAddress(), collection.getAddress().getAddress());
    assertEquals(identifier.getIdentifierValue(), collection.getCode());
    assertEquals(contactDetail.getContactDetailValue(), collection.getEmail().get(0));
    assertEquals(
        measurementOrFact.getMeasurementValue(), collection.getNumberSpecimens().toString());
    assertEquals(1, collection.getContactPersons().size());
    assertEquals(person.getGivenName(), collection.getContactPersons().get(0).getFirstName());

    // the key is not set
    assertThrows(
        IllegalArgumentException.class, () -> collectionService.updateFromLatimerCore(objectGroup));

    ObjectGroup createdObjectGroup = collectionService.getAsLatimerCore(key1);

    org.gbif.api.model.collections.latimercore.Identifier keyId =
        new org.gbif.api.model.collections.latimercore.Identifier();
    keyId.setIdentifierType(LatimerCoreConverter.IdentifierTypes.COLLECTION_GRSCICOLL_KEY);
    keyId.setIdentifierValue(key1.toString());
    createdObjectGroup.getIdentifier().add(keyId);

    createdObjectGroup.setDescription("desc");

    Reference reference = new Reference();
    reference.setReferenceName(LatimerCoreConverter.References.COLLECTION_API);
    reference.setReferenceType(LatimerCoreConverter.References.API);
    reference.setResourceIRI(URI.create("http://aaa.com"));
    createdObjectGroup.getReference().add(reference);

    createdObjectGroup.getPersonRole().get(0).getPerson().get(0).setGivenName("giv11");

    PersonRole personRole2 = new PersonRole();
    Person person2 = new Person();
    personRole2.getPerson().add(person2);
    person2.setFamilyName("fam2");
    person2.setGivenName("giv2");
    createdObjectGroup.getPersonRole().add(personRole2);

    collectionService.updateFromLatimerCore(createdObjectGroup);
    Collection updatedCollection = collectionService.get(key1);

    assertEquals(createdObjectGroup.getCollectionName(), updatedCollection.getName());
    assertEquals(address.getAddressCountry(), updatedCollection.getAddress().getCountry());
    assertEquals(address.getStreetAddress(), updatedCollection.getAddress().getAddress());
    assertEquals(
        mailingAddress.getAddressCountry(), updatedCollection.getMailingAddress().getCountry());
    assertEquals(
        mailingAddress.getStreetAddress(), updatedCollection.getMailingAddress().getAddress());
    assertEquals(identifier.getIdentifierValue(), updatedCollection.getCode());
    assertEquals(contactDetail.getContactDetailValue(), updatedCollection.getEmail().get(0));
    assertEquals(
        measurementOrFact.getMeasurementValue(), updatedCollection.getNumberSpecimens().toString());
    assertEquals(createdObjectGroup.getDescription(), updatedCollection.getDescription());
    assertEquals(reference.getResourceIRI(), updatedCollection.getApiUrls().get(0));
    assertEquals(2, updatedCollection.getContactPersons().size());
    assertTrue(
        updatedCollection.getContactPersons().stream()
            .anyMatch(c -> "giv11".equals(c.getFirstName())));

    // delete contact
    ObjectGroup updatedObjectGroup = collectionService.getAsLatimerCore(key1);
    updatedObjectGroup.getPersonRole().remove(0);
    collectionService.updateFromLatimerCore(updatedObjectGroup);
    updatedCollection = collectionService.get(key1);
    assertEquals(1, updatedCollection.getContactPersons().size());
  }

  @Test
  public void listMultipleParamsTest() {
    // institutions
    Institution institution1 = new Institution();
    institution1.setCode("code1");
    institution1.setName("name1");
    UUID institutionKey1 = institutionService.create(institution1);

    Institution institution2 = new Institution();
    institution2.setCode("code2");
    institution2.setName("name2");
    UUID institutionKey2 = institutionService.create(institution2);

    Collection collection1 = testData.newEntity();
    collection1.setCode("code1");
    collection1.setInstitutionKey(institutionKey1);
    collectionService.create(collection1);

    Collection collection2 = testData.newEntity();
    collection2.setCode("code2");
    collection2.setInstitutionKey(institutionKey1);
    collectionService.create(collection2);

    Collection collection3 = testData.newEntity();
    collection3.setInstitutionKey(institutionKey2);
    collectionService.create(collection3);

    PagingResponse<CollectionView> response =
        collectionService.list(
            CollectionSearchRequest.builder()
                .query("code1")
                .institution(institutionKey1)
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(1, response.getResults().size());

    response =
        collectionService.list(
            CollectionSearchRequest.builder()
                .query("foo")
                .institution(institutionKey1)
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(0, response.getResults().size());

    response =
        collectionService.list(
            CollectionSearchRequest.builder()
                .query("code2")
                .institution(institutionKey2)
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(0, response.getResults().size());

    response =
        collectionService.list(
            CollectionSearchRequest.builder()
                .query("code2")
                .institution(institutionKey1)
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(1, response.getResults().size());

    // list by contacts
    Contact contact1 = new Contact();
    contact1.setFirstName("Name1");
    contact1.setEmail(Collections.singletonList("aa1@aa.com"));
    contact1.setTaxonomicExpertise(Arrays.asList("aves", "fungi"));

    UserId userId1 = new UserId(IdType.OTHER, "12345");
    UserId userId2 = new UserId(IdType.OTHER, "abcde");
    contact1.setUserIds(Arrays.asList(userId1, userId2));
    collectionService.addContactPerson(collection1.getKey(), contact1);

    response =
        collectionService.list(
            CollectionSearchRequest.builder()
                .query("Name1")
                .institution(institutionKey1)
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(1, response.getResults().size());

    response =
        collectionService.list(
            CollectionSearchRequest.builder()
                .query("abcde")
                .institution(institutionKey1)
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(1, response.getResults().size());

    response =
        collectionService.list(
            CollectionSearchRequest.builder()
                .query("aa1@aa.com")
                .institution(institutionKey1)
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(1, response.getResults().size());

    response =
        collectionService.list(
            CollectionSearchRequest.builder()
                .query("aves")
                .institution(institutionKey1)
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(1, response.getResults().size());
  }

  @Test
  public void testSuggest() {
    Collection collection1 = testData.newEntity();
    collection1.setCode("CC");
    collection1.setName("Collection name");
    collectionService.create(collection1);

    Collection collection2 = testData.newEntity();
    collection2.setCode("CC2");
    collection2.setName("Collection name2");
    collectionService.create(collection2);

    Collection collection3 = testData.newEntity();
    collection3.setCode("CC3");
    collection3.setName("Collection name3");
    UUID key3 = collectionService.create(collection3);
    collectionService.delete(key3);

    assertEquals(2, collectionService.suggest("collection").size());
    assertEquals(2, collectionService.suggest("CC").size());
    assertEquals(1, collectionService.suggest("CC2").size());
    assertEquals(1, collectionService.suggest("name2").size());
  }

  @Test
  public void listDeletedTest() {
    Collection collection1 = testData.newEntity();
    collection1.setCode("code1");
    collection1.setName("Collection name");
    UUID key1 = collectionService.create(collection1);

    Collection collection2 = testData.newEntity();
    collection2.setCode("code2");
    collection2.setName("Collection name2");
    UUID key2 = collectionService.create(collection2);

    assertEquals(0, collectionService.listDeleted(null).getResults().size());

    collectionService.delete(key1);
    assertEquals(1, collectionService.listDeleted(null).getResults().size());

    collectionService.delete(key2);
    assertEquals(2, collectionService.listDeleted(null).getResults().size());

    Collection collection3 = testData.newEntity();
    collection3.setCode("code3");
    collection3.setName("Collection name3");
    UUID key3 = collectionService.create(collection3);

    Collection collection4 = testData.newEntity();
    collection4.setCode("code4");
    collection4.setName("Collection name4");
    UUID key4 = collectionService.create(collection4);

    CollectionSearchRequest searchRequest = new CollectionSearchRequest();
    searchRequest.setReplacedBy(key4);
    assertEquals(0, collectionService.listDeleted(searchRequest).getResults().size());
    collectionService.replace(key3, key4);
    assertEquals(1, collectionService.listDeleted(searchRequest).getResults().size());
  }

  @Test
  public void listWithoutParametersTest() {
    collectionService.create(testData.newEntity());
    collectionService.create(testData.newEntity());

    Collection collection3 = testData.newEntity();
    UUID key3 = collectionService.create(collection3);

    PagingResponse<CollectionView> response =
        collectionService.list(CollectionSearchRequest.builder().page(DEFAULT_PAGE).build());
    assertEquals(3, response.getResults().size());

    collectionService.delete(key3);

    response = collectionService.list(CollectionSearchRequest.builder().page(DEFAULT_PAGE).build());
    assertEquals(2, response.getResults().size());

    response =
        collectionService.list(
            CollectionSearchRequest.builder().page(new PagingRequest(0L, 1)).build());
    assertEquals(1, response.getResults().size());

    response =
        collectionService.list(
            CollectionSearchRequest.builder().page(new PagingRequest(0L, 0)).build());
    assertEquals(0, response.getResults().size());
  }

  @Test
  public void createCollectionWithoutCodeTest() {
    Collection c = testData.newEntity();
    c.setCode(null);
    assertThrows(ValidationException.class, () -> collectionService.create(c));
  }

  @Test
  public void updateCollectionWithoutCodeTest() {
    Collection c = testData.newEntity();
    UUID key = collectionService.create(c);

    Collection created = collectionService.get(key);
    created.setCode(null);
    assertThrows(IllegalArgumentException.class, () -> collectionService.update(created));
  }

  @Test
  public void updateAndReplaceTest() {
    Collection c = testData.newEntity();
    UUID key = collectionService.create(c);

    Collection created = collectionService.get(key);
    created.setReplacedBy(UUID.randomUUID());
    assertThrows(IllegalArgumentException.class, () -> collectionService.update(created));
  }

  @Test
  public void possibleDuplicatesTest() {
    testDuplicatesCommonCases();

    DuplicatesSearchParams params = new DuplicatesSearchParams();
    params.setSameInstitutionKey(true);
    params.setSameCode(true);

    DuplicatesResult result = duplicatesService.findPossibleDuplicates(params);
    assertEquals(1, result.getDuplicates().size());
    assertEquals(2, result.getDuplicates().get(0).size());

    Set<UUID> keysFound =
        result.getDuplicates().get(0).stream()
            .map(Duplicate::getInstitutionKey)
            .collect(Collectors.toSet());
    params.setInInstitutions(new ArrayList<>(keysFound));
    result = duplicatesService.findPossibleDuplicates(params);
    assertEquals(1, result.getDuplicates().size());
    assertEquals(2, result.getDuplicates().get(0).size());

    params.setInInstitutions(null);
    params.setNotInInstitutions(new ArrayList<>(keysFound));
    result = duplicatesService.findPossibleDuplicates(params);
    assertEquals(0, result.getDuplicates().size());
  }

  @Test
  public void invalidEmailsTest() {
    Collection collection = new Collection();
    collection.setCode("cc");
    collection.setName("n1");
    collection.setEmail(Collections.singletonList("asfs"));

    assertThrows(ConstraintViolationException.class, () -> collectionService.create(collection));

    collection.setEmail(Collections.singletonList("aa@aa.com"));
    UUID key = collectionService.create(collection);
    Collection collectionCreated = collectionService.get(key);

    collectionCreated.getEmail().add("asfs");
    assertThrows(
        ConstraintViolationException.class, () -> collectionService.update(collectionCreated));
  }

  @Test
  public void createFromDatasetTest() {
    Dataset dataset = createDataset();

    org.gbif.api.model.registry.Contact datasetContact = new org.gbif.api.model.registry.Contact();
    datasetContact.setFirstName("firstName");
    datasetContact.setLastName("lastName");
    datasetService.addContact(dataset.getKey(), datasetContact);

    org.gbif.api.model.registry.Contact datasetContact2 = new org.gbif.api.model.registry.Contact();
    datasetContact2.setFirstName("c2");
    datasetContact2.setType(ContactType.METADATA_AUTHOR);
    datasetService.addContact(dataset.getKey(), datasetContact2);

    String collectionCode = "CODE";
    UUID collectionKey = collectionService.createFromDataset(dataset.getKey(), collectionCode);
    Collection collection = collectionService.get(collectionKey);

    assertEquals(collectionCode, collection.getCode());
    assertEquals(MasterSourceType.GBIF_REGISTRY, collection.getMasterSource());

    assertEquals(Source.DATASET, collection.getMasterSourceMetadata().getSource());
    assertEquals(dataset.getKey().toString(), collection.getMasterSourceMetadata().getSourceId());

    assertTrue(
        collection.getIdentifiers().stream()
            .anyMatch(
                i ->
                    i.getType() == IdentifierType.DOI
                        && i.getIdentifier().equals(dataset.getDoi().getDoiName())));

    assertEquals(1, collection.getContactPersons().size());
  }

  @Test
  public void lockMasterSourceFieldsTest() {
    int numberSpecimensOriginal = 23;
    String descriptionOriginal = "description";
    String taxonomicCoverageOriginal = "orig taxon cov";

    Collection collection = new Collection();
    collection.setCode("code");
    collection.setName("name");
    collection.setDescription(descriptionOriginal);
    collection.setNumberSpecimens(numberSpecimensOriginal);
    collection.setTaxonomicCoverage(taxonomicCoverageOriginal);

    Address mailingAddress = new Address();
    mailingAddress.setAddress("safsf");
    mailingAddress.setCountry(Country.AFGHANISTAN);
    collection.setMailingAddress(mailingAddress);

    UUID collectionKey = collectionService.create(collection);

    Contact myContact = new Contact();
    myContact.setFirstName("myContact");
    int contactKey = collectionService.addContactPerson(collectionKey, myContact);

    int metadataKey =
        collectionService.addMasterSourceMetadata(
            collectionKey, new MasterSourceMetadata(Source.IH_IRN, "123"));

    collection.setNumberSpecimens(12);
    collection.setTaxonomicCoverage("aaa");
    collection.setDescription("dsgd");

    Address address = new Address();
    address.setAddress("safsf");
    address.setCountry(Country.AFGHANISTAN);
    collection.setAddress(address);

    collectionService.update(collection);

    Collection updatedCollection = collectionService.get(collectionKey);

    assertEquals(numberSpecimensOriginal, updatedCollection.getNumberSpecimens());
    assertNotEquals(descriptionOriginal, updatedCollection.getDescription());
    assertEquals(taxonomicCoverageOriginal, updatedCollection.getTaxonomicCoverage());
    assertNotNull(updatedCollection.getMailingAddress());
    assertNull(updatedCollection.getAddress());

    Contact contact = new Contact();
    contact.setFirstName("sfs");
    assertThrows(
        IllegalArgumentException.class,
        () -> collectionService.addContactPerson(collectionKey, contact));

    assertThrows(
        IllegalArgumentException.class,
        () -> collectionService.updateContactPerson(collectionKey, myContact));

    assertThrows(
        IllegalArgumentException.class,
        () -> collectionService.removeContactPerson(collectionKey, contactKey));

    assertDoesNotThrow(
        () ->
            collectionService.addIdentifier(
                collectionKey, new Identifier(IdentifierType.UNKNOWN, "sadf")));

    assertThrows(IllegalArgumentException.class, () -> collectionService.delete(collectionKey));

    // change the master source to GRSCICOLL
    collectionService.deleteMasterSourceMetadata(collectionKey);
    updatedCollection = collectionService.get(collectionKey);
    assertEquals(MasterSourceType.GRSCICOLL, updatedCollection.getMasterSource());

    assertDoesNotThrow(() -> collectionService.addContactPerson(collectionKey, contact));
    assertDoesNotThrow(() -> collectionService.updateContactPerson(collectionKey, myContact));
    assertDoesNotThrow(() -> collectionService.removeContactPerson(collectionKey, contactKey));

    // change the master source to GBIF_REGISTRY
    Dataset dataset = createDataset();
    metadataKey =
        collectionService.addMasterSourceMetadata(
            collectionKey, new MasterSourceMetadata(Source.DATASET, dataset.getKey().toString()));
    updatedCollection = collectionService.get(collectionKey);
    assertEquals(MasterSourceType.GBIF_REGISTRY, updatedCollection.getMasterSource());

    String currentDescription = updatedCollection.getDescription();
    updatedCollection.setDescription("another one");
    collectionService.update(updatedCollection);
    updatedCollection = collectionService.get(collectionKey);
    // the description must remain the same
    assertEquals(currentDescription, updatedCollection.getDescription());
    assertTrue(updatedCollection.getContactPersons().isEmpty());

    // delete the master source
    collectionService.deleteMasterSourceMetadata(collectionKey);
    assertDoesNotThrow(() -> collectionService.delete(collectionKey));
  }

  @Test
  public void vocabConceptsTest() {
    Collection collection1 = testData.newEntity();
    collection1.setCode("c1");
    collection1.setName("n1");
    collection1.setContentTypes(Collections.singletonList("foo"));
    assertThrows(IllegalArgumentException.class, () -> collectionService.create(collection1));

    collection1.setContentTypes(Arrays.asList("Archaeological", "C14"));
    UUID key = collectionService.create(collection1);
    Collection created = collectionService.get(key);

    assertEquals(2, created.getContentTypes().size());
    assertTrue(created.getContentTypes().contains("Archaeological"));
    assertTrue(created.getContentTypes().contains("C14"));

    created.getContentTypes().add("foo");
    assertThrows(IllegalArgumentException.class, () -> collectionService.update(created));
  }
}
