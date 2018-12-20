package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.vocabulary.collections.Discipline;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.database.LiquibaseModules;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

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

  private InstitutionMapper institutionMapper;
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
    List<Discipline> disciplines = new ArrayList<>();
    disciplines.add(Discipline.AGRICULTURAL_ANIMAL_SCIENCE);
    institution.setDisciplines(disciplines);

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
    assertEquals(1, institutionStored.getDisciplines().size());
    assertTrue(institutionStored.getDisciplines().contains(Discipline.AGRICULTURAL_ANIMAL_SCIENCE));
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

    List<Institution> cols = institutionMapper.list(null, null, PAGE.apply(5, 0L));
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

    Pageable pageable = PAGE.apply(5, 0L);

    List<Institution> cols = institutionMapper.list("i1 n1", null, pageable);
    assertEquals(1, cols.size());
    assertEquals("i1", cols.get(0).getCode());
    assertEquals("n1", cols.get(0).getName());

    cols = institutionMapper.list("i2 i1", null, pageable);
    assertEquals(0, cols.size());

    cols = institutionMapper.list("i3", null, pageable);
    assertEquals(0, cols.size());

    cols = institutionMapper.list("n1", null, pageable);
    assertEquals(2, cols.size());

    cols = institutionMapper.list("dummy address", null, pageable);
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

    assertEquals(2, institutionMapper.count(null, null));
  }
}
