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
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.MasterSourceMetadata;
import org.gbif.api.model.collections.UserId;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.collections.IdType;
import org.gbif.api.vocabulary.collections.MasterSourceType;
import org.gbif.api.vocabulary.collections.Source;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionContactMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.persistence.mapper.collections.MasterSourceSyncMetadataMapper;
import org.gbif.registry.persistence.mapper.collections.params.InstitutionListParams;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.net.URI;
import java.util.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

import static org.gbif.registry.ws.it.fixtures.TestConstants.PAGE;
import static org.junit.jupiter.api.Assertions.*;

public class InstitutionMapperIT extends BaseItTest {

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule =
      new TestCaseDatabaseInitializer(
          "institution_identifier",
          "institution_tag",
          "institution_occurrence_mapping",
          "institution");

  private InstitutionMapper institutionMapper;
  private AddressMapper addressMapper;
  private MachineTagMapper machineTagMapper;
  private IdentifierMapper identifierMapper;
  private CollectionContactMapper contactMapper;
  private MasterSourceSyncMetadataMapper metadataMapper;

  @Autowired
  public InstitutionMapperIT(
      InstitutionMapper institutionMapper,
      AddressMapper addressMapper,
      MachineTagMapper machineTagMapper,
      IdentifierMapper identifierMapper,
      CollectionContactMapper contactMapper,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer,
      MasterSourceSyncMetadataMapper metadataMapper) {
    super(principalProvider, esServer);
    this.institutionMapper = institutionMapper;
    this.addressMapper = addressMapper;
    this.machineTagMapper = machineTagMapper;
    this.identifierMapper = identifierMapper;
    this.contactMapper = contactMapper;
    this.metadataMapper = metadataMapper;
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
    institution.setDisciplines(Collections.singletonList("Archaeology"));
    institution.setEmail(Collections.singletonList("test@test.com"));
    institution.setPhone(Collections.singletonList("1234"));
    institution.setAlternativeCodes(
        Collections.singletonList(new AlternativeCode("CODE2", "another code")));
    institution.setDisplayOnNHCPortal(true);
    institution.setFeaturedImageUrl(URI.create("http://test.com"));
    institution.setFeaturedImageLicense(License.CC0_1_0);
    institution.setFeaturedImageAttribution("dummy image attribution");

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
    inst1.setMasterSource(MasterSourceType.IH);

    Address addressInst1 = new Address();
    addressInst1.setCountry(Country.DENMARK);
    addressInst1.setCity("Copenhagen");
    addressMapper.create(addressInst1);
    inst1.setAddress(addressInst1);

    Address mailAddressCol1 = new Address();
    mailAddressCol1.setCountry(Country.DENMARK);
    mailAddressCol1.setCity("Odense");
    addressMapper.create(mailAddressCol1);
    inst1.setMailingAddress(mailAddressCol1);

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
    inst2.setMasterSource(MasterSourceType.GBIF_REGISTRY);
    institutionMapper.create(inst2);

    Identifier identifier = new Identifier(IdentifierType.IH_IRN, "test_id");
    identifier.setCreatedBy("test");
    identifierMapper.createIdentifier(identifier);
    institutionMapper.addCollectionIdentifier(inst2.getKey(), identifier.getKey(), identifier.isPrimary());

    Institution inst3 = new Institution();
    inst3.setKey(UUID.randomUUID());
    inst3.setCode("i3");
    inst3.setName("Name of the third institution");
    inst3.setCreatedBy("test");
    inst3.setModifiedBy("test");
    institutionMapper.create(inst3);

    Institution inst4 = new Institution();
    inst4.setKey(UUID.randomUUID());
    inst4.setCode("i4");
    inst4.setName("Name of the forth institution");
    inst4.setCreatedBy("test");
    inst4.setModifiedBy("test");
    institutionMapper.create(inst4);

    MasterSourceMetadata masterSourceMetadata = new MasterSourceMetadata();
    masterSourceMetadata.setSource(Source.IH_IRN);
    masterSourceMetadata.setKey(123456);
    masterSourceMetadata.setSourceId("test-123");
    masterSourceMetadata.setCreatedBy("test");
    metadataMapper.create(masterSourceMetadata);
    institutionMapper.addMasterSourceMetadata(
        inst4.getKey(), masterSourceMetadata.getKey(), MasterSourceType.GRSCICOLL);

    Pageable page = PAGE.apply(5, 0L);

    assertSearch(
        InstitutionListParams.builder()
            .sourceId(asList("test-123"))
            .source(asList(Source.IH_IRN))
            .build(),
        1,
        inst4.getKey());
    assertSearch(InstitutionListParams.builder().page(page).build(), 4);
    assertSearch(InstitutionListParams.builder().code(asList("i1")).page(page).build(), 1);
    assertSearch(InstitutionListParams.builder().code(asList("I1")).page(page).build(), 1);
    assertSearch(InstitutionListParams.builder().name(asList("n2")).page(page).build(), 1);
    assertSearch(
        InstitutionListParams.builder().code(asList("i2")).name(asList("n2")).page(page).build(),
        1);
    assertSearch(
        InstitutionListParams.builder().code(asList("i1")).name(asList("n2")).page(page).build(),
        0);
    assertSearch(
        InstitutionListParams.builder()
            .fuzzyName(asList("nime of third institution"))
            .page(page)
            .build(),
        1);
    assertSearch(
        InstitutionListParams.builder().query("nime of third institution").page(page).build(), 0);
    assertSearch(
        InstitutionListParams.builder()
            .countries(Collections.singletonList(Country.DENMARK))
            .page(page)
            .build(),
        1);
    assertSearch(
        InstitutionListParams.builder()
            .countries(Collections.singletonList(Country.SPAIN))
            .page(page)
            .build(),
        0);
    assertSearch(InstitutionListParams.builder().city(asList("Odense")).page(page).build(), 0);
    assertSearch(
        InstitutionListParams.builder()
            .city(asList("Copenhagen"))
            .countries(Collections.singletonList(Country.DENMARK))
            .page(page)
            .build(),
        1);
    assertSearch(
        InstitutionListParams.builder()
            .city(asList("CPH"))
            .countries(Collections.singletonList(Country.DENMARK))
            .page(page)
            .build(),
        0);

    // machine tags
    assertSearch(
        InstitutionListParams.builder().machineTagNamespace(asList("dummy")).page(page).build(), 0);
    assertSearch(
        InstitutionListParams.builder().machineTagName(asList(mt.getName())).page(page).build(),
        1,
        inst1.getKey());

    assertSearch(
        InstitutionListParams.builder().machineTagName(asList(mt.getName())).page(page).build(),
        1,
        inst1.getKey());

    assertSearch(
        InstitutionListParams.builder().machineTagValue(asList(mt.getValue())).page(page).build(),
        1,
        inst1.getKey());

    assertSearch(
        InstitutionListParams.builder()
            .machineTagName(asList(mt.getName()))
            .machineTagName(asList(mt.getName()))
            .machineTagValue(asList(mt.getValue()))
            .page(page)
            .build(),
        1,
        inst1.getKey());

    // identifiers
    assertSearch(InstitutionListParams.builder().identifier(asList("dummy")).page(page).build(), 0);
    assertSearch(
        InstitutionListParams.builder()
            .machineTagName(asList(mt.getName()))
            .machineTagName(asList(mt.getName()))
            .machineTagValue(asList(mt.getValue()))
            .page(page)
            .build(),
        1,
        inst1.getKey());

    assertSearch(
        InstitutionListParams.builder()
            .identifierType(asList(identifier.getType()))
            .page(page)
            .build(),
        1,
        inst2.getKey());

    assertSearch(
        InstitutionListParams.builder()
            .identifier(asList(identifier.getIdentifier()))
            .page(page)
            .build(),
        1,
        inst2.getKey());

    assertSearch(
        InstitutionListParams.builder()
            .identifierType(asList(identifier.getType()))
            .identifier(asList(identifier.getIdentifier()))
            .page(page)
            .build(),
        1,
        inst2.getKey());

    assertSearch(
        InstitutionListParams.builder()
            .masterSourceType(asList(MasterSourceType.IH))
            .page(page)
            .build(),
        1,
        inst1.getKey());

    assertSearch(
        InstitutionListParams.builder()
            .masterSourceType(asList(MasterSourceType.GBIF_REGISTRY))
            .page(page)
            .build(),
        1,
        inst2.getKey());

    assertSearch(
        InstitutionListParams.builder()
            .masterSourceType(asList(MasterSourceType.GRSCICOLL))
            .page(page)
            .build(),
        1);
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
    assertSearch(
        InstitutionListParams.builder().query("i1 n1").page(page).build(), 1, inst1.getKey());
    assertSearch(InstitutionListParams.builder().query("i2 i1").page(page).build(), 0);
    assertSearch(InstitutionListParams.builder().query("i3").page(page).build(), 0);
    assertSearch(InstitutionListParams.builder().query("n1").page(page).build(), 2);
    assertSearch(
        InstitutionListParams.builder().query("dummy address fo ").page(page).build(),
        1,
        inst1.getKey());

    Contact contact1 = new Contact();
    contact1.setFirstName("Name1");
    contact1.setLastName("Surname1");
    contact1.setEmail(Collections.singletonList("aa1@aa.com"));
    contact1.setTaxonomicExpertise(Arrays.asList("aves", "fungi"));
    contact1.setCreatedBy("test");
    contact1.setModifiedBy("test");

    UserId userId1 = new UserId(IdType.OTHER, "12345");
    UserId userId2 = new UserId(IdType.OTHER, "abcde");
    contact1.setUserIds(Arrays.asList(userId1, userId2));

    contactMapper.createContact(contact1);

    contact1 = contactMapper.getContact(contact1.getKey());
    assertNotNull(contact1.getCreated());
    assertNotNull(contact1.getModified());

    institutionMapper.addContactPerson(inst1.getKey(), contact1.getKey());
    assertSearch(InstitutionListParams.builder().query("Name1").page(page).build(), 1);
    assertSearch(InstitutionListParams.builder().query("Name0").page(page).build(), 0);
    assertSearch(InstitutionListParams.builder().query("Surname1").page(page).build(), 1);
    assertSearch(InstitutionListParams.builder().query("aa1@aa.com").page(page).build(), 1);
    assertSearch(InstitutionListParams.builder().query("aves").page(page).build(), 1);
    assertSearch(InstitutionListParams.builder().query("12345").page(page).build(), 1);
    assertSearch(InstitutionListParams.builder().query("abcde").page(page).build(), 1);
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
    assertSearch(
        InstitutionListParams.builder().query("i1 n1").page(page).build(), 1, inst1.getKey());

    InstitutionListParams params = InstitutionListParams.builder().query("i1").page(page).build();
    List<Institution> institutions = institutionMapper.list(params);
    long count = institutionMapper.count(params);
    assertEquals(1, institutions.size());
    // it should return the one where the i1 is main code
    assertEquals(inst1.getKey(), institutions.get(0).getKey());
    // there are 2 insts with i1
    assertEquals(2, count);

    params = InstitutionListParams.builder().query("i2").page(page).build();
    institutions = institutionMapper.list(params);
    count = institutionMapper.count(params);
    assertEquals(1, institutions.size());
    // it should return the one where the i1 is main code
    assertEquals(inst2.getKey(), institutions.get(0).getKey());
    // there are 2 insts with i1
    assertEquals(2, count);

    assertSearch(
        InstitutionListParams.builder().alternativeCode(asList("i1")).page(page).page(page).build(),
        1,
        inst2.getKey());
  }

  private List<Institution> assertSearch(InstitutionListParams params, int expected) {
    List<Institution> res = institutionMapper.list(params);
    long count = institutionMapper.count(params);
    assertEquals(expected, count);
    assertEquals(res.size(), count);
    return res;
  }

  private void assertSearch(InstitutionListParams params, int expected, UUID expectedKey) {
    List<Institution> res = assertSearch(params, expected);
    assertEquals(expectedKey, res.get(0).getKey());
  }
}
