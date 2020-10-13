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
import org.gbif.api.model.collections.AlternativeCode;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.vocabulary.collections.Discipline;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.gbif.registry.ws.it.fixtures.TestConstants.PAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InstitutionMapperIT extends BaseItTest {

  private InstitutionMapper institutionMapper;
  private AddressMapper addressMapper;

  @Autowired
  public InstitutionMapperIT(
      InstitutionMapper institutionMapper,
      AddressMapper addressMapper,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer) {
    super(principalProvider, esServer);
    this.institutionMapper = institutionMapper;
    this.addressMapper = addressMapper;
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
    institution.setEmail(Collections.singletonList("test@test.com"));
    institution.setPhone(Collections.singletonList("1234"));
    institution.setAlternativeCodes(
        Collections.singletonList(new AlternativeCode("CODE2", "another code")));

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
    assertTrue(institution.lenientEquals(institutionStored));

    // assert address
    assertNotNull(institutionStored.getAddress());
    assertNotNull(institutionStored.getAddress().getKey());
    assertEquals("dummy address", institutionStored.getAddress().getAddress());

    // update entity
    institution.setDescription("Another dummy description");
    additionalNames.add("name 4");
    institution.setAdditionalNames(additionalNames);
    institutionMapper.update(institution);
    institutionStored = institutionMapper.get(key);
    assertTrue(institution.lenientEquals(institutionStored));

    // assert address
    assertNotNull(institutionStored.getAddress());
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

    Pageable page = PAGE.apply(5, 0L);
    assertEquals(
        2,
        institutionMapper
            .list(null, null, null, null, null, null, null, null, null, null, page)
            .size());
    assertEquals(
        1,
        institutionMapper
            .list(null, null, "i1", null, null, null, null, null, null, null, page)
            .size());
    assertEquals(
        1,
        institutionMapper
            .list(null, null, null, "n2", null, null, null, null, null, null, page)
            .size());
    assertEquals(
        1,
        institutionMapper
            .list(null, null, "i2", "n2", null, null, null, null, null, null, page)
            .size());
    assertEquals(
        0,
        institutionMapper
            .list(null, null, "i1", "n2", null, null, null, null, null, null, page)
            .size());
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
    address.setAddress("dummy address foo");
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

    List<Institution> cols =
        institutionMapper.list(
            "i1 n1", null, null, null, null, null, null, null, null, null, pageable);
    long count =
        institutionMapper.count("i1 n1", null, null, null, null, null, null, null, null, null);
    assertEquals(1, cols.size());
    assertEquals(count, cols.size());
    assertEquals("i1", cols.get(0).getCode());
    assertEquals("n1", cols.get(0).getName());

    cols =
        institutionMapper.list(
            "i2 i1", null, null, null, null, null, null, null, null, null, pageable);
    count = institutionMapper.count("i2 i1", null, null, null, null, null, null, null, null, null);
    assertEquals(0, cols.size());
    assertEquals(count, cols.size());

    cols =
        institutionMapper.list(
            "i3", null, null, null, null, null, null, null, null, null, pageable);
    count = institutionMapper.count("i3", null, null, null, null, null, null, null, null, null);
    assertEquals(0, cols.size());
    assertEquals(count, cols.size());

    cols =
        institutionMapper.list(
            "n1", null, null, null, null, null, null, null, null, null, pageable);
    count = institutionMapper.count("n1", null, null, null, null, null, null, null, null, null);
    assertEquals(2, cols.size());
    assertEquals(count, cols.size());

    cols =
        institutionMapper.list(
            "dummy address fo ", null, null, null, null, null, null, null, null, null, pageable);
    count =
        institutionMapper.count(
            "dummy address fo ", null, null, null, null, null, null, null, null, null);
    assertEquals(1, cols.size());
    assertEquals(count, cols.size());
  }

  @Test
  public void alternativeCodesTest() {
    Institution inst1 = new Institution();
    inst1.setKey(UUID.randomUUID());
    inst1.setCode("i1");
    inst1.setName("n1");
    inst1.setCreatedBy("test");
    inst1.setModifiedBy("test");
    inst1.setAlternativeCodes(Collections.singletonList(new AlternativeCode("i2", "test")));
    institutionMapper.create(inst1);

    Institution inst2 = new Institution();
    inst2.setKey(UUID.randomUUID());
    inst2.setCode("i2");
    inst2.setName("n2");
    inst2.setCreatedBy("test");
    inst2.setModifiedBy("test");
    inst2.setAlternativeCodes(Collections.singletonList(new AlternativeCode("i1", "test")));
    institutionMapper.create(inst2);

    Pageable pageable = PAGE.apply(1, 0L);
    List<Institution> institutions =
        institutionMapper.list(
            "i1", null, null, null, null, null, null, null, null, null, pageable);
    long count =
        institutionMapper.count("i1", null, null, null, null, null, null, null, null, null);
    assertEquals(1, institutions.size());
    // it should return the one where the i1 is main code
    assertEquals(inst1.getKey(), institutions.get(0).getKey());
    // there are 2 insts with i1
    assertEquals(2, count);

    institutions =
        institutionMapper.list(
            "i2", null, null, null, null, null, null, null, null, null, pageable);
    count = institutionMapper.count("i2", null, null, null, null, null, null, null, null, null);
    assertEquals(1, institutions.size());
    // it should return the one where the i2 is main code
    assertEquals(inst2.getKey(), institutions.get(0).getKey());
    // there are 2 insts with i2
    assertEquals(2, count);

    institutions =
        institutionMapper.list(
            null, null, null, null, "i1", null, null, null, null, null, pageable);
    count = institutionMapper.count(null, null, null, null, "i1", null, null, null, null, null);
    assertEquals(1, institutions.size());
    assertEquals(count, institutions.size());
    assertEquals(inst2.getKey(), institutions.get(0).getKey());
  }

  @Test
  public void countTest() {
    Institution inst1 = new Institution();
    inst1.setKey(UUID.randomUUID());
    inst1.setCode("i1");
    inst1.setName("n1");
    inst1.setCreatedBy("test");
    inst1.setModifiedBy("test");
    inst1.setAlternativeCodes(Collections.singletonList(new AlternativeCode("ii1", "test")));

    Institution inst2 = new Institution();
    inst2.setKey(UUID.randomUUID());
    inst2.setCode("i2");
    inst2.setName("n2");
    inst2.setCreatedBy("test");
    inst2.setModifiedBy("test");

    institutionMapper.create(inst1);
    institutionMapper.create(inst2);

    assertEquals(
        2, institutionMapper.count(null, null, null, null, null, null, null, null, null, null));
    assertEquals(
        1, institutionMapper.count(null, null, "i1", null, null, null, null, null, null, null));
    assertEquals(
        1, institutionMapper.count(null, null, null, "n2", null, null, null, null, null, null));
    assertEquals(
        1, institutionMapper.count(null, null, "i2", "n2", null, null, null, null, null, null));
    assertEquals(
        0, institutionMapper.count(null, null, "i1", "n2", null, null, null, null, null, null));
    assertEquals(
        1, institutionMapper.count(null, null, null, null, "ii1", null, null, null, null, null));
  }
}
