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
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.PersonMapper;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.gbif.registry.ws.it.fixtures.TestConstants.PAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PersonMapperTest extends BaseItTest {

  private PersonMapper personMapper;
  private AddressMapper addressMapper;

  @Autowired
  public PersonMapperTest(
      PersonMapper personMapper,
      AddressMapper addressMapper,
      SimplePrincipalProvider principalProvider) {
    super(principalProvider);
    this.personMapper = personMapper;
    this.addressMapper = addressMapper;
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
}
