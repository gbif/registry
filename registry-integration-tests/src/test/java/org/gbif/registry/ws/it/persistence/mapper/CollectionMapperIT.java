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
package org.gbif.registry.ws.it.persistence.mapper;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.AlternativeCode;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.collections.AccessionStatus;
import org.gbif.api.vocabulary.collections.PreservationType;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.gbif.registry.ws.it.fixtures.TestConstants.PAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CollectionMapperIT extends BaseItTest {

  private CollectionMapper collectionMapper;
  private AddressMapper addressMapper;
  private MachineTagMapper machineTagMapper;
  private IdentifierMapper identifierMapper;

  @Autowired
  public CollectionMapperIT(
      CollectionMapper collectionMapper,
      AddressMapper addressMapper,
      MachineTagMapper machineTagMapper,
      IdentifierMapper identifierMapper,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer) {
    super(principalProvider, esServer);
    this.collectionMapper = collectionMapper;
    this.addressMapper = addressMapper;
    this.machineTagMapper = machineTagMapper;
    this.identifierMapper = identifierMapper;
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

    Collection col2 = new Collection();
    col2.setKey(UUID.randomUUID());
    col2.setCode("c2");
    col2.setName("n2");
    col2.setCreatedBy("test");
    col2.setModifiedBy("test");

    Collection col3 = new Collection();
    col3.setKey(UUID.randomUUID());
    col3.setCode("c3");
    col3.setName("n3");
    col3.setCreatedBy("test");
    col3.setModifiedBy("test");

    collectionMapper.create(col1);
    collectionMapper.create(col2);
    collectionMapper.create(col3);

    Pageable page = PAGE.apply(2, 0L);
    assertEquals(
        2,
        collectionMapper
            .list(null, null, null, null, null, null, null, null, null, null, null, page)
            .size());

    page = PAGE.apply(5, 0L);
    assertEquals(
        3,
        collectionMapper
            .list(null, null, null, null, null, null, null, null, null, null, null, page)
            .size());
    assertEquals(
        1,
        collectionMapper
            .list(null, null, null, "c1", null, null, null, null, null, null, null, page)
            .size());
    assertEquals(
        1,
        collectionMapper
            .list(null, null, null, null, "n2", null, null, null, null, null, null, page)
            .size());
    assertEquals(
        1,
        collectionMapper
            .list(null, null, null, "c3", "n3", null, null, null, null, null, null, page)
            .size());
    assertEquals(
        0,
        collectionMapper
            .list(null, null, null, "c1", "n3", null, null, null, null, null, null, page)
            .size());
  }

  @Test
  public void searchTest() {
    Collection col1 = new Collection();
    col1.setKey(UUID.randomUUID());
    col1.setCode("c1");
    col1.setName("n1");
    col1.setCreatedBy("test");
    col1.setModifiedBy("test");

    Address address = new Address();
    address.setAddress("dummy address foo");
    addressMapper.create(address);
    col1.setAddress(address);

    collectionMapper.create(col1);

    MachineTag mt = new MachineTag("ns", "test", "foo");
    mt.setCreatedBy("test");
    machineTagMapper.createMachineTag(mt);
    collectionMapper.addMachineTag(col1.getKey(), mt.getKey());

    Collection col2 = new Collection();
    col2.setKey(UUID.randomUUID());
    col2.setCode("c2");
    col2.setName("n1");
    col2.setCreatedBy("test");
    col2.setModifiedBy("test");

    collectionMapper.create(col2);

    Identifier identifier = new Identifier(IdentifierType.IH_IRN, "test_id");
    identifier.setCreatedBy("test");
    identifierMapper.createIdentifier(identifier);
    collectionMapper.addIdentifier(col2.getKey(), identifier.getKey());

    Pageable pageable = PAGE.apply(5, 0L);

    List<Collection> cols =
        collectionMapper.list(
            null, null, "c1 n1", null, null, null, null, null, null, null, null, pageable);
    long count =
        collectionMapper.count(null, null, "c1 n1", null, null, null, null, null, null, null, null);
    assertEquals(1, cols.size());
    assertEquals(count, cols.size());
    assertEquals("c1", cols.get(0).getCode());
    assertEquals("n1", cols.get(0).getName());

    cols =
        collectionMapper.list(
            null, null, "c2 c1", null, null, null, null, null, null, null, null, pageable);
    count =
        collectionMapper.count(null, null, "c2 c1", null, null, null, null, null, null, null, null);
    assertEquals(0, cols.size());
    assertEquals(count, cols.size());

    cols =
        collectionMapper.list(
            null, null, "c3", null, null, null, null, null, null, null, null, pageable);
    count =
        collectionMapper.count(null, null, "c3", null, null, null, null, null, null, null, null);
    assertEquals(0, cols.size());
    assertEquals(count, cols.size());

    cols =
        collectionMapper.list(
            null, null, "n1", null, null, null, null, null, null, null, null, pageable);
    count =
        collectionMapper.count(null, null, "n1", null, null, null, null, null, null, null, null);
    assertEquals(2, cols.size());
    assertEquals(count, cols.size());

    cols =
        collectionMapper.list(
            null,
            null,
            "dummy address fo ",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            pageable);
    count =
        collectionMapper.count(
            null, null, "dummy address fo ", null, null, null, null, null, null, null, null);
    assertEquals(1, cols.size());
    assertEquals(count, cols.size());

    // machine tags
    cols =
        collectionMapper.list(
            null, null, null, null, null, null, "dummy", null, null, null, null, pageable);
    count =
        collectionMapper.count(null, null, null, null, null, null, "dummy", null, null, null, null);
    assertEquals(0, cols.size());
    assertEquals(count, cols.size());

    cols =
        collectionMapper.list(
            null,
            null,
            null,
            null,
            null,
            null,
            mt.getNamespace(),
            null,
            null,
            null,
            null,
            pageable);
    count =
        collectionMapper.count(
            null, null, null, null, null, null, mt.getNamespace(), null, null, null, null);
    assertEquals(1, cols.size());
    assertEquals(count, cols.size());
    assertEquals(col1.getKey(), cols.get(0).getKey());

    cols =
        collectionMapper.list(
            null, null, null, null, null, null, null, mt.getName(), null, null, null, pageable);
    count =
        collectionMapper.count(
            null, null, null, null, null, null, null, mt.getName(), null, null, null);
    assertEquals(1, cols.size());
    assertEquals(count, cols.size());
    assertEquals(col1.getKey(), cols.get(0).getKey());

    cols =
        collectionMapper.list(
            null, null, null, null, null, null, null, null, mt.getValue(), null, null, pageable);
    count =
        collectionMapper.count(
            null, null, null, null, null, null, null, null, mt.getValue(), null, null);
    assertEquals(1, cols.size());
    assertEquals(count, cols.size());
    assertEquals(col1.getKey(), cols.get(0).getKey());

    cols =
        collectionMapper.list(
            null,
            null,
            null,
            null,
            null,
            null,
            mt.getNamespace(),
            mt.getName(),
            mt.getValue(),
            null,
            null,
            pageable);
    count =
        collectionMapper.count(
            null,
            null,
            null,
            null,
            null,
            null,
            mt.getNamespace(),
            mt.getName(),
            mt.getValue(),
            null,
            null);
    assertEquals(1, cols.size());
    assertEquals(count, cols.size());
    assertEquals(col1.getKey(), cols.get(0).getKey());

    // identifiers
    cols =
        collectionMapper.list(
            null, null, null, null, null, null, null, null, null, null, "dummy", pageable);
    count =
        collectionMapper.count(null, null, null, null, null, null, null, null, null, null, "dummy");
    assertEquals(0, cols.size());
    assertEquals(count, cols.size());

    cols =
        collectionMapper.list(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            identifier.getType(),
            null,
            pageable);
    count =
        collectionMapper.count(
            null, null, null, null, null, null, null, null, null, identifier.getType(), null);
    assertEquals(1, cols.size());
    assertEquals(count, cols.size());
    assertEquals(col2.getKey(), cols.get(0).getKey());

    cols =
        collectionMapper.list(
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
            identifier.getIdentifier(),
            pageable);
    count =
        collectionMapper.count(
            null, null, null, null, null, null, null, null, null, null, identifier.getIdentifier());
    assertEquals(1, cols.size());
    assertEquals(count, cols.size());
    assertEquals(col2.getKey(), cols.get(0).getKey());

    cols =
        collectionMapper.list(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            identifier.getType(),
            identifier.getIdentifier(),
            pageable);
    count =
        collectionMapper.count(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            identifier.getType(),
            identifier.getIdentifier());
    assertEquals(1, cols.size());
    assertEquals(count, cols.size());
    assertEquals(col2.getKey(), cols.get(0).getKey());
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

    Pageable pageable = PAGE.apply(1, 0L);
    List<Collection> collections =
        collectionMapper.list(
            null, null, "c1", null, null, null, null, null, null, null, null, pageable);
    long count =
        collectionMapper.count(null, null, "c1", null, null, null, null, null, null, null, null);
    assertEquals(1, collections.size());
    // it should return the one where the c1 is main code
    assertEquals(coll1.getKey(), collections.get(0).getKey());
    // there are 2 colls with c1
    assertEquals(2, count);

    collections =
        collectionMapper.list(
            null, null, "c2", null, null, null, null, null, null, null, null, pageable);
    count =
        collectionMapper.count(null, null, "c2", null, null, null, null, null, null, null, null);
    assertEquals(1, collections.size());
    // it should return the one where the c2 is main code
    assertEquals(coll2.getKey(), collections.get(0).getKey());
    // there are 2 colls with c2
    assertEquals(2, count);

    collections =
        collectionMapper.list(
            null, null, null, null, null, "c1", null, null, null, null, null, pageable);
    count =
        collectionMapper.count(null, null, null, null, null, "c1", null, null, null, null, null);
    assertEquals(1, collections.size());
    assertEquals(count, collections.size());
    assertEquals(coll2.getKey(), collections.get(0).getKey());
  }

  @Test
  public void countTest() {
    Collection col1 = new Collection();
    col1.setKey(UUID.randomUUID());
    col1.setCode("c1");
    col1.setName("n1");
    col1.setCreatedBy("test");
    col1.setModifiedBy("test");
    col1.setAlternativeCodes(Collections.singletonList(new AlternativeCode("cc1", "test")));

    collectionMapper.create(col1);

    assertEquals(
        1,
        collectionMapper.count(null, null, null, null, null, null, null, null, null, null, null));
    assertEquals(
        0,
        collectionMapper.count(
            null, UUID.randomUUID(), null, null, null, null, null, null, null, null, null));
    assertEquals(
        1,
        collectionMapper.count(null, null, "c1", null, null, null, null, null, null, null, null));
    assertEquals(
        0,
        collectionMapper.count(null, null, null, "foo", null, null, null, null, null, null, null));
    assertEquals(
        1,
        collectionMapper.count(null, null, null, "c1", null, null, null, null, null, null, null));
    assertEquals(
        1,
        collectionMapper.count(null, null, null, null, "n1", null, null, null, null, null, null));
    assertEquals(
        1,
        collectionMapper.count(null, null, null, "c1", "n1", null, null, null, null, null, null));
    assertEquals(
        0,
        collectionMapper.count(null, null, null, "c2", "n1", null, null, null, null, null, null));
    assertEquals(
        1,
        collectionMapper.count(null, null, null, null, null, "cc1", null, null, null, null, null));
  }
}
