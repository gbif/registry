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
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.registry.DatabaseInitializer;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;

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
public class PersonMapperIT {

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

  @Autowired private PersonMapper personMapper;
  @Autowired private AddressMapper addressMapper;

  @ClassRule public static DatabaseInitializer databaseInitializer = new DatabaseInitializer();

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
    assertTrue(person.lenientEquals(personStored));

    // assert address
    assertNotNull(personStored.getMailingAddress().getKey());
    assertEquals("dummy address", personStored.getMailingAddress().getAddress());

    person.setFirstName("FN2");
    person.setLastName(null);
    person.setPhone("12234");
    personMapper.update(person);
    personStored = personMapper.get(key);
    assertTrue(person.lenientEquals(personStored));

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

    List<Person> persons = personMapper.list(null, null, "FN1", pageable);
    assertEquals(1, persons.size());
    assertEquals("FN1", persons.get(0).getFirstName());

    persons = personMapper.list(null, null, "FN0", pageable);
    assertEquals(0, persons.size());

    persons = personMapper.list(null, null, "12345", pageable);
    assertEquals(2, persons.size());

    persons = personMapper.list(null, null, "dummy address f ", pageable);
    assertEquals(1, persons.size());
  }

  @Test
  public void searchWithWeightsTest() {
    Person p1 = new Person();
    p1.setKey(UUID.randomUUID());
    p1.setFirstName("FN1");
    p1.setCreatedBy("test");
    p1.setModifiedBy("test");
    p1.setPosition("FN2");
    personMapper.create(p1);

    Person p2 = new Person();
    p2.setKey(UUID.randomUUID());
    p2.setFirstName("FN2");
    p2.setCreatedBy("test");
    p2.setModifiedBy("test");
    p2.setPosition("FN1");
    personMapper.create(p2);

    Pageable pageable = PAGE.apply(2, 0L);
    List<Person> persons = personMapper.list(null, null, "FN1", pageable);
    assertEquals(2, persons.size());

    pageable = PAGE.apply(1, 0L);
    persons = personMapper.list(null, null, "FN1", pageable);
    assertEquals(1, persons.size());
    assertEquals(p1.getKey(), persons.get(0).getKey());

    persons = personMapper.list(null, null, "FN2", pageable);
    assertEquals(1, persons.size());
    assertEquals(p2.getKey(), persons.get(0).getKey());
  }
}
