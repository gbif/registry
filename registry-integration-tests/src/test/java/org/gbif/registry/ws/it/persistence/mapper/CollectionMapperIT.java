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
import org.gbif.api.model.collections.IdType;
import org.gbif.api.model.collections.UserId;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.collections.AccessionStatus;
import org.gbif.api.vocabulary.collections.PreservationType;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionContactMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.dto.CollectionDto;
import org.gbif.registry.persistence.mapper.collections.params.CollectionSearchParams;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

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

  @Autowired
  public CollectionMapperIT(
      CollectionMapper collectionMapper,
      AddressMapper addressMapper,
      MachineTagMapper machineTagMapper,
      IdentifierMapper identifierMapper,
      CollectionContactMapper contactMapper,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer) {
    super(principalProvider, esServer);
    this.collectionMapper = collectionMapper;
    this.addressMapper = addressMapper;
    this.machineTagMapper = machineTagMapper;
    this.identifierMapper = identifierMapper;
    this.contactMapper = contactMapper;
  }

  @Test
  public void crudTest() {
    UUID key = UUID.randomUUID();

    assertNull(collectionMapper.get(key));

    Collection collection = new Collection();
    collection.setKey(key);
    collection.setAccessionStatus(AccessionStatus.INSTITUTIONAL);
    collection.setCode("CODE");
    collection.setName("NAME");
    collection.setCreatedBy("test");
    collection.setModifiedBy("test");
    collection.setEmail(Collections.singletonList("test@test.com"));
    collection.setPhone(Collections.singletonList("1234"));
    collection.setIndexHerbariorumRecord(true);
    collection.setNumberSpecimens(12);
    collection.setTaxonomicCoverage("taxonomic coverage");
    collection.setGeography("geography");
    collection.setNotes("notes for testing");
    collection.setIncorporatedCollections(Arrays.asList("col1", "col2"));
    collection.setImportantCollectors(Arrays.asList("collector 1", "collector 2"));
    collection.setCollectionSummary(Collections.singletonMap("key", 0));
    collection.setAlternativeCodes(
        Collections.singletonList(new AlternativeCode("CODE2", "another code")));

    List<PreservationType> preservationTypes = new ArrayList<>();
    preservationTypes.add(PreservationType.STORAGE_CONTROLLED_ATMOSPHERE);
    preservationTypes.add(PreservationType.SAMPLE_CRYOPRESERVED);
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
    preservationTypes.add(PreservationType.SAMPLE_DRIED);
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
    col1.setCreatedBy("test");
    col1.setModifiedBy("test");

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
    col2.setCreatedBy("test");
    col2.setModifiedBy("test");
    collectionMapper.create(col2);

    Identifier identifier = new Identifier(IdentifierType.IH_IRN, "test_id");
    identifier.setCreatedBy("test");
    identifierMapper.createIdentifier(identifier);
    collectionMapper.addIdentifier(col2.getKey(), identifier.getKey());

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

    Pageable page = PAGE.apply(2, 0L);
    List<CollectionDto> dtos =
        collectionMapper.list(CollectionSearchParams.builder().build(), page);
    assertEquals(2, dtos.size());

    page = PAGE.apply(5, 0L);
    assertSearch(CollectionSearchParams.builder().build(), page, 4);
    assertSearch(CollectionSearchParams.builder().code("c1").build(), page, 1);
    assertSearch(CollectionSearchParams.builder().code("C1").build(), page, 1);
    assertSearch(CollectionSearchParams.builder().name("n2").build(), page, 1);
    assertSearch(CollectionSearchParams.builder().code("c3").name("n3").build(), page, 1);
    assertSearch(CollectionSearchParams.builder().code("c1").name("n3").build(), page, 0);
    assertSearch(
        CollectionSearchParams.builder().fuzzyName("nime of fourth collection").build(), page, 1);
    assertSearch(
        CollectionSearchParams.builder().query("nime of fourth collection").build(), page, 0);
    assertSearch(CollectionSearchParams.builder().country(Country.DENMARK).build(), page, 1);
    assertSearch(CollectionSearchParams.builder().country(Country.SPAIN).build(), page, 0);
    assertSearch(CollectionSearchParams.builder().city("Odense").build(), page, 1);
    assertSearch(
        CollectionSearchParams.builder().city("Copenhagen").country(Country.DENMARK).build(),
        page,
        1);
    assertSearch(
        CollectionSearchParams.builder().city("CPH").country(Country.DENMARK).build(), page, 0);

    // machine tags
    assertSearch(CollectionSearchParams.builder().machineTagNamespace("dummy").build(), page, 0);
    assertSearch(
        CollectionSearchParams.builder().machineTagName(mt.getName()).build(),
        page,
        1,
        col1.getKey());

    assertSearch(
        CollectionSearchParams.builder().machineTagName(mt.getName()).build(),
        page,
        1,
        col1.getKey());

    assertSearch(
        CollectionSearchParams.builder().machineTagValue(mt.getValue()).build(),
        page,
        1,
        col1.getKey());

    assertSearch(
        CollectionSearchParams.builder()
            .machineTagName(mt.getName())
            .machineTagName(mt.getName())
            .machineTagValue(mt.getValue())
            .build(),
        page,
        1,
        col1.getKey());

    // identifiers
    assertSearch(CollectionSearchParams.builder().identifier("dummy").build(), page, 0);
    assertSearch(
        CollectionSearchParams.builder()
            .machineTagName(mt.getName())
            .machineTagName(mt.getName())
            .machineTagValue(mt.getValue())
            .build(),
        page,
        1,
        col1.getKey());

    assertSearch(
        CollectionSearchParams.builder().identifierType(identifier.getType()).build(),
        page,
        1,
        col2.getKey());

    assertSearch(
        CollectionSearchParams.builder().identifier(identifier.getIdentifier()).build(),
        page,
        1,
        col2.getKey());

    assertSearch(
        CollectionSearchParams.builder()
            .identifierType(identifier.getType())
            .identifier(identifier.getIdentifier())
            .build(),
        page,
        1,
        col2.getKey());
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
    assertSearch(CollectionSearchParams.builder().query("c1 n1").build(), page, 1, col1.getKey());

    assertSearch(CollectionSearchParams.builder().query("c2 c1").build(), page, 0);
    assertSearch(CollectionSearchParams.builder().query("c3").build(), page, 0);
    assertSearch(CollectionSearchParams.builder().query("n1").build(), page, 2);
    assertSearch(CollectionSearchParams.builder().query("insecta").build(), page, 1);
    assertSearch(CollectionSearchParams.builder().query("Hymenoptera").build(), page, 1);

    assertSearch(
        CollectionSearchParams.builder().query("dummy address fo ").build(),
        page,
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
    assertSearch(CollectionSearchParams.builder().query("Name1").build(), page, 1);
    assertSearch(CollectionSearchParams.builder().query("Name0").build(), page, 0);
    assertSearch(CollectionSearchParams.builder().query("Surname1").build(), page, 1);
    assertSearch(CollectionSearchParams.builder().query("aa1@aa.com").build(), page, 1);
    assertSearch(CollectionSearchParams.builder().query("aves").build(), page, 1);
    assertSearch(CollectionSearchParams.builder().query("12345").build(), page, 1);
    assertSearch(CollectionSearchParams.builder().query("abcde").build(), page, 1);
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
    CollectionSearchParams params = CollectionSearchParams.builder().query("c1").build();
    List<CollectionDto> dtos = collectionMapper.list(params, page);
    long count = collectionMapper.count(params);
    assertEquals(1, dtos.size());
    assertEquals(coll1.getKey(), dtos.get(0).getCollection().getKey());
    assertEquals(2, count);

    page = PAGE.apply(2, 0L);
    assertSearch(CollectionSearchParams.builder().query("c1").build(), page, 2);

    page = PAGE.apply(1, 0L);
    params = CollectionSearchParams.builder().query("c2").build();
    dtos = collectionMapper.list(params, page);
    count = collectionMapper.count(params);
    assertEquals(1, dtos.size());
    assertEquals(coll2.getKey(), dtos.get(0).getCollection().getKey());
    assertEquals(2, count);

    page = PAGE.apply(2, 0L);
    assertSearch(CollectionSearchParams.builder().query("c2").build(), page, 2);

    assertSearch(
        CollectionSearchParams.builder().alternativeCode("c1").build(), page, 1, coll2.getKey());
  }

  private List<CollectionDto> assertSearch(
      CollectionSearchParams params, Pageable page, int expected) {
    List<CollectionDto> dtos = collectionMapper.list(params, page);
    long count = collectionMapper.count(params);
    assertEquals(expected, count);
    assertEquals(dtos.size(), count);
    return dtos;
  }

  private void assertSearch(
      CollectionSearchParams params, Pageable page, int expected, UUID expectedKey) {
    List<CollectionDto> dtos = assertSearch(params, page, expected);
    assertEquals(expectedKey, dtos.get(0).getCollection().getKey());
  }
}
