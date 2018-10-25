package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.vocabulary.AccessionStatus;
import org.gbif.api.model.collections.vocabulary.PreservationType;
import org.gbif.api.model.registry.Tag;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.database.LiquibaseModules;
import org.gbif.registry.guice.RegistryTestModules;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.google.inject.Injector;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CollectionMapperTest {

  private CollectionMapper collectionMapper;
  private AddressMapper addressMapper;

  @ClassRule
  public static LiquibaseInitializer liquibase = new LiquibaseInitializer(LiquibaseModules.database());

  @Rule
  public final DatabaseInitializer databaseRule = new DatabaseInitializer(LiquibaseModules.database());

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

    List<PreservationType> preservationTypes = new ArrayList<>();
    preservationTypes.add(PreservationType.STORAGE_CONTROLLED_ATMOSPHERE);
    preservationTypes.add(PreservationType.SAMPLE_CRYOPRESERVED);
    collection.setPreservationTypes(preservationTypes);

    Address address = new Address();
    address.setKey(1);
    address.setAddress("dummy address");
    addressMapper.create(address);

    collection.setAddress(address);

    collectionMapper.create(collection);

    Collection collectionStored = collectionMapper.get(key);

    assertEquals("CODE", collectionStored.getCode());
    assertEquals("NAME", collectionStored.getName());
    assertEquals(AccessionStatus.INSTITUTIONAL, collectionStored.getAccessionStatus());
    assertEquals(2, collectionStored.getPreservationTypes().size());

    // assert address
    assertEquals((Integer) 1, collectionStored.getAddress().getKey());
    assertEquals("dummy address", collectionStored.getAddress().getAddress());
  }

}
