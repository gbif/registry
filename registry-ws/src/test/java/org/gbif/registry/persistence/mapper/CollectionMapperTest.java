package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.vocabulary.collections.AccessionStatus;
import org.gbif.api.vocabulary.collections.PreservationType;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.database.LiquibaseModules;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

import com.google.inject.Injector;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class CollectionMapperTest {

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

  private CollectionMapper collectionMapper;
  private AddressMapper addressMapper;

  @ClassRule
  public static LiquibaseInitializer liquibase =
      new LiquibaseInitializer(LiquibaseModules.database());

  @Rule
  public final DatabaseInitializer databaseRule =
      new DatabaseInitializer(LiquibaseModules.database());

  @Before
  public void setup() {
    Injector inj = RegistryTestModules.mybatis();
    collectionMapper = inj.getInstance(CollectionMapper.class);
    addressMapper = inj.getInstance(AddressMapper.class);
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

    assertEquals("CODE", collectionStored.getCode());
    assertEquals("NAME", collectionStored.getName());
    assertEquals(AccessionStatus.INSTITUTIONAL, collectionStored.getAccessionStatus());
    assertEquals(2, collectionStored.getPreservationTypes().size());
    assertTrue(collectionStored.getPreservationTypes().contains(PreservationType.SAMPLE_CRYOPRESERVED));
    assertEquals(1, collectionStored.getEmail().size());
    assertTrue(collectionStored.getEmail().contains("test@test.com"));
    assertEquals(1, collectionStored.getPhone().size());
    assertTrue(collectionStored.getPhone().contains("1234"));
    assertTrue(collectionStored.isIndexHerbariorumRecord());
    assertEquals(12, collectionStored.getNumberSpecimens());

    // assert address
    assertNotNull(collectionStored.getAddress().getKey());
    assertEquals("dummy address", collectionStored.getAddress().getAddress());

    // update entity
    collection.setDescription("dummy description");
    preservationTypes.add(PreservationType.SAMPLE_DRIED);
    collection.setPreservationTypes(preservationTypes);
    collectionMapper.update(collection);
    collectionStored = collectionMapper.get(key);

    assertEquals("CODE", collectionStored.getCode());
    assertEquals("NAME", collectionStored.getName());
    assertEquals("dummy description", collectionStored.getDescription());
    assertEquals(AccessionStatus.INSTITUTIONAL, collectionStored.getAccessionStatus());
    assertEquals(3, collectionStored.getPreservationTypes().size());

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

    List<Collection> cols = collectionMapper.list(null,null,"c1 n1", null, null, pageable);
    assertEquals(1, cols.size());
    assertEquals("c1", cols.get(0).getCode());
    assertEquals("n1", cols.get(0).getName());

    cols = collectionMapper.list(null,null,"c2 c1", null, null, pageable);
    assertEquals(0, cols.size());

    cols = collectionMapper.list(null,null,"c3", null, null, pageable);
    assertEquals(0, cols.size());

    cols = collectionMapper.list(null,null,"n1", null, null, pageable);
    assertEquals(2, cols.size());

    cols = collectionMapper.list(null,null,"dummy address fo ", null, null, pageable);
    assertEquals(1, cols.size());
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
    assertEquals(0, collectionMapper.count(null, null, null,"foo", null));
    assertEquals(1, collectionMapper.count(null, null, null,"c1", null));
    assertEquals(1, collectionMapper.count(null, null, null,null, "n1"));
    assertEquals(1, collectionMapper.count(null, null, null,"c1", "n1"));
    assertEquals(0, collectionMapper.count(null, null, null,"c2", "n1"));
  }
}
