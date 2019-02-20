package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.database.LiquibaseModules;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.PersonMapper;

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

public class PersonMapperTest {

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

  private PersonMapper personMapper;
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
    personMapper = inj.getInstance(PersonMapper.class);
    addressMapper = inj.getInstance(AddressMapper.class);
  }

  @Test
  public void crudTest() {
    UUID key = UUID.randomUUID();

    assertNull(personMapper.get(key));

    Person person = new Person();
    person.setKey(key);
    person.setFirstName("FN");
    person.setLastName("LN");
    person.setEmail("test@test.com");
    person.setCreatedBy("TEST");
    person.setModifiedBy("TEST");

    Address address = new Address();
    address.setAddress("dummy address");
    addressMapper.create(address);
    assertNotNull(address.getKey());

    person.setMailingAddress(address);

    personMapper.create(person);

    Person personStored = personMapper.get(key);

    assertEquals("FN", personStored.getFirstName());
    assertEquals("LN", personStored.getLastName());
    assertEquals("test@test.com", personStored.getEmail());

    // assert address
    assertNotNull(personStored.getMailingAddress().getKey());
    assertEquals("dummy address", personStored.getMailingAddress().getAddress());

    person.setFirstName("FN2");
    person.setLastName(null);
    person.setPhone("12234");
    personMapper.update(person);
    personStored = personMapper.get(key);

    assertEquals("FN2", personStored.getFirstName());
    assertNull(personStored.getLastName());
    assertEquals("12234", personStored.getPhone());
    assertEquals("test@test.com", personStored.getEmail());

    // assert address
    assertEquals("dummy address", personStored.getMailingAddress().getAddress());

    // delete address
    person.setMailingAddress(null);
    personMapper.update(person);
    personStored = personMapper.get(key);
    assertNull(personStored.getMailingAddress());

    // delete entity
    personMapper.delete(key);
    personStored = personMapper.get(key);
    assertNotNull(personStored.getDeleted());
  }

  @Test
  public void listTest() {
    Person p1 = new Person();
    p1.setKey(UUID.randomUUID());
    p1.setFirstName("FN1");
    p1.setCreatedBy("test");
    p1.setModifiedBy("test");

    Person p2 = new Person();
    p2.setKey(UUID.randomUUID());
    p2.setFirstName("FN2");
    p2.setCreatedBy("test");
    p2.setModifiedBy("test");

    personMapper.create(p1);
    personMapper.create(p2);

    List<Person> staffs = personMapper.list(null, null, null, PAGE.apply(5, 0L));
    assertEquals(2, staffs.size());
  }

  @Test
  public void searchTest() {
    Person p1 = new Person();
    p1.setKey(UUID.randomUUID());
    p1.setFirstName("FN1");
    p1.setFax("12345");
    p1.setCreatedBy("test");
    p1.setModifiedBy("test");

    Address address = new Address();
    address.setAddress("dummy address foo");
    addressMapper.create(address);

    p1.setMailingAddress(address);

    Person p2 = new Person();
    p2.setKey(UUID.randomUUID());
    p2.setFirstName("FN2");
    p2.setFax("12345");
    p2.setCreatedBy("test");
    p2.setModifiedBy("test");

    personMapper.create(p1);
    personMapper.create(p2);

    Pageable pageable = PAGE.apply(5, 0L);

    List<Person> persons = personMapper.list(null, null,"FN1", pageable);
    assertEquals(1, persons.size());
    assertEquals("FN1", persons.get(0).getFirstName());

    persons = personMapper.list(null, null,"FN0", pageable);
    assertEquals(0, persons.size());

    persons = personMapper.list(null, null,"12345", pageable);
    assertEquals(2, persons.size());

    persons = personMapper.list(null, null,"dummy address f", pageable);
    assertEquals(1, persons.size());
  }
}
