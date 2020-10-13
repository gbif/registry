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
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.persistence.mapper.collections.PersonMapper;
import org.gbif.registry.persistence.mapper.collections.external.CollectionDto;
import org.gbif.registry.persistence.mapper.collections.external.IDigBioMapper;
import org.gbif.registry.persistence.mapper.collections.external.IdentifierDto;
import org.gbif.registry.persistence.mapper.collections.external.MachineTagDto;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class IDigBioMapperIT extends BaseItTest {

  private IDigBioMapper iDigBioMapper;
  private CollectionMapper collectionMapper;
  private InstitutionMapper institutionMapper;
  private MachineTagMapper machineTagMapper;
  private IdentifierMapper identifierMapper;
  private PersonMapper personMapper;
  private AddressMapper addressMapper;

  @Autowired
  public IDigBioMapperIT(
      IDigBioMapper iDigBioMapper,
      MachineTagMapper machineTagMapper,
      CollectionMapper collectionMapper,
      IdentifierMapper identifierMapper,
      InstitutionMapper institutionMapper,
      PersonMapper personMapper,
      AddressMapper addressMapper,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer) {
    super(principalProvider, esServer);
    this.iDigBioMapper = iDigBioMapper;
    this.machineTagMapper = machineTagMapper;
    this.collectionMapper = collectionMapper;
    this.identifierMapper = identifierMapper;
    this.institutionMapper = institutionMapper;
    this.personMapper = personMapper;
    this.addressMapper = addressMapper;
  }

  @Test
  public void getMachineTagsTest() {
    Collection col1 = new Collection();
    col1.setKey(UUID.randomUUID());
    col1.setCode("c1");
    col1.setName("n1");
    col1.setCreatedBy("test");
    col1.setModifiedBy("test");

    collectionMapper.create(col1);

    Identifier i = new Identifier(IdentifierType.IH_IRN, "irn1");
    i.setCreatedBy("test");
    identifierMapper.createIdentifier(i);
    collectionMapper.addIdentifier(col1.getKey(), i.getKey());

    List<IdentifierDto> ids = iDigBioMapper.getIdentifiers(Collections.singleton(col1.getKey()));
    assertEquals(1, ids.size());
  }

  @Test
  public void getIdentifiersTest() {
    Collection col1 = new Collection();
    col1.setKey(UUID.randomUUID());
    col1.setCode("c1");
    col1.setName("n1");
    col1.setCreatedBy("test");
    col1.setModifiedBy("test");

    collectionMapper.create(col1);

    MachineTag mt = new MachineTag("iDigBio.org", "test", "foo");
    mt.setCreatedBy("test");
    machineTagMapper.createMachineTag(mt);
    collectionMapper.addMachineTag(col1.getKey(), mt.getKey());

    List<MachineTagDto> tags = iDigBioMapper.getMachineTags(null);
    assertEquals(1, tags.size());
  }

  @Test
  public void getCollectionsTest() {
    Address addr1 = new Address();
    addr1.setAddress("addr");
    addr1.setCity("city");
    addr1.setProvince("provi");
    addr1.setPostalCode("pc");
    addressMapper.create(addr1);

    Address addr2 = new Address();
    addr2.setAddress("addr2");
    addr2.setCity("city2");
    addr2.setProvince("provi2");
    addr2.setPostalCode("pc2");
    addressMapper.create(addr2);

    Institution i1 = new Institution();
    i1.setKey(UUID.randomUUID());
    i1.setCode("i1");
    i1.setName("i1");
    i1.setCreatedBy("test");
    i1.setModifiedBy("test");
    i1.setAddress(addr1);
    i1.setLatitude(BigDecimal.valueOf(12));
    i1.setLongitude(BigDecimal.valueOf(13));
    i1.setAlternativeCodes(Collections.singletonList(new AlternativeCode("II", "test")));
    institutionMapper.create(i1);

    MachineTag uniqueNameUUIDMt = new MachineTag("iDigBio.org", "UniqueNameUUID", "foo");
    uniqueNameUUIDMt.setCreatedBy("test");
    machineTagMapper.createMachineTag(uniqueNameUUIDMt);
    institutionMapper.addMachineTag(i1.getKey(), uniqueNameUUIDMt.getKey());

    Collection col1 = new Collection();
    col1.setKey(UUID.randomUUID());
    col1.setCode("c1");
    col1.setName("n1");
    col1.setDescription("desc1");
    col1.setAlternativeCodes(Collections.singletonList(new AlternativeCode("CC", "test")));
    col1.setInstitutionKey(i1.getKey());
    col1.setMailingAddress(addr2);
    col1.setCreatedBy("test");
    col1.setModifiedBy("test");
    collectionMapper.create(col1);

    Person p1 = new Person();
    p1.setKey(UUID.randomUUID());
    p1.setFirstName("a");
    p1.setLastName("b");
    p1.setPosition("p");
    p1.setEmail("aadsf@aa.com");
    p1.setCreatedBy("test");
    p1.setModifiedBy("test");
    personMapper.create(p1);
    collectionMapper.addContact(col1.getKey(), p1.getKey());

    Person p2 = new Person();
    p2.setKey(UUID.randomUUID());
    p2.setFirstName("a2");
    p2.setLastName("b2");
    p2.setPosition("p2");
    p2.setEmail("aadsf2@aa.com");
    p2.setCreatedBy("test");
    p2.setModifiedBy("test");
    personMapper.create(p2);
    collectionMapper.addContact(col1.getKey(), p2.getKey());

    List<CollectionDto> colls = iDigBioMapper.getCollections(Collections.singleton(col1.getKey()));
    assertEquals(1, colls.size());
    CollectionDto collDto = colls.get(0);
    assertNotNull(collDto.getContact());
    assertEquals(uniqueNameUUIDMt.getValue(), collDto.getUniqueNameUUID());
  }

  @Test
  public void findCollectionByIDigBioUuidTest() {
    Collection col1 = new Collection();
    col1.setKey(UUID.randomUUID());
    col1.setCode("c1");
    col1.setName("n1");
    col1.setCreatedBy("test");
    col1.setModifiedBy("test");

    collectionMapper.create(col1);

    MachineTag mt = new MachineTag("iDigBio.org", "CollectionUUID", "urn:uuid:abcd");
    mt.setCreatedBy("test");
    machineTagMapper.createMachineTag(mt);
    collectionMapper.addMachineTag(col1.getKey(), mt.getKey());

    UUID found = iDigBioMapper.findCollectionByIDigBioUuid("urn:uuid:abcd");
    assertEquals(col1.getKey(), found);
  }
}
