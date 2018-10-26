package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.database.LiquibaseModules;
import org.gbif.registry.guice.RegistryTestModules;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.inject.Injector;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class InstitutionMapperTest {

  private InstitutionMapper institutionMapper;
  private AddressMapper addressMapper;

  @ClassRule
  public static LiquibaseInitializer liquibase = new LiquibaseInitializer(LiquibaseModules.database());

  @Rule
  public final DatabaseInitializer databaseRule = new DatabaseInitializer(LiquibaseModules.database());

  @Before
  public void setup() {
    Injector inj = RegistryTestModules.mybatis();
    institutionMapper = inj.getInstance(InstitutionMapper.class);
    addressMapper = inj.getInstance(AddressMapper.class);
  }

  @Test
  public void crudTest() {
    UUID key = UUID.randomUUID();

    assertNull(institutionMapper.get(key));

    Institution institution = new Institution();
    institution.setKey(key);
    institution.setCode("CODE");
    institution.setName("NAME");
    institution.setDescription("dummy description");
    institution.setCreatedBy("test");
    institution.setModifiedBy("test");
    institution.setActive(true);
    institution.setHomepage(URI.create("http://dummy.com"));

    List<String> additionalNames = new ArrayList<>();
    additionalNames.add("name2");
    additionalNames.add("name3");
    institution.setAdditionalNames(additionalNames);

    Address address = new Address();
    address.setAddress("dummy address");
    addressMapper.create(address);
    assertNotNull(address.getKey());

    institution.setAddress(address);

    institutionMapper.create(institution);

    Institution institutionStored = institutionMapper.get(key);

    assertEquals("CODE", institutionStored.getCode());
    assertEquals("NAME", institutionStored.getName());
    assertEquals("dummy description", institutionStored.getDescription());
    assertEquals("test", institutionStored.getCreatedBy());
    assertEquals(2, institutionStored.getAdditionalNames().size());
    assertEquals(URI.create("http://dummy.com"), institutionStored.getHomepage());
    assertTrue(institutionStored.isActive());

    // assert address
    assertNotNull(institutionStored.getAddress().getKey());
    assertEquals("dummy address", institutionStored.getAddress().getAddress());

    // update entity
    institution.setDescription("Another dummy description");
    additionalNames.add("name 4");
    institution.setAdditionalNames(additionalNames);
    institutionMapper.update(institution);
    institutionStored = institutionMapper.get(key);

    assertEquals("CODE", institutionStored.getCode());
    assertEquals("NAME", institutionStored.getName());
    assertEquals("Another dummy description", institutionStored.getDescription());
    assertEquals("test", institutionStored.getCreatedBy());
    assertEquals(3, institutionStored.getAdditionalNames().size());
    assertEquals(URI.create("http://dummy.com"), institutionStored.getHomepage());
    assertTrue(institutionStored.isActive());

    // assert address
    assertEquals("dummy address", institutionStored.getAddress().getAddress());

    // delete address
    institution.setAddress(null);
    institutionMapper.update(institution);
    institutionStored = institutionMapper.get(key);
    assertNull(institutionStored.getAddress());

    // delete entity
    institutionMapper.delete(key);
    institutionStored = institutionMapper.get(key);
    assertNotNull(institutionStored.getDeleted());
  }

  @Test
  public void listTest() {
    Institution inst1 = new Institution();
    inst1.setKey(UUID.randomUUID());
    inst1.setCode("i1");
    inst1.setName("n1");
    inst1.setCreatedBy("test");
    inst1.setModifiedBy("test");

    Institution inst2 = new Institution();
    inst2.setKey(UUID.randomUUID());
    inst2.setCode("i2");
    inst2.setName("n2");
    inst2.setCreatedBy("test");
    inst2.setModifiedBy("test");

    institutionMapper.create(inst1);
    institutionMapper.create(inst2);

    Pageable pageable = new Pageable() {
      @Override
      public int getLimit() {
        return 5;
      }

      @Override
      public long getOffset() {
        return 0;
      }
    };

    List<Institution> cols = institutionMapper.list(pageable);
    assertEquals(2, cols.size());
  }

  @Test
  public void searchTest() {
    Institution inst1 = new Institution();
    inst1.setKey(UUID.randomUUID());
    inst1.setCode("i1");
    inst1.setName("n1");
    inst1.setCreatedBy("test");
    inst1.setModifiedBy("test");

    Address address = new Address();
    address.setAddress("dummy address");
    addressMapper.create(address);

    inst1.setAddress(address);

    Institution inst2 = new Institution();
    inst2.setKey(UUID.randomUUID());
    inst2.setCode("i2");
    inst2.setName("n1");
    inst2.setCreatedBy("test");
    inst2.setModifiedBy("test");

    institutionMapper.create(inst1);
    institutionMapper.create(inst2);

    Pageable pageable = new Pageable() {
      @Override
      public int getLimit() {
        return 5;
      }

      @Override
      public long getOffset() {
        return 0;
      }
    };

    List<Institution> cols = institutionMapper.search("i1 n1", pageable);
    assertEquals(1, cols.size());
    assertEquals("i1", cols.get(0).getCode());
    assertEquals("n1", cols.get(0).getName());

    cols = institutionMapper.search("i2 i1", pageable);
    assertEquals(0, cols.size());

    cols = institutionMapper.search("i3", pageable);
    assertEquals(0, cols.size());

    cols = institutionMapper.search("n1", pageable);
    assertEquals(2, cols.size());

    cols = institutionMapper.search("dummy address", pageable);
    assertEquals(1, cols.size());
  }

  @Test
  public void countTest() {
    Institution inst1 = new Institution();
    inst1.setKey(UUID.randomUUID());
    inst1.setCode("i1");
    inst1.setName("n1");
    inst1.setCreatedBy("test");
    inst1.setModifiedBy("test");

    Institution inst2 = new Institution();
    inst2.setKey(UUID.randomUUID());
    inst2.setCode("i2");
    inst2.setName("n2");
    inst2.setCreatedBy("test");
    inst2.setModifiedBy("test");

    institutionMapper.create(inst1);
    institutionMapper.create(inst2);

    assertEquals(2, institutionMapper.count());
  }

}
