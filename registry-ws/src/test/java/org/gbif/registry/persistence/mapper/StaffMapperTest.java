package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Staff;
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

public class StaffMapperTest {

  private StaffMapper staffMapper;
  private AddressMapper addressMapper;

  @ClassRule
  public static LiquibaseInitializer liquibase = new LiquibaseInitializer(LiquibaseModules.database());

  @Rule
  public final DatabaseInitializer databaseRule = new DatabaseInitializer(LiquibaseModules.database());

  @Before
  public void setup() {
    Injector inj = RegistryTestModules.mybatis();
    staffMapper = inj.getInstance(StaffMapper.class);
    addressMapper = inj.getInstance(AddressMapper.class);
  }

  @Test
  public void crudTest() {
    UUID key = UUID.randomUUID();

    assertNull(staffMapper.get(key));

    Staff staff = new Staff();
    staff.setKey(key);
    staff.setFirstName("FN");
    staff.setLastName("LN");
    staff.setEmail("test@test.com");
    staff.setCreatedBy("TEST");
    staff.setModifiedBy("TEST");

    Address address = new Address();
    address.setKey(1);
    address.setAddress("dummy address");
    addressMapper.create(address);

    staff.setMailingAddress(address);

    staffMapper.create(staff);

    Staff staffStored = staffMapper.get(key);

    assertEquals("FN", staffStored.getFirstName());
    assertEquals("LN", staffStored.getLastName());
    assertEquals("test@test.com", staffStored.getEmail());

    // assert address
    assertEquals((Integer) 1, staffStored.getMailingAddress().getKey());
    assertEquals("dummy address", staffStored.getMailingAddress().getAddress());

    staff.setFirstName("FN2");
    staff.setLastName(null);
    staff.setPhone("12234");
    staffMapper.update(staff);
    staffStored = staffMapper.get(key);

    assertEquals("FN2", staffStored.getFirstName());
    assertNull(staffStored.getLastName());
    assertEquals("12234", staffStored.getPhone());
    assertEquals("test@test.com", staffStored.getEmail());

    // assert address
    assertEquals((Integer) 1, staffStored.getMailingAddress().getKey());
    assertEquals("dummy address", staffStored.getMailingAddress().getAddress());

    // delete address
    staff.setMailingAddress(null);
    staffMapper.update(staff);
    staffStored = staffMapper.get(key);
    assertNull(staffStored.getMailingAddress());

    // delete entity
    staffMapper.delete(key);
    staffStored = staffMapper.get(key);
    assertNotNull(staffStored.getDeleted());
  }

  @Test
  public void listTest() {
    Staff s1 = new Staff();
    s1.setKey(UUID.randomUUID());
    s1.setFirstName("FN1");
    s1.setCreatedBy("test");
    s1.setModifiedBy("test");

    Staff s2 = new Staff();
    s2.setKey(UUID.randomUUID());
    s2.setFirstName("FN2");
    s2.setCreatedBy("test");
    s2.setModifiedBy("test");

    staffMapper.create(s1);
    staffMapper.create(s2);

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

    List<Staff> staffs = staffMapper.list(pageable);
    assertEquals(2, staffs.size());
  }

  @Test
  public void searchTest() {
    Staff s1 = new Staff();
    s1.setKey(UUID.randomUUID());
    s1.setFirstName("FN1");
    s1.setFax("12345");
    s1.setCreatedBy("test");
    s1.setModifiedBy("test");

    Address address = new Address();
    address.setKey(1);
    address.setAddress("dummy address");
    addressMapper.create(address);

    s1.setMailingAddress(address);

    Staff s2 = new Staff();
    s2.setKey(UUID.randomUUID());
    s2.setFirstName("FN2");
    s2.setFax("12345");
    s2.setCreatedBy("test");
    s2.setModifiedBy("test");

    staffMapper.create(s1);
    staffMapper.create(s2);

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

    List<Staff> staffs = staffMapper.search("FN1", pageable);
    assertEquals(1, staffs.size());
    assertEquals("FN1", staffs.get(0).getFirstName());

    staffs = staffMapper.search("FN0", pageable);
    assertEquals(0, staffs.size());

    staffs = staffMapper.search("12345", pageable);
    assertEquals(2, staffs.size());

    staffs = staffMapper.search("dummy address", pageable);
    assertEquals(1, staffs.size());
  }

}
