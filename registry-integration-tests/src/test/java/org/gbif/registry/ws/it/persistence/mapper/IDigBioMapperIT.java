/*
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
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionContactMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.persistence.mapper.collections.PersonMapper;
import org.gbif.registry.persistence.mapper.collections.external.IDigBioCollectionDto;
import org.gbif.registry.persistence.mapper.collections.external.IDigBioMapper;
import org.gbif.registry.persistence.mapper.collections.external.IdentifierDto;
import org.gbif.registry.persistence.mapper.collections.external.MachineTagDto;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

import static org.gbif.registry.domain.collections.Constants.IDIGBIO_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class IDigBioMapperIT extends BaseItTest {

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule =
      new TestCaseDatabaseInitializer(
          "collection_person", "collection", "institution", "address", "identifier");

  private IDigBioMapper iDigBioMapper;
  private CollectionMapper collectionMapper;
  private InstitutionMapper institutionMapper;
  private MachineTagMapper machineTagMapper;
  private IdentifierMapper identifierMapper;
  private PersonMapper personMapper;
  private AddressMapper addressMapper;
  private CollectionContactMapper contactMapper;

  @Autowired
  public IDigBioMapperIT(
      IDigBioMapper iDigBioMapper,
      MachineTagMapper machineTagMapper,
      CollectionMapper collectionMapper,
      IdentifierMapper identifierMapper,
      InstitutionMapper institutionMapper,
      PersonMapper personMapper,
      AddressMapper addressMapper,
      CollectionContactMapper contactMapper,
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
    this.contactMapper = contactMapper;
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

    MachineTag mt = new MachineTag(IDIGBIO_NAMESPACE, "test", "foo");
    mt.setCreatedBy("test");
    machineTagMapper.createMachineTag(mt);
    collectionMapper.addMachineTag(col1.getKey(), mt.getKey());

    List<MachineTagDto> tags = iDigBioMapper.getIDigBioMachineTags(null);
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

    MachineTag uniqueNameUUIDMt = new MachineTag(IDIGBIO_NAMESPACE, "UniqueNameUUID", "foo");
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

    Contact c1 = new Contact();
    c1.setFirstName("a");
    c1.setLastName("b");
    c1.setPosition(Collections.singletonList("p"));
    c1.setEmail(Collections.singletonList("aadsf@aa.com"));
    c1.setCreatedBy("test");
    c1.setModifiedBy("test");
    contactMapper.createContact(c1);
    collectionMapper.addContactPerson(col1.getKey(), c1.getKey());

    Contact c2 = new Contact();
    c2.setFirstName("a2");
    c2.setLastName("b2");
    c2.setPosition(Collections.singletonList("p2"));
    c2.setEmail(Collections.singletonList("aadsf2@aa.com"));
    c2.setCreatedBy("test");
    c2.setModifiedBy("test");
    contactMapper.createContact(c2);
    collectionMapper.addContactPerson(col1.getKey(), c2.getKey());

    List<IDigBioCollectionDto> colls =
        iDigBioMapper.getCollections(Collections.singleton(col1.getKey()));
    assertEquals(1, colls.size());
    IDigBioCollectionDto collDto = colls.get(0);
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

    MachineTag mt = new MachineTag(IDIGBIO_NAMESPACE, "CollectionUUID", "urn:uuid:abcd");
    mt.setCreatedBy("test");
    machineTagMapper.createMachineTag(mt);
    collectionMapper.addMachineTag(col1.getKey(), mt.getKey());

    Set<UUID> found = iDigBioMapper.findIDigBioCollections("urn:uuid:abcd");
    assertEquals(col1.getKey(), found.iterator().next());
  }

  @Test
  public void findCollectionsByCountryTest() {
    Address addr1 = new Address();
    addr1.setAddress("addr");
    addr1.setCity("city");
    addr1.setProvince("provi");
    addr1.setPostalCode("pc");
    addr1.setCountry(Country.UNITED_STATES);
    addressMapper.create(addr1);

    Collection col1 = new Collection();
    col1.setKey(UUID.randomUUID());
    col1.setCode("c1");
    col1.setName("n1");
    col1.setAddress(addr1);
    col1.setCreatedBy("test");
    col1.setModifiedBy("test");

    collectionMapper.create(col1);

    Set<UUID> colls =
        iDigBioMapper.findCollectionsByCountry(Country.UNITED_STATES.getIso2LetterCode());
    assertEquals(1, colls.size());
  }
}
