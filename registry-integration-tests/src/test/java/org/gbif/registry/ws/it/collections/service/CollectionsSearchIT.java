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
package org.gbif.registry.ws.it.collections.service;

import static org.gbif.registry.domain.collections.TypeParam.COLLECTION;
import static org.gbif.registry.domain.collections.TypeParam.INSTITUTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.AlternativeCode;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.request.CollectionDescriptorsSearchRequest;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.collections.search.CollectionFacet;
import org.gbif.api.model.collections.search.CollectionSearchResponse;
import org.gbif.api.model.collections.search.CollectionsFullSearchResponse;
import org.gbif.api.model.collections.search.FacetedSearchResponse;
import org.gbif.api.model.collections.search.InstitutionSearchResponse;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.DescriptorsService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.SortOrder;
import org.gbif.api.vocabulary.TypeStatus;
import org.gbif.api.vocabulary.collections.CollectionFacetParameter;
import org.gbif.api.vocabulary.collections.CollectionsSortField;
import org.gbif.api.vocabulary.collections.InstitutionFacetParameter;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.service.collections.CollectionsSearchService;
import org.gbif.registry.test.mocks.NubResourceClientMock;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/** Tests the {@link CollectionsSearchService} * */
public class CollectionsSearchIT extends BaseServiceIT {

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = new TestCaseDatabaseInitializer();

  private final CollectionsSearchService searchService;
  private final InstitutionService institutionService;
  private final CollectionService collectionService;
  private final DescriptorsService descriptorsService;

  private final Institution i1 = new Institution();
  private final Institution i11 = new Institution();
  private final Institution i2 = new Institution();
  private final Collection c1 = new Collection();
  private final Collection c2 = new Collection();
  private final Collection c3 = new Collection();

  @Autowired
  public CollectionsSearchIT(
      SimplePrincipalProvider simplePrincipalProvider,
      CollectionsSearchService collectionsSearchService,
      InstitutionService institutionService,
      CollectionService collectionService,
      DescriptorsService descriptorsService) {
    super(simplePrincipalProvider);
    this.searchService = collectionsSearchService;
    this.institutionService = institutionService;
    this.collectionService = collectionService;
    this.descriptorsService = descriptorsService;
  }

  @SneakyThrows
  @BeforeEach
  public void loadData() {
    i1.setCode("I1");
    i1.setName("Institution 1");
    i1.setTypes(Arrays.asList("t1", "t2"));
    i1.setDisciplines(Arrays.asList("d1", "d2"));
    Address addressI1 = new Address();
    addressI1.setCountry(Country.AFGHANISTAN);
    addressI1.setAddress("foo street");
    i1.setAddress(addressI1);
    i1.setDisplayOnNHCPortal(false);
    institutionService.create(i1);

    i11.setCode("I11");
    i11.setName("I11");
    institutionService.create(i11);

    i2.setCode("I2");
    i2.setName("Institution 2");
    i2.setDescription("different than i1");
    i2.setTypes(Collections.singletonList("t2"));
    i2.setDisciplines(Collections.singletonList("d2"));
    i2.setAlternativeCodes(Collections.singletonList(new AlternativeCode("II2", "test")));
    i2.getIdentifiers().add(new Identifier(IdentifierType.LSID, "lsid-inst"));
    Address addressI2 = new Address();
    addressI2.setCountry(Country.UNITED_STATES);
    i2.setMailingAddress(addressI2);
    institutionService.create(i2);

    c1.setCode("C1");
    c1.setName("Collection 1");
    c1.setInstitutionKey(i1.getKey());
    Address addressC1 = new Address();
    addressC1.setCountry(Country.SPAIN);
    addressC1.setCity("Oviedo");
    addressC1.setProvince("Asturias");
    addressC1.setAddress("fake street");
    c1.setAddress(addressC1);
    c1.setPreservationTypes(Collections.singletonList("pType1"));
    c1.setNumberSpecimens(4000);
    c1.setDisplayOnNHCPortal(false);
    collectionService.create(c1);

    descriptorsService.createDescriptorGroup(
        StreamUtils.copyToByteArray(
            new ClassPathResource("collections/descriptors.csv").getInputStream()),
        ExportFormat.TSV,
        "My personal descriptor set",
        "description",
        c1.getKey());

    descriptorsService.createDescriptorGroup(
        StreamUtils.copyToByteArray(
            new ClassPathResource("collections/descriptors2.csv").getInputStream()),
        ExportFormat.TSV,
        "My descriptor set 2",
        "description",
        c1.getKey());

    c2.setCode("C2");
    c2.setName("Collection 2");
    c2.setNumberSpecimens(1000);
    c2.setInstitutionKey(i2.getKey());
    c2.setAlternativeCodes(Collections.singletonList(new AlternativeCode("CC2", "test")));
    c2.getIdentifiers().add(new Identifier(IdentifierType.LSID, "lsid-coll"));
    c2.setPreservationTypes(Collections.singletonList("pType2"));
    collectionService.create(c2);

    descriptorsService.createDescriptorGroup(
        StreamUtils.copyToByteArray(
            new ClassPathResource("collections/descriptors3.csv").getInputStream()),
        ExportFormat.TSV,
        "My descriptor set 3",
        "unusual description",
        c2.getKey());

    c3.setCode("C3");
    c3.setName("Third");
    c3.setInstitutionKey(i2.getKey());
    c3.setPreservationTypes(Collections.singletonList("pType1"));

    Address addressC3 = new Address();
    addressC3.setCountry(Country.PORTUGAL);
    c3.setMailingAddress(addressC3);
    collectionService.create(c3);
  }

  @Test
  public void searchByCodeTest() {
    List<CollectionsFullSearchResponse> responses =
        searchService.search("I1", true, null, null, null, 10);
    assertEquals(3, responses.size());
    assertEquals(i1.getKey(), responses.get(0).getKey());
    assertEquals(1, responses.get(0).getHighlights().size());

    responses = searchService.search("i1", true, null, null, null, 10);
    assertEquals(3, responses.size());
    assertEquals(i11.getKey(), responses.get(0).getKey());
    assertEquals(2, responses.get(0).getHighlights().size());
  }

  @Test
  public void searchByNameTest() {
    List<CollectionsFullSearchResponse> responses =
        searchService.search("Collection", true, null, null, null, 10);
    assertEquals(2, responses.size());

    responses = searchService.search("Collection 2", true, null, null, null, 10);
    assertEquals(2, responses.size());
    assertEquals(1, responses.get(0).getHighlights().size());
    assertEquals(1, responses.get(1).getHighlights().size());

    responses = searchService.search("Colllection 1", true, null, null, null, 10);
    assertEquals(2, responses.size());
    assertEquals(1, responses.get(0).getHighlights().size());
    assertEquals(1, responses.get(1).getHighlights().size());
  }

  @Test
  public void searchByAlternativeCodesTest() {
    List<CollectionsFullSearchResponse> responses =
        searchService.search("II2", true, null, null, null, 10);
    assertEquals(1, responses.size());
    assertEquals(i2.getKey(), responses.get(0).getKey());

    responses = searchService.search("test", true, null, null, null, 10);
    assertEquals(0, responses.size());
  }

  @Test
  public void searchByAddressFieldsTest() {
    List<CollectionsFullSearchResponse> responses =
        searchService.search("street", true, null, null, null, 10);
    assertEquals(2, responses.size());

    responses = searchService.search(Country.SPAIN.getIso2LetterCode(), true, null, null, null, 10);
    assertEquals(1, responses.size());
    assertEquals(c1.getKey(), responses.get(0).getKey());
    assertEquals(1, responses.get(0).getHighlights().size());

    responses = searchService.search("oviedo", true, null, null, null, 10);
    assertEquals(1, responses.size());
    assertEquals(c1.getKey(), responses.get(0).getKey());
    assertEquals(1, responses.get(0).getHighlights().size());

    responses = searchService.search("street asturias", true, null, null, null, 10);
    assertEquals(1, responses.size());
    assertEquals(c1.getKey(), responses.get(0).getKey());
    assertEquals(2, responses.get(0).getHighlights().size());
  }

  @Test
  public void searchWithoutHighlightTest() {
    List<CollectionsFullSearchResponse> responses =
        searchService.search("Collection", false, null, null, null, 10);
    assertTrue(responses.get(0).getHighlights().isEmpty());
  }

  @Test
  public void noMatchesTest() {
    List<CollectionsFullSearchResponse> responses =
        searchService.search("nothing", false, null, null, null, 10);
    assertEquals(0, responses.size());

    responses = searchService.search("collection made up", false, null, null, null, 10);
    assertEquals(0, responses.size());

    responses = searchService.search("I1 made up", false, null, null, null, 10);
    assertEquals(0, responses.size());
  }

  @Test
  public void displayOnNHCPortalTest() {
    List<CollectionsFullSearchResponse> responses =
        searchService.search(null, false, null, Collections.singletonList(true), null, 10);
    assertEquals(4, responses.size());

    responses = searchService.search(null, false, null, Collections.singletonList(false), null, 10);
    assertEquals(2, responses.size());
  }

  @Test
  public void typeParamTest() {
    List<CollectionsFullSearchResponse> responses =
        searchService.search(null, false, INSTITUTION, null, null, 10);
    assertEquals(3, responses.size());
    assertTrue(responses.stream().allMatch(d -> d.getType().equals("institution")));

    responses = searchService.search(null, false, COLLECTION, null, null, 10);
    assertEquals(3, responses.size());
    assertTrue(responses.stream().allMatch(d -> d.getType().equals("collection")));

    responses = searchService.search(null, false, null, null, null, 10);
    assertEquals(6, responses.size());
  }

  @Test
  public void countryFilterTest() {
    List<CollectionsFullSearchResponse> responses =
        searchService.search(null, true, null, null, Collections.singletonList(Country.SPAIN), 10);
    assertEquals(1, responses.size());
    assertEquals(c1.getKey(), responses.get(0).getKey());

    responses =
        searchService.search(
            null, true, null, null, Collections.singletonList(Country.DENMARK), 10);
    assertEquals(0, responses.size());

    responses =
        searchService.search(
            null, true, INSTITUTION, null, Collections.singletonList(Country.SPAIN), 10);
    assertEquals(0, responses.size());

    responses =
        searchService.search("I1", true, null, null, Collections.singletonList(Country.SPAIN), 10);
    assertEquals(0, responses.size());

    responses =
        searchService.search("C1", true, null, null, Collections.singletonList(Country.SPAIN), 10);
    assertEquals(1, responses.size());
    assertEquals(c1.getKey(), responses.get(0).getKey());
    assertEquals(1, responses.get(0).getHighlights().size());

    responses =
        searchService.search(
            "C1", true, null, null, Collections.singletonList(Country.DENMARK), 10);
    assertEquals(0, responses.size());
  }

  @Test
  public void searchInstitutionsTest() {
    PagingResponse<InstitutionSearchResponse> response =
        searchService.searchInstitutions(InstitutionSearchRequest.builder().build());
    assertEquals(3, response.getResults().size());
    assertEquals(3, response.getCount());

    assertEquals(
        1,
        searchService
            .searchInstitutions(
                InstitutionSearchRequest.builder()
                    .country(Collections.singletonList(i1.getAddress().getCountry()))
                    .build())
            .getResults()
            .size());

    assertEquals(
        1,
        searchService
            .searchInstitutions(InstitutionSearchRequest.builder().q(i1.getName()).build())
            .getResults()
            .size());

    response =
        searchService.searchInstitutions(
            InstitutionSearchRequest.builder().q("different").hl(true).build());
    assertEquals(1, response.getResults().size());
    assertEquals(1, response.getResults().get(0).getHighlights().size());
  }

  @Test
  public void searchCollectionsTest() {
    assertEquals(
        3,
        searchService
            .searchCollections(CollectionDescriptorsSearchRequest.builder().build())
            .getResults()
            .size());

    assertEquals(
        1,
        searchService
            .searchCollections(CollectionDescriptorsSearchRequest.builder().q("Asturias").build())
            .getResults()
            .size());

    PagingResponse<CollectionSearchResponse> response =
        searchService.searchCollections(
            CollectionDescriptorsSearchRequest.builder().q("Asturias").hl(true).build());
    assertEquals(1, response.getResults().size());
    assertEquals(1, response.getCount());
    assertEquals(1, response.getResults().get(0).getHighlights().size());
    assertTrue(response.getResults().get(0).getDescriptorMatches().isEmpty());

    response =
        searchService.searchCollections(
            CollectionDescriptorsSearchRequest.builder().q("aves").build());
    assertEquals(1, response.getResults().size());
    assertEquals(1, response.getCount());
    assertTrue(response.getResults().get(0).getHighlights().isEmpty());
    assertEquals(2, response.getResults().get(0).getDescriptorMatches().size());

    response =
        searchService.searchCollections(
            CollectionDescriptorsSearchRequest.builder().q("aves").hl(true).build());
    assertEquals(1, response.getResults().size());
    assertEquals(1, response.getCount());
    assertEquals(2, response.getResults().get(0).getDescriptorMatches().size());
    assertEquals(1, response.getResults().get(0).getHighlights().size());

    assertDescriptorSearch(
        1,
        1,
        CollectionDescriptorsSearchRequest.builder()
            .descriptorCountry(Collections.singletonList(Country.UNITED_STATES))
            .build());
    assertDescriptorSearch(
        1,
        2,
        CollectionDescriptorsSearchRequest.builder()
            .descriptorCountry(Arrays.asList(Country.PORTUGAL, Country.UNITED_STATES))
            .build());
    assertDescriptorSearch(
        0,
        0,
        CollectionDescriptorsSearchRequest.builder()
            .descriptorCountry(Collections.singletonList(Country.AFGHANISTAN))
            .build());
    assertDescriptorSearch(
        1, 2, CollectionDescriptorsSearchRequest.builder().individualCount("10, 50").build());
    assertDescriptorSearch(
        1,
        3,
        CollectionDescriptorsSearchRequest.builder()
            .recordedBy(Collections.singletonList("Marcos"))
            .build());
    assertDescriptorSearch(
        1,
        2,
        CollectionDescriptorsSearchRequest.builder()
            .typeStatus(Arrays.asList(TypeStatus.COTYPE.name(), TypeStatus.EPITYPE.name()))
            .build());

    assertDescriptorSearch(
        1,
        null,
        CollectionDescriptorsSearchRequest.builder()
            .usageName(Collections.singletonList(NubResourceClientMock.DEFAULT_USAGE.getName()))
            .build());

    assertDescriptorSearch(
        1,
        1,
        CollectionDescriptorsSearchRequest.builder()
            .descriptorCountry(Collections.singletonList(Country.PORTUGAL))
            .q("cc2")
            .build());

    assertDescriptorSearch(
        2,
        null,
        CollectionDescriptorsSearchRequest.builder()
            .descriptorCountry(Collections.singletonList(Country.DENMARK))
            .build());

    PagingResponse<CollectionSearchResponse> first =
        searchService.searchCollections(
            CollectionDescriptorsSearchRequest.builder()
                .descriptorCountry(Collections.singletonList(Country.DENMARK))
                .limit(1)
                .build());
    assertEquals(1, first.getResults().size());
    PagingResponse<CollectionSearchResponse> second =
        searchService.searchCollections(
            CollectionDescriptorsSearchRequest.builder()
                .descriptorCountry(Collections.singletonList(Country.DENMARK))
                .offset(1L)
                .limit(1)
                .build());
    assertEquals(1, first.getResults().size());
    assertNotEquals(first.getResults().get(0).getKey(), second.getResults().get(0).getKey());

    PagingResponse<CollectionSearchResponse> sorted =
        searchService.searchCollections(
            CollectionDescriptorsSearchRequest.builder()
                .sortBy(CollectionsSortField.NUMBER_SPECIMENS)
                .sortOrder(SortOrder.ASC)
                .offset(0L)
                .limit(1)
                .build());
    assertEquals(c2.getKey(), sorted.getResults().get(0).getKey());
    sorted =
        searchService.searchCollections(
            CollectionDescriptorsSearchRequest.builder()
                .sortBy(CollectionsSortField.NUMBER_SPECIMENS)
                .sortOrder(SortOrder.ASC)
                .offset(1L)
                .limit(1)
                .build());
    assertEquals(c1.getKey(), sorted.getResults().get(0).getKey());
    sorted =
        searchService.searchCollections(
            CollectionDescriptorsSearchRequest.builder()
                .sortBy(CollectionsSortField.NUMBER_SPECIMENS)
                .sortOrder(SortOrder.DESC)
                .offset(0L)
                .limit(1)
                .build());
    assertEquals(c1.getKey(), sorted.getResults().get(0).getKey());

    PagingResponse<CollectionSearchResponse> result =
        searchService.searchCollections(
            CollectionDescriptorsSearchRequest.builder().q("personal").hl(true).build());
    assertEquals(1, result.getResults().size());
    assertEquals(c1.getKey(), result.getResults().get(0).getKey());
    assertEquals(
        "descriptorGroup.title",
        result.getResults().get(0).getHighlights().iterator().next().getField());
    assertTrue(result.getResults().get(0).getDescriptorMatches().isEmpty());

    result =
        searchService.searchCollections(
            CollectionDescriptorsSearchRequest.builder().q("unusual").hl(true).build());
    assertEquals(1, result.getResults().size());
    assertEquals(c2.getKey(), result.getResults().get(0).getKey());
    assertEquals(
        "descriptorGroup.description",
        result.getResults().get(0).getHighlights().iterator().next().getField());
    assertTrue(result.getResults().get(0).getDescriptorMatches().isEmpty());
  }

  @Test
  public void collectionsFacetTest() {
    FacetedSearchResponse<CollectionSearchResponse, CollectionFacetParameter> searchResponse =
        searchService.searchCollections(
            CollectionDescriptorsSearchRequest.builder()
                .facets(Collections.singleton(CollectionFacetParameter.COUNTRY))
                .build());
    assertEquals(3, searchResponse.getResults().size());
    assertEquals(1, searchResponse.getFacets().size());
    CollectionFacet<CollectionFacetParameter> facet = searchResponse.getFacets().get(0);
    assertEquals(2, facet.getCardinality());
    assertEquals(CollectionFacetParameter.COUNTRY, facet.getField());
    assertEquals(2, facet.getCounts().size());
    assertEquals(
        1,
        facet.getCounts().stream()
            .filter(c -> c.getName().equals(Country.SPAIN.getIso2LetterCode()))
            .count());
    assertEquals(
        1,
        facet.getCounts().stream()
            .filter(c -> c.getName().equals(Country.PORTUGAL.getIso2LetterCode()))
            .count());
    assertTrue(facet.getCounts().stream().allMatch(c -> c.getCount() == 1));

    searchResponse =
        searchService.searchCollections(
            CollectionDescriptorsSearchRequest.builder()
                .country(Collections.singletonList(Country.SPAIN))
                .facets(Collections.singleton(CollectionFacetParameter.COUNTRY))
                .build());
    assertEquals(1, searchResponse.getResults().size());
    assertEquals(1, searchResponse.getFacets().size());
    facet = searchResponse.getFacets().get(0);
    assertEquals(1, facet.getCardinality());
    assertEquals(CollectionFacetParameter.COUNTRY, facet.getField());
    assertEquals(1, facet.getCounts().size());
    assertEquals(
        1,
        facet.getCounts().stream()
            .filter(c -> c.getName().equals(Country.SPAIN.getIso2LetterCode()))
            .count());
    assertTrue(facet.getCounts().stream().allMatch(c -> c.getCount() == 1));

    searchResponse =
        searchService.searchCollections(
            CollectionDescriptorsSearchRequest.builder()
                .country(Collections.singletonList(Country.SPAIN))
                .facets(Collections.singleton(CollectionFacetParameter.COUNTRY))
                .multiSelectFacets(true)
                .build());
    assertEquals(1, searchResponse.getResults().size());
    assertEquals(1, searchResponse.getFacets().size());
    facet = searchResponse.getFacets().get(0);
    assertEquals(2, facet.getCardinality());
    assertEquals(CollectionFacetParameter.COUNTRY, facet.getField());
    assertEquals(2, facet.getCounts().size());
    assertEquals(
        1,
        facet.getCounts().stream()
            .filter(c -> c.getName().equals(Country.SPAIN.getIso2LetterCode()))
            .count());
    assertEquals(
        1,
        facet.getCounts().stream()
            .filter(c -> c.getName().equals(Country.PORTUGAL.getIso2LetterCode()))
            .count());
    assertTrue(facet.getCounts().stream().allMatch(c -> c.getCount() == 1));

    searchResponse =
        searchService.searchCollections(
            CollectionDescriptorsSearchRequest.builder()
                .facets(Collections.singleton(CollectionFacetParameter.COUNTRY))
                .facetMinCount(2)
                .build());
    assertEquals(3, searchResponse.getResults().size());
    assertEquals(1, searchResponse.getFacets().size());
    facet = searchResponse.getFacets().get(0);
    assertEquals(2, facet.getCardinality());
    assertEquals(CollectionFacetParameter.COUNTRY, facet.getField());
    assertEquals(0, facet.getCounts().size());

    searchResponse =
        searchService.searchCollections(
            CollectionDescriptorsSearchRequest.builder()
                .facets(Collections.singleton(CollectionFacetParameter.PRESERVATION_TYPE))
                .build());
    assertEquals(3, searchResponse.getResults().size());
    assertEquals(1, searchResponse.getFacets().size());
    facet = searchResponse.getFacets().get(0);
    assertEquals(2, facet.getCardinality());
    assertEquals(CollectionFacetParameter.PRESERVATION_TYPE, facet.getField());
    assertEquals(1, facet.getCounts().stream().filter(c -> c.getCount() == 1).count());
    assertEquals(1, facet.getCounts().stream().filter(c -> c.getCount() == 2).count());

    searchResponse =
        searchService.searchCollections(
            CollectionDescriptorsSearchRequest.builder()
                .facets(
                    Set.of(
                        CollectionFacetParameter.COUNTRY,
                        CollectionFacetParameter.PRESERVATION_TYPE))
                .build());
    assertEquals(2, searchResponse.getFacets().size());
    assertEquals(
        1,
        searchResponse.getFacets().stream()
            .filter(f -> f.getField() == CollectionFacetParameter.COUNTRY)
            .count());
    assertEquals(
        1,
        searchResponse.getFacets().stream()
            .filter(f -> f.getField() == CollectionFacetParameter.PRESERVATION_TYPE)
            .count());

    searchResponse =
        searchService.searchCollections(
            CollectionDescriptorsSearchRequest.builder()
                .facets(
                    Set.of(
                        CollectionFacetParameter.COUNTRY,
                        CollectionFacetParameter.PRESERVATION_TYPE))
                .facetLimit(1)
                .build());
    assertEquals(2, searchResponse.getFacets().size());
    assertTrue(searchResponse.getFacets().stream().allMatch(f -> f.getCounts().size() == 1));

    searchResponse =
        searchService.searchCollections(
            CollectionDescriptorsSearchRequest.builder()
                .facets(
                    Set.of(
                        CollectionFacetParameter.COUNTRY,
                        CollectionFacetParameter.PRESERVATION_TYPE))
                .facetLimit(1)
                .facetPages(
                    Map.of(CollectionFacetParameter.PRESERVATION_TYPE, new PagingRequest(0, 2)))
                .build());
    assertEquals(2, searchResponse.getFacets().size());
    assertEquals(
        1, searchResponse.getFacets().stream().filter(f -> f.getCounts().size() == 1).count());
    assertEquals(
        1, searchResponse.getFacets().stream().filter(f -> f.getCounts().size() == 2).count());

    searchResponse =
        searchService.searchCollections(
            CollectionDescriptorsSearchRequest.builder()
                .facets(
                    Set.of(
                        CollectionFacetParameter.COUNTRY,
                        CollectionFacetParameter.PRESERVATION_TYPE))
                .facetLimit(1)
                .facetPages(
                    Map.of(CollectionFacetParameter.PRESERVATION_TYPE, new PagingRequest(1, 2)))
                .build());
    assertEquals(2, searchResponse.getFacets().size());
    assertTrue(searchResponse.getFacets().stream().allMatch(f -> f.getCounts().size() == 1));
  }

  @Test
  public void institutionFacetsTest() {
    FacetedSearchResponse<InstitutionSearchResponse, InstitutionFacetParameter> searchResponse =
        searchService.searchInstitutions(
            InstitutionSearchRequest.builder()
                .facets(Collections.singleton(InstitutionFacetParameter.TYPE))
                .build());
    assertEquals(3, searchResponse.getResults().size());
    assertEquals(1, searchResponse.getFacets().size());
    CollectionFacet<InstitutionFacetParameter> facet = searchResponse.getFacets().get(0);
    assertEquals(2, facet.getCardinality());
    assertEquals(InstitutionFacetParameter.TYPE, facet.getField());
    assertEquals(2, facet.getCounts().size());
    assertEquals(
        1L,
        facet.getCounts().stream()
            .filter(c -> c.getName().equals("t1"))
            .map(CollectionFacet.Count::getCount)
            .findFirst()
            .orElse(0L));
    assertEquals(
        2L,
        facet.getCounts().stream()
            .filter(c -> c.getName().equals("t2"))
            .map(CollectionFacet.Count::getCount)
            .findFirst()
            .orElse(0L));

    searchResponse =
        searchService.searchInstitutions(
            InstitutionSearchRequest.builder()
                .facets(Collections.singleton(InstitutionFacetParameter.TYPE))
                .country(Collections.singletonList(Country.UNITED_STATES))
                .build());
    assertEquals(1, searchResponse.getResults().size());
    assertEquals(1, searchResponse.getFacets().size());
    facet = searchResponse.getFacets().get(0);
    assertEquals(1, facet.getCardinality());
    assertEquals(InstitutionFacetParameter.TYPE, facet.getField());
    assertEquals(1, facet.getCounts().size());
    assertEquals("t2", facet.getCounts().get(0).getName());
    assertEquals(1, facet.getCounts().get(0).getCount());

    searchResponse =
        searchService.searchInstitutions(
            InstitutionSearchRequest.builder()
                .facets(Collections.singleton(InstitutionFacetParameter.TYPE))
                .country(Collections.singletonList(Country.UNITED_STATES))
                .multiSelectFacets(true)
                .build());
    assertEquals(1, searchResponse.getResults().size());
    assertEquals(1, searchResponse.getFacets().size());
    facet = searchResponse.getFacets().get(0);
    assertEquals(2, facet.getCardinality());
    assertEquals(InstitutionFacetParameter.TYPE, facet.getField());
    assertEquals(2, facet.getCounts().size());
    assertEquals(
        1L,
        facet.getCounts().stream()
            .filter(c -> c.getName().equals("t1"))
            .map(CollectionFacet.Count::getCount)
            .findFirst()
            .orElse(0L));
    assertEquals(
        2L,
        facet.getCounts().stream()
            .filter(c -> c.getName().equals("t2"))
            .map(CollectionFacet.Count::getCount)
            .findFirst()
            .orElse(0L));

    searchResponse =
        searchService.searchInstitutions(
            InstitutionSearchRequest.builder()
                .facets(
                    Set.of(InstitutionFacetParameter.DISCIPLINE, InstitutionFacetParameter.COUNTRY))
                .multiSelectFacets(true)
                .build());
    assertEquals(2, searchResponse.getFacets().size());
    assertEquals(
        1,
        searchResponse.getFacets().stream()
            .filter(f -> f.getField() == InstitutionFacetParameter.COUNTRY)
            .count());
    assertEquals(
        1,
        searchResponse.getFacets().stream()
            .filter(f -> f.getField() == InstitutionFacetParameter.DISCIPLINE)
            .count());

    searchResponse =
        searchService.searchInstitutions(
            InstitutionSearchRequest.builder()
                .facets(
                    Set.of(InstitutionFacetParameter.COUNTRY, InstitutionFacetParameter.DISCIPLINE))
                .facetLimit(1)
                .build());
    assertEquals(2, searchResponse.getFacets().size());
    assertTrue(searchResponse.getFacets().stream().allMatch(f -> f.getCounts().size() == 1));

    searchResponse =
        searchService.searchInstitutions(
            InstitutionSearchRequest.builder()
                .facets(
                    Set.of(InstitutionFacetParameter.COUNTRY, InstitutionFacetParameter.DISCIPLINE))
                .facetLimit(1)
                .facetPages(Map.of(InstitutionFacetParameter.DISCIPLINE, new PagingRequest(0, 2)))
                .build());
    assertEquals(2, searchResponse.getFacets().size());
    assertEquals(
        1, searchResponse.getFacets().stream().filter(f -> f.getCounts().size() == 1).count());
    assertEquals(
        1, searchResponse.getFacets().stream().filter(f -> f.getCounts().size() == 2).count());
  }

  private void assertDescriptorSearch(
      int expectedResults,
      Integer expectedDescriptors,
      CollectionDescriptorsSearchRequest searchRequest) {
    PagingResponse<CollectionSearchResponse> response =
        searchService.searchCollections(searchRequest);
    assertEquals(expectedResults, response.getResults().size());
    assertEquals(expectedResults, response.getCount());
    if (expectedResults > 0) {
      assertTrue(response.getResults().stream().noneMatch(v -> v.getDescriptorMatches().isEmpty()));
    }
    if (expectedResults == 1 && expectedDescriptors != null) {
      assertEquals(expectedDescriptors, response.getResults().get(0).getDescriptorMatches().size());
    }
  }
}
