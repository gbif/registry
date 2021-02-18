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
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.collections.Discipline;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.persistence.mapper.collections.params.InstitutionSearchParams;
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
  private MachineTagMapper machineTagMapper;
  private IdentifierMapper identifierMapper;

  @Autowired
  public InstitutionMapperIT(
      InstitutionMapper institutionMapper,
      AddressMapper addressMapper,
      MachineTagMapper machineTagMapper,
      IdentifierMapper identifierMapper,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer) {
    super(principalProvider, esServer);
    this.institutionMapper = institutionMapper;
    this.addressMapper = addressMapper;
    this.machineTagMapper = machineTagMapper;
    this.identifierMapper = identifierMapper;
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

    Address addressInst1 = new Address();
    addressInst1.setCountry(Country.DENMARK);
    addressInst1.setCity("Copenhagen");
    addressMapper.create(addressInst1);
    inst1.setAddress(addressInst1);

    institutionMapper.create(inst1);

    MachineTag mt = new MachineTag("ns", "test", "foo");
    mt.setCreatedBy("test");
    machineTagMapper.createMachineTag(mt);
    institutionMapper.addMachineTag(inst1.getKey(), mt.getKey());

    Institution inst2 = new Institution();
    inst2.setKey(UUID.randomUUID());
    inst2.setCode("i2");
    inst2.setName("n2");
    inst2.setCreatedBy("test");
    inst2.setModifiedBy("test");
    institutionMapper.create(inst2);

    Identifier identifier = new Identifier(IdentifierType.IH_IRN, "test_id");
    identifier.setCreatedBy("test");
    identifierMapper.createIdentifier(identifier);
    institutionMapper.addIdentifier(inst2.getKey(), identifier.getKey());

    Institution inst3 = new Institution();
    inst3.setKey(UUID.randomUUID());
    inst3.setCode("i3");
    inst3.setName("Name of the third institution");
    inst3.setCreatedBy("test");
    inst3.setModifiedBy("test");
    institutionMapper.create(inst3);


    Pageable page = PAGE.apply(5, 0L);

    assertSearch(InstitutionSearchParams.builder().build(), page, 3);
    assertSearch(InstitutionSearchParams.builder().code("i1").build(), page, 1);
    assertSearch(InstitutionSearchParams.builder().name("n2").build(), page, 1);
    assertSearch(InstitutionSearchParams.builder().code("i2").name("n2").build(), page, 1);
    assertSearch(InstitutionSearchParams.builder().code("i1").name("n2").build(), page, 0);
    assertSearch(InstitutionSearchParams.builder().fuzzyName("nime of third institution").build(), page, 1);
    assertSearch(InstitutionSearchParams.builder().query("nime of third institution").build(), page, 0);
    assertSearch(InstitutionSearchParams.builder().country(Country.DENMARK).build(), page, 1);
    assertSearch(InstitutionSearchParams.builder().country(Country.SPAIN).build(), page, 0);
    assertSearch(
        InstitutionSearchParams.builder().city("Copenhagen").country(Country.DENMARK).build(),
        page,
        1);
    assertSearch(
        InstitutionSearchParams.builder().city("CPH").country(Country.DENMARK).build(), page, 0);

    // machine tags
    assertSearch(InstitutionSearchParams.builder().machineTagNamespace("dummy").build(), page, 0);
    assertSearch(
        InstitutionSearchParams.builder().machineTagName(mt.getName()).build(),
        page,
        1,
        inst1.getKey());

    assertSearch(
        InstitutionSearchParams.builder().machineTagName(mt.getName()).build(),
        page,
        1,
        inst1.getKey());

    assertSearch(
        InstitutionSearchParams.builder().machineTagValue(mt.getValue()).build(),
        page,
        1,
        inst1.getKey());

    assertSearch(
        InstitutionSearchParams.builder()
            .machineTagName(mt.getName())
            .machineTagName(mt.getName())
            .machineTagValue(mt.getValue())
            .build(),
        page,
        1,
        inst1.getKey());

    // identifiers
    assertSearch(InstitutionSearchParams.builder().identifier("dummy").build(), page, 0);
    assertSearch(
        InstitutionSearchParams.builder()
            .machineTagName(mt.getName())
            .machineTagName(mt.getName())
            .machineTagValue(mt.getValue())
            .build(),
        page,
        1,
        inst1.getKey());

    assertSearch(
        InstitutionSearchParams.builder().identifierType(identifier.getType()).build(),
        page,
        1,
        inst2.getKey());

    assertSearch(
        InstitutionSearchParams.builder().identifier(identifier.getIdentifier()).build(),
        page,
        1,
        inst2.getKey());

    assertSearch(
        InstitutionSearchParams.builder()
            .identifierType(identifier.getType())
            .identifier(identifier.getIdentifier())
            .build(),
        page,
        1,
        inst2.getKey());
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

    Pageable page = PAGE.apply(5, 0L);
    assertSearch(InstitutionSearchParams.builder().query("i1 n1").build(), page, 1, inst1.getKey());
    assertSearch(InstitutionSearchParams.builder().query("i2 i1").build(), page, 0);
    assertSearch(InstitutionSearchParams.builder().query("i3").build(), page, 0);
    assertSearch(InstitutionSearchParams.builder().query("n1").build(), page, 2);
    assertSearch(
        InstitutionSearchParams.builder().query("dummy address fo ").build(),
        page,
        1,
        inst1.getKey());
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

    Pageable page = PAGE.apply(1, 0L);
    assertSearch(InstitutionSearchParams.builder().query("i1 n1").build(), page, 1, inst1.getKey());

    InstitutionSearchParams params = InstitutionSearchParams.builder().query("i1").build();
    List<Institution> institutions = institutionMapper.list(params, page);
    long count = institutionMapper.count(params);
    assertEquals(1, institutions.size());
    // it should return the one where the i1 is main code
    assertEquals(inst1.getKey(), institutions.get(0).getKey());
    // there are 2 insts with i1
    assertEquals(2, count);

    params = InstitutionSearchParams.builder().query("i2").build();
    institutions = institutionMapper.list(params, page);
    count = institutionMapper.count(params);
    assertEquals(1, institutions.size());
    // it should return the one where the i1 is main code
    assertEquals(inst2.getKey(), institutions.get(0).getKey());
    // there are 2 insts with i1
    assertEquals(2, count);

    assertSearch(
        InstitutionSearchParams.builder().alternativeCode("i1").build(), page, 1, inst2.getKey());
  }

  private List<Institution> assertSearch(
      InstitutionSearchParams params, Pageable page, int expected) {
    List<Institution> res = institutionMapper.list(params, page);
    long count = institutionMapper.count(params);
    assertEquals(expected, count);
    assertEquals(res.size(), count);
    return res;
  }

  private void assertSearch(
      InstitutionSearchParams params, Pageable page, int expected, UUID expectedKey) {
    List<Institution> res = assertSearch(params, page, expected);
    assertEquals(expectedKey, res.get(0).getKey());
  }
}
