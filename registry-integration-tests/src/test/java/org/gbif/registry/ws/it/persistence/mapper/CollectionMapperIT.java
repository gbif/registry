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
package org.gbif.registry.ws.it.persistence.mapper;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.AlternativeCode;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.MasterSourceMetadata;
import org.gbif.api.model.collections.UserId;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.collections.IdType;
import org.gbif.api.vocabulary.collections.MasterSourceType;
import org.gbif.api.vocabulary.collections.Source;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionContactMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.MasterSourceSyncMetadataMapper;
import org.gbif.registry.persistence.mapper.collections.dto.CollectionDto;
import org.gbif.registry.persistence.mapper.collections.params.CollectionListParams;
import org.gbif.registry.persistence.mapper.collections.params.RangeParam;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

import static org.gbif.registry.ws.it.fixtures.TestConstants.PAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CollectionMapperIT extends BaseItTest {

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule =
      new TestCaseDatabaseInitializer("collection");

  private CollectionMapper collectionMapper;
  private AddressMapper addressMapper;
  private MachineTagMapper machineTagMapper;
  private IdentifierMapper identifierMapper;
  private CollectionContactMapper contactMapper;
  private MasterSourceSyncMetadataMapper metadataMapper;

  @Autowired
  public CollectionMapperIT(
      CollectionMapper collectionMapper,
      AddressMapper addressMapper,
      MachineTagMapper machineTagMapper,
      IdentifierMapper identifierMapper,
      CollectionContactMapper contactMapper,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer,
      MasterSourceSyncMetadataMapper metadataMapper) {
    super(principalProvider, esServer);
    this.collectionMapper = collectionMapper;
    this.addressMapper = addressMapper;
    this.machineTagMapper = machineTagMapper;
    this.identifierMapper = identifierMapper;
    this.contactMapper = contactMapper;
    this.metadataMapper = metadataMapper;
  }

  @Test
  public void crudTest() {
    UUID key = UUID.randomUUID();

    assertNull(collectionMapper.get(key));

    Collection collection = new Collection();
    collection.setKey(key);
    collection.setAccessionStatus("Institutional");
    collection.setCode("CODE");
    collection.setName("NAME");
    collection.setCreatedBy("test");
    collection.setModifiedBy("test");
    collection.setEmail(Collections.singletonList("test@test.com"));
    collection.setPhone(Collections.singletonList("1234"));
    collection.setNumberSpecimens(12);
    collection.setTaxonomicCoverage("taxonomic coverage");
    collection.setGeographicCoverage("geography");
    collection.setNotes("notes for testing");
    collection.setIncorporatedCollections(Arrays.asList("col1", "col2"));
    collection.setAlternativeCodes(
        Collections.singletonList(new AlternativeCode("CODE2", "another code")));
    collection.setDivision("division");
    collection.setDepartment("department");
    collection.setDisplayOnNHCPortal(true);
    collection.setFeaturedImageUrl(URI.create("http://test.com"));
    collection.setFeaturedImageLicense(License.CC0_1_0);
    collection.setTemporalCoverage("temporal coverage");
    collection.setFeaturedImageAttribution("dummy image attribution");

    List<String> preservationTypes = new ArrayList<>();
    preservationTypes.add("StorageControlledAtmosphere");
    preservationTypes.add("SampleCryopreserved");
    collection.setPreservationTypes(preservationTypes);

    Address address = new Address();
    address.setAddress("dummy address");
    addressMapper.create(address);
    assertNotNull(address.getKey());

    collection.setAddress(address);

    collectionMapper.create(collection);

    Collection collectionStored = collectionMapper.get(key);
    assertTrue(collection.lenientEquals(collectionStored));

    // assert address
    assertNotNull(collectionStored.getAddress());
    assertNotNull(collectionStored.getAddress().getKey());
    assertEquals("dummy address", collectionStored.getAddress().getAddress());

    // update entity
    collection.setDescription("dummy description");
    preservationTypes.add("SampleDried");
    collection.setPreservationTypes(preservationTypes);
    collectionMapper.update(collection);
    collectionStored = collectionMapper.get(key);
    assertTrue(collection.lenientEquals(collectionStored));

    // assert address
    assertNotNull(collectionStored.getAddress());
    assertEquals("dummy address", collectionStored.getAddress().getAddress());

    // delete address
    collection.setAddress(null);
    collectionMapper.update(collection);
    collectionStored = collectionMapper.get(key);
    assertNull(collectionStored.getAddress());

    // delete entity
    collectionMapper.delete(key);
    collectionStored = collectionMapper.get(key);
    assertNotNull(collectionStored.getDeleted());
  }

  @Test
  public void listTest() {
    Collection col1 = new Collection();
    col1.setKey(UUID.randomUUID());
    col1.setCode("c1");
    col1.setName("n1");
    col1.setNumberSpecimens(10);
    col1.setCreatedBy("test");
    col1.setModifiedBy("test");
    col1.setMasterSource(MasterSourceType.IH);

    Address addressCol1 = new Address();
    addressCol1.setCountry(Country.DENMARK);
    addressCol1.setCity("Copenhagen");
    addressMapper.create(addressCol1);
    col1.setAddress(addressCol1);

    Address mailAddressCol1 = new Address();
    mailAddressCol1.setCountry(Country.DENMARK);
    mailAddressCol1.setCity("Odense");
    addressMapper.create(mailAddressCol1);
    col1.setMailingAddress(mailAddressCol1);

    collectionMapper.create(col1);

    MachineTag mt = new MachineTag("ns", "test", "foo");
    mt.setCreatedBy("test");
    machineTagMapper.createMachineTag(mt);
    collectionMapper.addMachineTag(col1.getKey(), mt.getKey());

    Collection col2 = new Collection();
    col2.setKey(UUID.randomUUID());
    col2.setCode("c2");
    col2.setName("n2");
    col2.setNumberSpecimens(110);
    col2.setCreatedBy("test");
    col2.setModifiedBy("test");
    col2.setMasterSource(MasterSourceType.GBIF_REGISTRY);
    collectionMapper.create(col2);

    Identifier identifier = new Identifier(IdentifierType.IH_IRN, "test_id");
    identifier.setCreatedBy("test");
    identifierMapper.createIdentifier(identifier);
    collectionMapper.addCollectionIdentifier(
        col2.getKey(), identifier.getKey(), identifier.isPrimary());

    Collection col3 = new Collection();
    col3.setKey(UUID.randomUUID());
    col3.setCode("c3");
    col3.setName("n3");
    col3.setCreatedBy("test");
    col3.setModifiedBy("test");
    collectionMapper.create(col3);

    Collection col4 = new Collection();
    col4.setKey(UUID.randomUUID());
    col4.setCode("c4");
    col4.setName("name of fourth collection");
    col4.setCreatedBy("test");
    col4.setModifiedBy("test");
    collectionMapper.create(col4);

    Collection col5 = new Collection();
    col5.setKey(UUID.randomUUID());
    col5.setCode("c5");
    col5.setName("name of fifth collection");
    col5.setCreatedBy("test");
    col5.setModifiedBy("test");
    collectionMapper.create(col5);

    MasterSourceMetadata masterSourceMetadata = new MasterSourceMetadata();
    masterSourceMetadata.setSource(Source.IH_IRN);
    masterSourceMetadata.setKey(123456);
    masterSourceMetadata.setSourceId("test-123");
    masterSourceMetadata.setCreatedBy("test");
    metadataMapper.create(masterSourceMetadata);
    collectionMapper.addMasterSourceMetadata(
        col5.getKey(), masterSourceMetadata.getKey(), MasterSourceType.GRSCICOLL);

    Pageable page = PAGE.apply(2, 0L);
    List<CollectionDto> dtos =
        collectionMapper.list(CollectionListParams.builder().page(page).build());
    assertEquals(2, dtos.size());

    page = PAGE.apply(5, 0L);
    assertSearch(
        CollectionListParams.builder()
            .sourceId(asList("test-123"))
            .source(asList(Source.IH_IRN))
            .build(),
        1,
        col5.getKey());
    assertSearch(
        CollectionListParams.builder()
            .numberSpecimens(
                Arrays.asList(new RangeParam<>(0, 20, null), new RangeParam<>(null, null, 2)))
            .page(page)
            .build(),
        1);
    assertSearch(
        CollectionListParams.builder()
            .numberSpecimens(
                Arrays.asList(new RangeParam<>(0, 20, null), new RangeParam<>(null, null, 110)))
            .page(page)
            .build(),
        2);
    assertSearch(
        CollectionListParams.builder()
            .numberSpecimens(
                Arrays.asList(new RangeParam<>(0, 20, null), new RangeParam<>(40, 140, null)))
            .page(page)
            .build(),
        2);
    assertSearch(CollectionListParams.builder().page(page).build(), 5);
    assertSearch(CollectionListParams.builder().code(asList("c1")).page(page).build(), 1);
    assertSearch(CollectionListParams.builder().code(asList("C1")).page(page).build(), 1);
    assertSearch(CollectionListParams.builder().name(asList("n2")).page(page).build(), 1);
    assertSearch(
        CollectionListParams.builder().code(asList("c3")).name(asList("n3")).page(page).build(), 1);
    assertSearch(
        CollectionListParams.builder().code(asList("c1")).name(asList("n3")).page(page).build(), 0);
    assertSearch(
        CollectionListParams.builder()
            .fuzzyName(asList("nime of fourth collection"))
            .page(page)
            .build(),
        1);
    assertSearch(
        CollectionListParams.builder().query("nime of fourth collection").page(page).build(), 0);
    assertSearch(
        CollectionListParams.builder()
            .countries(Collections.singletonList(Country.DENMARK))
            .page(page)
            .build(),
        1);
    assertSearch(
        CollectionListParams.builder()
            .countries(Collections.singletonList(Country.SPAIN))
            .page(page)
            .build(),
        0);
    assertSearch(CollectionListParams.builder().city(asList("Odense")).page(page).build(), 0);
    assertSearch(
        CollectionListParams.builder()
            .city(asList("Copenhagen"))
            .countries(Collections.singletonList(Country.DENMARK))
            .page(page)
            .build(),
        1);
    assertSearch(
        CollectionListParams.builder()
            .city(asList("CPH"))
            .countries(Collections.singletonList(Country.DENMARK))
            .page(page)
            .build(),
        0);

    // machine tags
    assertSearch(
        CollectionListParams.builder().machineTagNamespace(asList("dummy")).page(page).build(), 0);
    assertSearch(
        CollectionListParams.builder().machineTagName(asList(mt.getName())).page(page).build(),
        1,
        col1.getKey());

    assertSearch(
        CollectionListParams.builder().machineTagName(asList(mt.getName())).page(page).build(),
        1,
        col1.getKey());

    assertSearch(
        CollectionListParams.builder().machineTagValue(asList(mt.getValue())).page(page).build(),
        1,
        col1.getKey());

    assertSearch(
        CollectionListParams.builder()
            .machineTagName(asList(mt.getName()))
            .machineTagName(asList(mt.getName()))
            .machineTagValue(asList(mt.getValue()))
            .page(page)
            .build(),
        1,
        col1.getKey());

    // identifiers
    assertSearch(CollectionListParams.builder().identifier(asList("dummy")).page(page).build(), 0);
    assertSearch(
        CollectionListParams.builder()
            .machineTagName(asList(mt.getName()))
            .machineTagName(asList(mt.getName()))
            .machineTagValue(asList(mt.getValue()))
            .page(page)
            .build(),
        1,
        col1.getKey());

    assertSearch(
        CollectionListParams.builder()
            .identifierType(asList(identifier.getType()))
            .page(page)
            .build(),
        1,
        col2.getKey());

    assertSearch(
        CollectionListParams.builder()
            .identifier(asList(identifier.getIdentifier()))
            .page(page)
            .build(),
        1,
        col2.getKey());

    assertSearch(
        CollectionListParams.builder()
            .identifierType(asList(identifier.getType()))
            .identifier(asList(identifier.getIdentifier()))
            .page(page)
            .build(),
        1,
        col2.getKey());

    assertSearch(
        CollectionListParams.builder()
            .masterSourceType(asList(MasterSourceType.IH))
            .page(page)
            .build(),
        1,
        col1.getKey());

    assertSearch(
        CollectionListParams.builder()
            .masterSourceType(asList(MasterSourceType.GBIF_REGISTRY))
            .page(page)
            .build(),
        1,
        col2.getKey());

    assertSearch(
        CollectionListParams.builder()
            .masterSourceType(asList(MasterSourceType.GRSCICOLL))
            .page(page)
            .build(),
        1);
  }

  @Test
  public void searchTest() {
    Collection col1 = new Collection();
    col1.setKey(UUID.randomUUID());
    col1.setCode("c1");
    col1.setName("n1");
    col1.setTaxonomicCoverage("Insecta|Lepidoptera|Coleoptera|Hymenoptera");
    col1.setCreatedBy("test");
    col1.setModifiedBy("test");

    Address address = new Address();
    address.setAddress("dummy address foo");
    addressMapper.create(address);
    col1.setAddress(address);

    collectionMapper.create(col1);

    Collection col2 = new Collection();
    col2.setKey(UUID.randomUUID());
    col2.setCode("c2");
    col2.setName("n1");
    col2.setCreatedBy("test");
    col2.setModifiedBy("test");
    collectionMapper.create(col2);

    Pageable page = PAGE.apply(5, 0L);
    assertSearch(
        CollectionListParams.builder().query("c1 n1").page(page).build(), 1, col1.getKey());

    assertSearch(CollectionListParams.builder().query("c2 c1").page(page).build(), 0);
    assertSearch(CollectionListParams.builder().query("c3").page(page).build(), 0);
    assertSearch(CollectionListParams.builder().query("n1").page(page).build(), 2);
    assertSearch(CollectionListParams.builder().query("insecta").page(page).build(), 1);
    assertSearch(CollectionListParams.builder().query("Hymenoptera").page(page).build(), 1);

    assertSearch(
        CollectionListParams.builder().query("dummy address fo ").page(page).build(),
        1,
        col1.getKey());

    Contact contact1 = new Contact();
    contact1.setFirstName("Name1");
    contact1.setLastName("Surname1");
    contact1.setEmail(Collections.singletonList("aa1@aa.com"));
    contact1.setTaxonomicExpertise(Arrays.asList("aves", "fungi"));
    contact1.setCreatedBy("test");
    contact1.setModifiedBy("test");

    UserId userId1 = new UserId(IdType.OTHER, "12345");
    UserId userId2 = new UserId(IdType.OTHER, "abcde");
    contact1.setUserIds(Arrays.asList(userId1, userId2));

    contactMapper.createContact(contact1);

    contact1 = contactMapper.getContact(contact1.getKey());
    assertNotNull(contact1.getCreated());
    assertNotNull(contact1.getModified());

    collectionMapper.addContactPerson(col1.getKey(), contact1.getKey());
    assertSearch(CollectionListParams.builder().query("Name1").page(page).build(), 1);
    assertSearch(CollectionListParams.builder().query("Name0").page(page).build(), 0);
    assertSearch(CollectionListParams.builder().query("Surname1").page(page).build(), 1);
    assertSearch(CollectionListParams.builder().query("aa1@aa.com").page(page).build(), 1);
    assertSearch(CollectionListParams.builder().query("aves").page(page).build(), 1);
    assertSearch(CollectionListParams.builder().query("12345").page(page).build(), 1);
    assertSearch(CollectionListParams.builder().query("abcde").page(page).build(), 1);
  }

  @Test
  public void alternativeCodesTest() {
    Collection coll1 = new Collection();
    coll1.setKey(UUID.randomUUID());
    coll1.setCode("c1");
    coll1.setName("n1");
    coll1.setCreatedBy("test");
    coll1.setModifiedBy("test");
    coll1.setAlternativeCodes(Collections.singletonList(new AlternativeCode("c2", "test")));
    collectionMapper.create(coll1);

    Collection coll2 = new Collection();
    coll2.setKey(UUID.randomUUID());
    coll2.setCode("c2");
    coll2.setName("n2");
    coll2.setCreatedBy("test");
    coll2.setModifiedBy("test");
    coll2.setAlternativeCodes(Collections.singletonList(new AlternativeCode("c1", "test")));
    collectionMapper.create(coll2);

    Pageable page = PAGE.apply(1, 0L);
    CollectionListParams params = CollectionListParams.builder().query("c1").page(page).build();
    List<CollectionDto> dtos = collectionMapper.list(params);
    long count = collectionMapper.count(params);
    assertEquals(1, dtos.size());
    assertEquals(coll1.getKey(), dtos.get(0).getCollection().getKey());
    assertEquals(2, count);

    page = PAGE.apply(2, 0L);
    assertSearch(CollectionListParams.builder().query("c1").page(page).build(), 2);

    page = PAGE.apply(1, 0L);
    params = CollectionListParams.builder().query("c2").page(page).build();
    dtos = collectionMapper.list(params);
    count = collectionMapper.count(params);
    assertEquals(1, dtos.size());
    assertEquals(coll2.getKey(), dtos.get(0).getCollection().getKey());
    assertEquals(2, count);

    page = PAGE.apply(2, 0L);
    assertSearch(CollectionListParams.builder().query("c2").page(page).build(), 2);

    assertSearch(
        CollectionListParams.builder().alternativeCode(asList("c1")).page(page).build(),
        1,
        coll2.getKey());
  }

  private List<CollectionDto> assertSearch(CollectionListParams params, int expected) {
    List<CollectionDto> dtos = collectionMapper.list(params);
    long count = collectionMapper.count(params);
    assertEquals(expected, count);
    assertEquals(dtos.size(), count);
    return dtos;
  }

  private void assertSearch(CollectionListParams params, int expected, UUID expectedKey) {
    List<CollectionDto> dtos = assertSearch(params, expected);
    assertEquals(expectedKey, dtos.get(0).getCollection().getKey());
  }
}
