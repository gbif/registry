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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.AlternativeCode;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.descriptors.DescriptorGroup;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.License;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionsSearchMapper;
import org.gbif.registry.persistence.mapper.collections.DescriptorsMapper;
import org.gbif.registry.persistence.mapper.collections.dto.CollectionSearchDto;
import org.gbif.registry.persistence.mapper.collections.dto.DescriptorDto;
import org.gbif.registry.persistence.mapper.collections.params.DescriptorsParams;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

public class CollectionsSearchMapperIT extends BaseItTest {

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule =
      new TestCaseDatabaseInitializer("collection");

  private CollectionMapper collectionMapper;
  private AddressMapper addressMapper;
  private DescriptorsMapper descriptorsMapper;
  private CollectionsSearchMapper collectionsSearchMapper;

  @Autowired
  public CollectionsSearchMapperIT(
      CollectionMapper collectionMapper,
      AddressMapper addressMapper,
      DescriptorsMapper descriptorsMapper,
      CollectionsSearchMapper collectionsSearchMapper,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer) {
    super(principalProvider, esServer);
    this.collectionMapper = collectionMapper;
    this.addressMapper = addressMapper;
    this.descriptorsMapper = descriptorsMapper;
    this.collectionsSearchMapper = collectionsSearchMapper;
  }

  @Test
  public void searchTest() {
    UUID collectionKey = UUID.randomUUID();

    Collection collection = new Collection();
    collection.setKey(collectionKey);
    collection.setAccessionStatus("Institutional");
    collection.setCode("CODE");
    collection.setName("NAME");
    collection.setCreatedBy("test");
    collection.setModifiedBy("test");
    collection.setEmail(Collections.singletonList("test@test.com"));
    collection.setPhone(Collections.singletonList("1234"));
    collection.setNumberSpecimens(12);
    collection.setTaxonomicCoverage("taxonomic coverage");
    collection.setGeographicCoverage("geography");
    collection.setNotes("notes for testing");
    collection.setIncorporatedCollections(Arrays.asList("col1", "col2"));
    collection.setAlternativeCodes(
        Collections.singletonList(new AlternativeCode("CODE2", "another code")));
    collection.setDivision("division");
    collection.setDepartment("department");
    collection.setDisplayOnNHCPortal(true);
    collection.setFeaturedImageUrl(URI.create("http://test.com"));
    collection.setFeaturedImageLicense(License.CC0_1_0);
    collection.setTemporalCoverage("temporal coverage");
    collection.setFeaturedImageAttribution("dummy image attribution");

    List<String> preservationTypes = new ArrayList<>();
    preservationTypes.add("StorageControlledAtmosphere");
    preservationTypes.add("SampleCryopreserved");
    collection.setPreservationTypes(preservationTypes);

    Address address = new Address();
    address.setAddress("dummy address");
    address.setCountry(Country.SPAIN);
    address.setCity("Oviedo");
    addressMapper.create(address);
    assertNotNull(address.getKey());
    collection.setAddress(address);

    Address mailingAddress = new Address();
    mailingAddress.setAddress("dummy mailing address");
    mailingAddress.setCountry(Country.SPAIN);
    mailingAddress.setCity("Oviedo");
    addressMapper.create(mailingAddress);
    assertNotNull(mailingAddress.getKey());
    collection.setMailingAddress(mailingAddress);

    collectionMapper.create(collection);

    DescriptorGroup DescriptorGroup = new DescriptorGroup();
    DescriptorGroup.setCollectionKey(collectionKey);
    DescriptorGroup.setTitle("title");
    DescriptorGroup.setCreatedBy("test");
    DescriptorGroup.setModifiedBy("test");
    descriptorsMapper.createDescriptorGroup(DescriptorGroup);

    DescriptorDto descriptorDto = new DescriptorDto();
    descriptorDto.setDescriptorGroupKey(DescriptorGroup.getKey());
    descriptorDto.setUsageName("aves");
    descriptorDto.setCountry(Country.SPAIN);
    descriptorsMapper.createDescriptor(descriptorDto);

    List<CollectionSearchDto> dtos =
        collectionsSearchMapper.searchCollections(
            DescriptorsParams.builder().query("aves").build());
    assertEquals(1, dtos.size());
    assertEquals(Country.SPAIN, dtos.get(0).getCountry());
    assertEquals(Country.SPAIN, dtos.get(0).getMailingCountry());
    assertEquals("Oviedo", dtos.get(0).getCity());
    assertEquals("Oviedo", dtos.get(0).getMailingCity());

    dtos =
        collectionsSearchMapper.searchCollections(
            DescriptorsParams.builder().query("division").build());
    assertEquals(1, dtos.size());
    assertEquals(0, dtos.get(0).getQueryDescriptorRank());
    assertTrue(dtos.get(0).getQueryRank() > 0);

    dtos =
        collectionsSearchMapper.searchCollections(
            DescriptorsParams.builder().query("aves").build());
    assertEquals(1, dtos.size());
    assertEquals(0, dtos.get(0).getQueryRank());
    assertTrue(dtos.get(0).getQueryDescriptorRank() > 0);

    dtos =
        collectionsSearchMapper.searchCollections(
            DescriptorsParams.builder().usageName(Collections.singletonList("aves")).build());
    assertEquals(1, dtos.size());
    assertNotNull(dtos.get(0).getDescriptorKey());
  }
}
