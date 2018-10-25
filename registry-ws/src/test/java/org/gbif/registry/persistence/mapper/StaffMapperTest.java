package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Staff;
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
  }

}
