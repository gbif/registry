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
package org.gbif.registry.ws.it.collections.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.gbif.api.model.collections.request.CollectionDescriptorsSearchRequest;
import org.gbif.api.model.collections.request.InstitutionFacetedSearchRequest;
import org.gbif.api.model.collections.search.CollectionFacet;
import org.gbif.api.model.collections.search.CollectionSearchResponse;
import org.gbif.api.model.collections.search.CollectionsFullSearchResponse;
import org.gbif.api.model.collections.search.FacetedSearchResponse;
import org.gbif.api.model.collections.search.Highlight;
import org.gbif.api.model.collections.search.InstitutionSearchResponse;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.collections.CollectionFacetParameter;
import org.gbif.api.vocabulary.collections.InstitutionFacetParameter;
import org.gbif.registry.service.collections.CollectionsSearchService;
import org.gbif.registry.ws.client.collections.CollectionsSearchClient;
import org.gbif.registry.ws.it.fixtures.RequestTestFixture;
import org.gbif.registry.ws.it.fixtures.TestConstants;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;

public class CollectionsSearchResourceTest extends BaseResourceIT {

  @MockBean private CollectionsSearchService collectionsSearchService;

  private final CollectionsSearchClient collectionsSearchClient;

  @Autowired
  public CollectionsSearchResourceTest(
      SimplePrincipalProvider simplePrincipalProvider,
      RequestTestFixture requestTestFixture,
      @LocalServerPort int localServerPort) {
    super(simplePrincipalProvider, requestTestFixture);
    this.collectionsSearchClient =
        prepareClient(
            TestConstants.TEST_GRSCICOLL_ADMIN, localServerPort, CollectionsSearchClient.class);
  }

  @Test
  public void searchTest() {
    String q = "foo";
    boolean hl = true;
    int limit = 10;

    CollectionsFullSearchResponse response = new CollectionsFullSearchResponse();
    response.setCode("c1");
    response.setInstitutionKey(UUID.randomUUID());
    Highlight highlight = new Highlight();
    highlight.setField("field1");
    highlight.setSnippet("snippet");
    response.setHighlights(Collections.singleton(highlight));

    when(collectionsSearchService.search(
            q, hl, null, null, Collections.singletonList(Country.SPAIN), limit))
        .thenReturn(Collections.singletonList(response));

    List<CollectionsFullSearchResponse> responseReturned =
        collectionsSearchClient.searchCrossEntities(
            q, hl, null, null, Collections.singletonList(Country.SPAIN), limit);
    assertEquals(1, responseReturned.size());
    assertEquals(response, responseReturned.get(0));
  }

  @Test
  public void searchInstitutionsTest() {
    InstitutionSearchResponse response = new InstitutionSearchResponse();
    response.setCode("c1");
    response.setKey(UUID.randomUUID());
    Highlight highlight = new Highlight();
    highlight.setField("field1");
    highlight.setSnippet("snippet");
    response.setHighlights(Collections.singleton(highlight));

    when(collectionsSearchService.searchInstitutions(any()))
        .thenReturn(new FacetedSearchResponse<>(0, 20, 1L, Collections.singletonList(response)));

    FacetedSearchResponse<InstitutionSearchResponse, InstitutionFacetParameter> responseReturned =
        collectionsSearchClient.searchInstitutions(
            InstitutionFacetedSearchRequest.builder().build());
    assertEquals(1, responseReturned.getResults().size());
    assertEquals(response, responseReturned.getResults().get(0));
  }

  @Test
  public void searchCollectionsTest() {
    CollectionSearchResponse response = new CollectionSearchResponse();
    response.setCode("c1");
    response.setKey(UUID.randomUUID());
    Highlight highlight = new Highlight();
    highlight.setField("field1");
    highlight.setSnippet("snippet");
    response.setHighlights(Collections.singleton(highlight));

    when(collectionsSearchService.searchCollections(any()))
        .thenReturn(new FacetedSearchResponse<>(0, 20, 1L, Collections.singletonList(response)));

    FacetedSearchResponse<CollectionSearchResponse, CollectionFacetParameter> responseReturned =
        collectionsSearchClient.searchCollections(
            CollectionDescriptorsSearchRequest.builder().build());
    assertEquals(1, responseReturned.getResults().size());
    assertEquals(response, responseReturned.getResults().get(0));
  }

  @Test
  public void facetSearchInstitutionsTest() {
    InstitutionFacetedSearchRequest searchRequest =
        InstitutionFacetedSearchRequest.builder()
            .facets(Set.of(InstitutionFacetParameter.COUNTRY))
            .offset(0L)
            .limit(20)
            .build();

    InstitutionSearchResponse response = new InstitutionSearchResponse();
    response.setCode("c1");
    response.setKey(UUID.randomUUID());

    CollectionFacet<InstitutionFacetParameter> facet = new CollectionFacet<>();
    facet.setCardinality(1);
    facet.setField(InstitutionFacetParameter.COUNTRY);
    facet.setCounts(List.of(new CollectionFacet.Count("ES", 2L)));

    FacetedSearchResponse<InstitutionSearchResponse, InstitutionFacetParameter> searchResponse =
        new FacetedSearchResponse<>(
            new PagingRequest(),
            1L,
            Collections.singletonList(response),
            Collections.singletonList(facet));

    when(collectionsSearchService.searchInstitutions(searchRequest)).thenReturn(searchResponse);

    FacetedSearchResponse<InstitutionSearchResponse, InstitutionFacetParameter> responseReturned =
        collectionsSearchClient.searchInstitutions(searchRequest);
    assertEquals(1, responseReturned.getResults().size());
    assertEquals(response, responseReturned.getResults().get(0));
    assertEquals(1, responseReturned.getFacets().size());
    assertEquals(facet, responseReturned.getFacets().get(0));
  }

  @Test
  public void facetSearchCollectionsTest() {
    CollectionDescriptorsSearchRequest searchRequest =
        CollectionDescriptorsSearchRequest.builder()
            .facets(Set.of(CollectionFacetParameter.COUNTRY))
            .offset(0L)
            .limit(20)
            .build();

    CollectionSearchResponse response = new CollectionSearchResponse();
    response.setCode("c1");
    response.setKey(UUID.randomUUID());

    CollectionFacet<CollectionFacetParameter> facet = new CollectionFacet<>();
    facet.setCardinality(1);
    facet.setField(CollectionFacetParameter.COUNTRY);
    facet.setCounts(List.of(new CollectionFacet.Count("ES", 2L)));

    FacetedSearchResponse<CollectionSearchResponse, CollectionFacetParameter> searchResponse =
        new FacetedSearchResponse<>(
            new PagingRequest(),
            1L,
            Collections.singletonList(response),
            Collections.singletonList(facet));

    when(collectionsSearchService.searchCollections(searchRequest)).thenReturn(searchResponse);

    FacetedSearchResponse<CollectionSearchResponse, CollectionFacetParameter> responseReturned =
        collectionsSearchClient.searchCollections(searchRequest);
    assertEquals(1, responseReturned.getResults().size());
    assertEquals(response, responseReturned.getResults().get(0));
    assertEquals(1, responseReturned.getFacets().size());
    assertEquals(facet, responseReturned.getFacets().get(0));
  }
}
