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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.descriptors.DescriptorGroup;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.collections.CollectionFacetParameter;
import org.gbif.api.vocabulary.collections.InstitutionFacetParameter;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.persistence.mapper.dto.GrSciCollVocabConceptDto;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionsSearchMapper;
import org.gbif.registry.persistence.mapper.collections.DescriptorsMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.persistence.mapper.collections.dto.CollectionSearchDto;
import org.gbif.registry.persistence.mapper.collections.dto.DescriptorDto;
import org.gbif.registry.persistence.mapper.collections.dto.FacetDto;
import org.gbif.registry.persistence.mapper.collections.params.DescriptorsListParams;
import org.gbif.registry.persistence.mapper.collections.params.InstitutionListParams;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.gbif.registry.persistence.mapper.GrScicollVocabConceptMapper;
import org.gbif.registry.ws.it.collections.ConceptTestSetup;

public class CollectionsSearchMapperIT extends BaseItTest {

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule =
      new TestCaseDatabaseInitializer("collection");

  private CollectionMapper collectionMapper;
  private InstitutionMapper institutionMapper;
  private AddressMapper addressMapper;
  private DescriptorsMapper descriptorsMapper;
  private CollectionsSearchMapper collectionsSearchMapper;
  private GrScicollVocabConceptMapper grScicollVocabConceptMapper;

  @Autowired
  public CollectionsSearchMapperIT(
      CollectionMapper collectionMapper,
      InstitutionMapper institutionMapper,
      AddressMapper addressMapper,
      DescriptorsMapper descriptorsMapper,
      CollectionsSearchMapper collectionsSearchMapper,
      GrScicollVocabConceptMapper grScicollVocabConceptMapper,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer) {
    super(principalProvider, esServer);
    this.collectionMapper = collectionMapper;
    this.institutionMapper = institutionMapper;
    this.addressMapper = addressMapper;
    this.descriptorsMapper = descriptorsMapper;
    this.collectionsSearchMapper = collectionsSearchMapper;
    this.grScicollVocabConceptMapper = grScicollVocabConceptMapper;
  }

  @BeforeEach
  public void setupFacets() {
    ConceptTestSetup.setupCommonConcepts(grScicollVocabConceptMapper);
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

    DescriptorGroup descriptorGroup = new DescriptorGroup();
    descriptorGroup.setCollectionKey(collectionKey);
    descriptorGroup.setTitle("title");
    descriptorGroup.setCreatedBy("test");
    descriptorGroup.setModifiedBy("test");
    descriptorsMapper.createDescriptorGroup(descriptorGroup);

    DescriptorDto descriptorDto = new DescriptorDto();
    descriptorDto.setDescriptorGroupKey(descriptorGroup.getKey());
    descriptorDto.setUsageName("aves");
    descriptorDto.setCountry(Country.SPAIN);
    descriptorsMapper.createDescriptor(descriptorDto);

    List<CollectionSearchDto> dtos =
        collectionsSearchMapper.searchCollections(
            DescriptorsListParams.builder().query("aves").build());
    assertEquals(1, dtos.size());
    assertEquals(Country.SPAIN, dtos.get(0).getCountry());
    assertEquals(Country.SPAIN, dtos.get(0).getMailingCountry());
    assertEquals("Oviedo", dtos.get(0).getCity());
    assertEquals("Oviedo", dtos.get(0).getMailingCity());

    dtos =
        collectionsSearchMapper.searchCollections(
            DescriptorsListParams.builder().query("division").build());
    assertEquals(1, dtos.size());
    assertEquals(0, dtos.get(0).getQueryDescriptorRank());
    assertTrue(dtos.get(0).getQueryRank() > 0);

    dtos =
        collectionsSearchMapper.searchCollections(
            DescriptorsListParams.builder().query("aves").build());
    assertEquals(1, dtos.size());
    assertEquals(0, dtos.get(0).getQueryRank());
    assertTrue(dtos.get(0).getQueryDescriptorRank() > 0);

    dtos =
        collectionsSearchMapper.searchCollections(
            DescriptorsListParams.builder().usageName(Collections.singletonList("aves")).build());
    assertEquals(1, dtos.size());
    assertNotNull(dtos.get(0).getDescriptorKey());
  }

  @Test
  public void facetsTest() {
    UUID i1Key = UUID.randomUUID();
    Institution i1 = new Institution();
    i1.setKey(i1Key);
    i1.setName("i1");
    i1.setCode("i1");
    i1.setTypes(Arrays.asList("ty1", "ty2"));
    i1.setDisciplines(Arrays.asList("di1", "di2"));
    i1.setCreatedBy("test");
    i1.setModifiedBy("test");

    Address ai1 = new Address();
    ai1.setCountry(Country.SPAIN);
    ai1.setCity("Oviedo");
    addressMapper.create(ai1);
    assertNotNull(ai1.getKey());
    i1.setAddress(ai1);

    institutionMapper.create(i1);

    // Create institution facet links for i1
    Long ty1FacetKey = grScicollVocabConceptMapper.getConceptKeyByVocabularyAndName("InstitutionType", "ty1");
    Long ty2FacetKey = grScicollVocabConceptMapper.getConceptKeyByVocabularyAndName("InstitutionType", "ty2");
    Long di1FacetKey = grScicollVocabConceptMapper.getConceptKeyByVocabularyAndName("Discipline", "di1");
    Long di2FacetKey = grScicollVocabConceptMapper.getConceptKeyByVocabularyAndName("Discipline", "di2");

    grScicollVocabConceptMapper.insertInstitutionConcept(i1Key, ty1FacetKey);
    grScicollVocabConceptMapper.insertInstitutionConcept(i1Key, ty2FacetKey);
    grScicollVocabConceptMapper.insertInstitutionConcept(i1Key, di1FacetKey);
    grScicollVocabConceptMapper.insertInstitutionConcept(i1Key, di2FacetKey);

    UUID i2Key = UUID.randomUUID();
    Institution i2 = new Institution();
    i2.setKey(i2Key);
    i2.setName("i2");
    i2.setCode("i2");
    i2.setTypes(Collections.singletonList("ty1"));
    i2.setCreatedBy("test");
    i2.setModifiedBy("test");

    Address ai2 = new Address();
    ai2.setCountry(Country.SPAIN);
    ai2.setCity("Bilbao");
    addressMapper.create(ai2);
    assertNotNull(ai2.getKey());
    i2.setMailingAddress(ai2);

    institutionMapper.create(i2);

    // Create institution facet links for i2 (only has ty1 type)
    grScicollVocabConceptMapper.insertInstitutionConcept(i2Key, ty1FacetKey);

    UUID c1Key = UUID.randomUUID();
    Collection c1 = new Collection();
    c1.setKey(c1Key);
    c1.setAccessionStatus("Institutional");
    c1.setCode("c1");
    c1.setName("name1");
    c1.setCreatedBy("test");
    c1.setModifiedBy("test");

    List<String> preservationTypes = new ArrayList<>();
    preservationTypes.add("StorageControlledAtmosphere");
    preservationTypes.add("SampleCryopreserved");
    c1.setPreservationTypes(preservationTypes);

    Address address = new Address();
    address.setCountry(Country.SPAIN);
    address.setCity("Oviedo");
    addressMapper.create(address);
    assertNotNull(address.getKey());
    c1.setAddress(address);

    Address mailingAddress = new Address();
    mailingAddress.setCountry(Country.SPAIN);
    addressMapper.create(mailingAddress);
    assertNotNull(mailingAddress.getKey());
    c1.setMailingAddress(mailingAddress);

    collectionMapper.create(c1);

    // Create facet links for collection facets that need to work in tests
    // For c1 with preservationTypes: StorageControlledAtmosphere, SampleCryopreserved
    Long storageControlledId = grScicollVocabConceptMapper.getConceptKeyByVocabularyAndName("PreservationType", "StorageControlledAtmosphere");
    Long sampleCryopreservedId = grScicollVocabConceptMapper.getConceptKeyByVocabularyAndName("PreservationType", "SampleCryopreserved");
    if (storageControlledId == null) {
      GrSciCollVocabConceptDto facet = new GrSciCollVocabConceptDto();
      facet.setConceptKey(20001L); // Use unique test key
      facet.setVocabularyKey(2000L); // Use test vocabulary key
      facet.setVocabularyName("PreservationType");
      facet.setName("StorageControlledAtmosphere");
      facet.setPath("StorageControlledAtmosphere");
      grScicollVocabConceptMapper.create(facet);
      grScicollVocabConceptMapper.update(facet);
      storageControlledId = facet.getConceptKey();
    }
    if (sampleCryopreservedId == null) {
      GrSciCollVocabConceptDto facet = new GrSciCollVocabConceptDto();
      facet.setConceptKey(20002L); // Use unique test key
      facet.setVocabularyKey(2000L); // Use test vocabulary key
      facet.setVocabularyName("PreservationType");
      facet.setName("SampleCryopreserved");
      facet.setPath("SampleCryopreserved");
      grScicollVocabConceptMapper.create(facet);
      grScicollVocabConceptMapper.update(facet);
      sampleCryopreservedId = facet.getConceptKey();
    }
    grScicollVocabConceptMapper.insertCollectionConcept(c1Key, storageControlledId);
    grScicollVocabConceptMapper.insertCollectionConcept(c1Key, sampleCryopreservedId);

    DescriptorGroup descriptorGroup = new DescriptorGroup();
    descriptorGroup.setCollectionKey(c1Key);
    descriptorGroup.setTitle("title");
    descriptorGroup.setCreatedBy("test");
    descriptorGroup.setModifiedBy("test");
    descriptorsMapper.createDescriptorGroup(descriptorGroup);

    DescriptorDto descriptorDto1 = new DescriptorDto();
    descriptorDto1.setDescriptorGroupKey(descriptorGroup.getKey());
    descriptorDto1.setUsageName("aves");
    descriptorDto1.setCountry(Country.DENMARK);
    descriptorDto1.setRecordedBy(Arrays.asList("John", "Clint"));
    descriptorDto1.setKingdomKey("1");
    descriptorsMapper.createDescriptor(descriptorDto1);

    DescriptorDto descriptorDto2 = new DescriptorDto();
    descriptorDto2.setDescriptorGroupKey(descriptorGroup.getKey());
    descriptorDto2.setKingdomKey("1");
    descriptorDto2.setCountry(Country.DENMARK);
    descriptorDto2.setObjectClassificationName("obn1");
    descriptorsMapper.createDescriptor(descriptorDto2);

    UUID c2Key = UUID.randomUUID();
    Collection c2 = new Collection();
    c2.setKey(c2Key);
    c2.setAccessionStatus("Institutional");
    c2.setCode("c2");
    c2.setName("name2");
    c2.setCreatedBy("test");
    c2.setModifiedBy("test");

    address = new Address();
    address.setCountry(Country.SPAIN);
    addressMapper.create(address);
    assertNotNull(address.getKey());
    c2.setAddress(address);

    mailingAddress = new Address();
    mailingAddress.setCountry(Country.SPAIN);
    mailingAddress.setCity("Bilbao");
    addressMapper.create(mailingAddress);
    assertNotNull(mailingAddress.getKey());
    c2.setMailingAddress(mailingAddress);

    collectionMapper.create(c2);

    DescriptorGroup descriptorGroupC2 = new DescriptorGroup();
    descriptorGroupC2.setCollectionKey(c2Key);
    descriptorGroupC2.setTitle("title");
    descriptorGroupC2.setCreatedBy("test");
    descriptorGroupC2.setModifiedBy("test");
    descriptorsMapper.createDescriptorGroup(descriptorGroupC2);

    DescriptorDto descriptorDtoC2 = new DescriptorDto();
    descriptorDtoC2.setDescriptorGroupKey(descriptorGroupC2.getKey());
    descriptorDtoC2.setKingdomKey("2");
    descriptorDtoC2.setTaxonKeys(List.of("123"));
    descriptorDtoC2.setRecordedBy(Collections.singletonList("John"));
    descriptorDtoC2.setCountry(Country.DENMARK);
    descriptorDtoC2.setObjectClassificationName("obn1");
    descriptorsMapper.createDescriptor(descriptorDtoC2);

    // Debug: Let's see what collections and facets we have
    System.out.println("=== Testing PRESERVATION_TYPE facets ===");
    System.out.println("Collections created: c1Key=" + c1Key + ", c2Key=" + c2Key);
    System.out.println("c1 preservation types: " + c1.getPreservationTypes());
    System.out.println("c2 preservation types: " + c2.getPreservationTypes());

    List<FacetDto> facetDtos =
        collectionsSearchMapper.collectionFacet(
            DescriptorsListParams.builder()
                .facet(CollectionFacetParameter.PRESERVATION_TYPE)
                .build());

    System.out.println("Facet query returned " + facetDtos.size() + " results:");
    facetDtos.forEach(f -> System.out.println("  Facet: " + f.getFacet() + ", Count: " + f.getCount()));

    assertEquals(2, facetDtos.size());

    facetDtos =
      collectionsSearchMapper.collectionFacet(
        DescriptorsListParams.builder().facet(CollectionFacetParameter.COUNTRY)
          .taxonKey(Collections.singletonList("123")).build());
    assertEquals(1, facetDtos.size());

    facetDtos =
        collectionsSearchMapper.collectionFacet(
            DescriptorsListParams.builder().facet(CollectionFacetParameter.COUNTRY).build());
    assertEquals(1, facetDtos.size());
    assertEquals(2, facetDtos.get(0).getCount());
    assertEquals(
        1,
        collectionsSearchMapper.collectionFacetCardinality(
            DescriptorsListParams.builder().facet(CollectionFacetParameter.COUNTRY).build()));

    facetDtos =
        collectionsSearchMapper.collectionFacet(
            DescriptorsListParams.builder()
                .facet(CollectionFacetParameter.DESCRIPTOR_COUNTRY)
                .build());
    assertEquals(1, facetDtos.size());
    assertEquals(2, facetDtos.get(0).getCount());
    assertEquals(
        1,
        collectionsSearchMapper.collectionFacetCardinality(
            DescriptorsListParams.builder()
                .facet(CollectionFacetParameter.DESCRIPTOR_COUNTRY)
                .build()));

    facetDtos =
        collectionsSearchMapper.collectionFacet(
            DescriptorsListParams.builder().facet(CollectionFacetParameter.KINGDOM_KEY).build());
    assertEquals(2, facetDtos.size());
    facetDtos.forEach(f -> assertEquals(1, f.getCount()));

    assertEquals(
        2,
        collectionsSearchMapper.collectionFacetCardinality(
            DescriptorsListParams.builder().facet(CollectionFacetParameter.KINGDOM_KEY).build()));

    facetDtos =
        collectionsSearchMapper.collectionFacet(
            DescriptorsListParams.builder().facet(CollectionFacetParameter.TYPE_STATUS).build());
    assertEquals(0, facetDtos.size());

    assertEquals(
        0,
        collectionsSearchMapper.collectionFacetCardinality(
            DescriptorsListParams.builder().facet(CollectionFacetParameter.TYPE_STATUS).build()));

    facetDtos =
        collectionsSearchMapper.collectionFacet(
            DescriptorsListParams.builder().facet(CollectionFacetParameter.CITY).build());
    assertEquals(2, facetDtos.size());
    assertEquals(2, facetDtos.stream().filter(f -> f.getCount() == 1).count());
    assertEquals(
        2,
        collectionsSearchMapper.collectionFacetCardinality(
            DescriptorsListParams.builder().facet(CollectionFacetParameter.CITY).build()));

    facetDtos =
        collectionsSearchMapper.collectionFacet(
            DescriptorsListParams.builder().facet(CollectionFacetParameter.RECORDED_BY).build());
    assertEquals(2, facetDtos.size());
    assertEquals(1, facetDtos.stream().filter(f -> f.getCount() == 1).count());
    assertEquals(1, facetDtos.stream().filter(f -> f.getCount() == 2).count());
    assertEquals(
        2,
        collectionsSearchMapper.collectionFacetCardinality(
            DescriptorsListParams.builder().facet(CollectionFacetParameter.RECORDED_BY).build()));

    facetDtos =
        collectionsSearchMapper.collectionFacet(
            DescriptorsListParams.builder()
                .facet(CollectionFacetParameter.OBJECT_CLASSIFICATION)
                .build());
    assertEquals(1, facetDtos.size());
    assertEquals(1, facetDtos.stream().filter(f -> f.getCount() == 2).count());
    assertEquals(
        1,
        collectionsSearchMapper.collectionFacetCardinality(
            DescriptorsListParams.builder()
                .facet(CollectionFacetParameter.OBJECT_CLASSIFICATION)
                .build()));

    // institution facets
    facetDtos =
        collectionsSearchMapper.institutionFacet(
            InstitutionListParams.builder().facet(InstitutionFacetParameter.COUNTRY).build());
    assertEquals(1, facetDtos.size());
    assertEquals(1, facetDtos.stream().filter(f -> f.getCount() == 2).count());
    assertEquals(
        1,
        collectionsSearchMapper.institutionFacetCardinality(
            InstitutionListParams.builder().facet(InstitutionFacetParameter.COUNTRY).build()));

    facetDtos =
        collectionsSearchMapper.institutionFacet(
            InstitutionListParams.builder().facet(InstitutionFacetParameter.CITY).build());
    assertEquals(2, facetDtos.size());
    assertEquals(2, facetDtos.stream().filter(f -> f.getCount() == 1).count());
    assertEquals(
        2,
        collectionsSearchMapper.institutionFacetCardinality(
            InstitutionListParams.builder().facet(InstitutionFacetParameter.CITY).build()));

    facetDtos =
        collectionsSearchMapper.institutionFacet(
            InstitutionListParams.builder().facet(InstitutionFacetParameter.TYPE).build());
    assertEquals(2, facetDtos.size());
    assertEquals(1, facetDtos.stream().filter(f -> f.getCount() == 2).count());
    assertEquals(1, facetDtos.stream().filter(f -> f.getCount() == 1).count());
    assertEquals(
        2,
        collectionsSearchMapper.institutionFacetCardinality(
            InstitutionListParams.builder().facet(InstitutionFacetParameter.TYPE).build()));

    facetDtos =
        collectionsSearchMapper.institutionFacet(
            InstitutionListParams.builder().facet(InstitutionFacetParameter.DISCIPLINE).build());
    assertEquals(2, facetDtos.size());
    assertEquals(2, facetDtos.stream().filter(f -> f.getCount() == 1).count());
    assertEquals(
        2,
        collectionsSearchMapper.institutionFacetCardinality(
            InstitutionListParams.builder().facet(InstitutionFacetParameter.DISCIPLINE).build()));
  }

  @Test
  public void hierarchicalFacetsTest() {
    // Get facet IDs from the setup data
    Long biologicalFacetId = grScicollVocabConceptMapper.getConceptKeyByVocabularyAndName("Discipline", "Biological");
    Long archaeologicalFacetId = grScicollVocabConceptMapper.getConceptKeyByVocabularyAndName("CollectionContentType", "Archaeological");
    Long sampleDriedFacetId = grScicollVocabConceptMapper.getConceptKeyByVocabularyAndName("PreservationType", "SampleDried");
    Long storageIndoorsFacetId = grScicollVocabConceptMapper.getConceptKeyByVocabularyAndName("PreservationType", "StorageIndoors");

    // Create test collection with hierarchical facet data
    UUID c1Key = UUID.randomUUID();
    Collection c1 = new Collection();
    c1.setKey(c1Key);
    c1.setCode("hierarchical-test-1");
    c1.setName("Hierarchical Test Collection 1");
    c1.setCreatedBy("test");
    c1.setModifiedBy("test");
    c1.setContentTypes(Arrays.asList("Biological", "Archaeological"));
    c1.setPreservationTypes(Arrays.asList("SampleDried", "StorageIndoors"));

    collectionMapper.create(c1);

    // Create facet links manually since EventListener might not be active in tests
    grScicollVocabConceptMapper.insertCollectionConcept(c1Key, biologicalFacetId);
    grScicollVocabConceptMapper.insertCollectionConcept(c1Key, archaeologicalFacetId);
    grScicollVocabConceptMapper.insertCollectionConcept(c1Key, sampleDriedFacetId);
    grScicollVocabConceptMapper.insertCollectionConcept(c1Key, storageIndoorsFacetId);

    // Test hierarchical content type facets
    List<FacetDto> facetDtos =
        collectionsSearchMapper.collectionFacet(
            DescriptorsListParams.builder()
                .facet(CollectionFacetParameter.CONTENT_TYPE)
                .build());

    // Should return hierarchical facet counts, not array-based
    assertTrue(facetDtos.size() > 0, "Content type facets should be returned");

    // Test hierarchical preservation type facets
    facetDtos =
        collectionsSearchMapper.collectionFacet(
            DescriptorsListParams.builder()
                .facet(CollectionFacetParameter.PRESERVATION_TYPE)
                .build());

    assertTrue(facetDtos.size() > 0, "Preservation type facets should be returned");

    // Verify cardinality calculation for hierarchical facets
    long cardinality = collectionsSearchMapper.collectionFacetCardinality(
        DescriptorsListParams.builder()
            .facet(CollectionFacetParameter.CONTENT_TYPE)
            .build());
    assertTrue(cardinality > 0, "Content type cardinality should be > 0");

    cardinality = collectionsSearchMapper.collectionFacetCardinality(
        DescriptorsListParams.builder()
            .facet(CollectionFacetParameter.PRESERVATION_TYPE)
            .build());
    assertTrue(cardinality > 0, "Preservation type cardinality should be > 0");

    // Clean up
    collectionMapper.delete(c1Key);
  }

  @Test
  public void hierarchicalFacetFilteringTest() {
    // Get facet IDs from the setup data
    Long archaeologicalFacetId = grScicollVocabConceptMapper.getConceptKeyByVocabularyAndName("CollectionContentType", "Archaeological");
    Long sampleDriedFacetId = grScicollVocabConceptMapper.getConceptKeyByVocabularyAndName("PreservationType", "SampleDried");
    Long storageIndoorsFacetId = grScicollVocabConceptMapper.getConceptKeyByVocabularyAndName("PreservationType", "StorageIndoors");

    // Create test collections with specific content types for hierarchical testing
    UUID c3Key = UUID.randomUUID();
    Collection c3 = new Collection();
    c3.setKey(c3Key);
    c3.setCode("c3");
    c3.setName("Hierarchical Test Collection");
    c3.setCreatedBy("test");
    c3.setModifiedBy("test");

    // Set content types that should create facet links
    c3.setContentTypes(List.of("Archaeological"));
    c3.setPreservationTypes(Arrays.asList("SampleDried", "StorageIndoors"));

    collectionMapper.create(c3);

    // Create facet links manually since EventListener might not be active in tests
    grScicollVocabConceptMapper.insertCollectionConcept(c3Key, archaeologicalFacetId);
    grScicollVocabConceptMapper.insertCollectionConcept(c3Key, sampleDriedFacetId);
    grScicollVocabConceptMapper.insertCollectionConcept(c3Key, storageIndoorsFacetId);


    // Test that hierarchical filtering works with facet-based approach
    List<CollectionSearchDto> results = collectionsSearchMapper.searchCollections(
        DescriptorsListParams.builder()
            .contentTypes(List.of("Archaeological"))
            .build());

    // Should find collections with Archaeological content type via facet links
    assertFalse(results.isEmpty());
    assertTrue(results.stream().anyMatch(r -> r.getKey().equals(c3Key)));

    // Test preservation type filtering
    results = collectionsSearchMapper.searchCollections(
        DescriptorsListParams.builder()
            .preservationTypes(List.of("SampleDried"))
            .build());

    assertFalse(results.isEmpty());
    assertTrue(results.stream().anyMatch(r -> r.getKey().equals(c3Key)));

    // Test combined filtering
    results = collectionsSearchMapper.searchCollections(
        DescriptorsListParams.builder()
            .contentTypes(List.of("Archaeological"))
            .preservationTypes(List.of("SampleDried"))
            .build());

    assertFalse(results.isEmpty());
    assertTrue(results.stream().anyMatch(r -> r.getKey().equals(c3Key)));

    // Clean up
    collectionMapper.delete(c3Key);
  }
}
