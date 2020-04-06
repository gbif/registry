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
package org.gbif.registry.persistence.mapper.collections;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.vocabulary.collections.AccessionStatus;
import org.gbif.api.vocabulary.collections.PreservationType;
import org.gbif.registry.DatabaseInitializer;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SpringBootTest(classes = {RegistryIntegrationTestsConfiguration.class})
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@Transactional // this rollbacks the transactions
public class CollectionMapperIT {

  @Autowired public CollectionMapper collectionMapper;

  @Autowired public AddressMapper addressMapper;

  @ClassRule public static DatabaseInitializer databaseInitializer = new DatabaseInitializer();

  private static final BiFunction<Integer, Long, Pageable> PAGE =
      (limit, offset) ->
          new Pageable() {
            @Override
            public int getLimit() {
              return limit;
            }

            @Override
            public long getOffset() {
              return offset;
            }
          };

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
    assertEquals(2, collectionMapper.list(null, null, null, null, null, page).size());

    page = PAGE.apply(5, 0L);
    assertEquals(3, collectionMapper.list(null, null, null, null, null, page).size());
    assertEquals(1, collectionMapper.list(null, null, null, "c1", null, page).size());
    assertEquals(1, collectionMapper.list(null, null, null, null, "n2", page).size());
    assertEquals(1, collectionMapper.list(null, null, null, "c3", "n3", page).size());
    assertEquals(0, collectionMapper.list(null, null, null, "c1", "n3", page).size());
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

    Collection col2 = new Collection();
    col2.setKey(UUID.randomUUID());
    col2.setCode("c2");
    col2.setName("n1");
    col2.setCreatedBy("test");
    col2.setModifiedBy("test");

    collectionMapper.create(col1);
    collectionMapper.create(col2);

    Pageable pageable = PAGE.apply(5, 0L);

    List<Collection> cols = collectionMapper.list(null, null, "c1 n1", null, null, pageable);
    assertEquals(1, cols.size());
    assertEquals("c1", cols.get(0).getCode());
    assertEquals("n1", cols.get(0).getName());

    cols = collectionMapper.list(null, null, "c2 c1", null, null, pageable);
    assertEquals(0, cols.size());

    cols = collectionMapper.list(null, null, "c3", null, null, pageable);
    assertEquals(0, cols.size());

    cols = collectionMapper.list(null, null, "n1", null, null, pageable);
    assertEquals(2, cols.size());

    cols = collectionMapper.list(null, null, "dummy address fo ", null, null, pageable);
    assertEquals(1, cols.size());
  }

  @Test
  public void searchWithWeightsTest() {
    Collection col1 = new Collection();
    col1.setKey(UUID.randomUUID());
    col1.setCode("longcodetotest1");
    col1.setName("n1");
    col1.setCreatedBy("test");
    col1.setModifiedBy("test");
    col1.setDescription("longcodetotest2");
    collectionMapper.create(col1);

    Collection col2 = new Collection();
    col2.setKey(UUID.randomUUID());
    col2.setCode("longcodetotest2");
    col2.setName("n2");
    col2.setCreatedBy("test");
    col2.setModifiedBy("test");
    col2.setDescription("longcodetotest1");
    collectionMapper.create(col2);

    Pageable pageable = PAGE.apply(2, 0L);
    List<Collection> cols =
        collectionMapper.list(null, null, "longcodetotest1", null, null, pageable);
    assertEquals(2, cols.size());

    pageable = PAGE.apply(1, 0L);
    cols = collectionMapper.list(null, null, "longcodetotest1", null, null, pageable);
    assertEquals(1, cols.size());
    assertEquals(col1.getKey(), cols.get(0).getKey());

    cols = collectionMapper.list(null, null, "longcodetotest2", null, null, pageable);
    assertEquals(1, cols.size());
    assertEquals(col2.getKey(), cols.get(0).getKey());
  }

  @Test
  public void countTest() {
    Collection col1 = new Collection();
    col1.setKey(UUID.randomUUID());
    col1.setCode("c1");
    col1.setName("n1");
    col1.setCreatedBy("test");
    col1.setModifiedBy("test");

    collectionMapper.create(col1);

    assertEquals(1, collectionMapper.count(null, null, null, null, null));
    assertEquals(0, collectionMapper.count(null, UUID.randomUUID(), null, null, null));
    assertEquals(1, collectionMapper.count(null, null, "c1", null, null));
    assertEquals(0, collectionMapper.count(null, null, null, "foo", null));
    assertEquals(1, collectionMapper.count(null, null, null, "c1", null));
    assertEquals(1, collectionMapper.count(null, null, null, null, "n1"));
    assertEquals(1, collectionMapper.count(null, null, null, "c1", "n1"));
    assertEquals(0, collectionMapper.count(null, null, null, "c2", "n1"));
  }
}
