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
package org.gbif.registry.ws.client.collections;
import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
import lombok.AllArgsConstructor;

import org.gbif.api.model.collections.request.CollectionDescriptorsSearchRequest;
import org.gbif.api.model.collections.request.InstitutionFacetedSearchRequest;
import org.gbif.api.model.collections.search.CollectionSearchResponse;
import org.gbif.api.model.collections.search.CollectionsFullSearchResponse;
import org.gbif.api.model.collections.search.FacetedSearchResponse;
import org.gbif.api.model.collections.search.InstitutionSearchResponse;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.collections.CollectionFacetParameter;
import org.gbif.api.vocabulary.collections.InstitutionFacetParameter;
import org.gbif.registry.domain.collections.TypeParam;

import java.util.List;
import java.util.Set;


public interface CollectionsSearchClient {

  @RequestLine("GET /grscicoll/search")
  @Headers("Accept: application/json")
  List<CollectionsFullSearchResponse> searchCrossEntities(@QueryMap SearchRequest searchRequest);

  @RequestLine("GET /grscicoll/institution/search")
  @Headers("Accept: application/json")
  FacetedSearchResponse<InstitutionSearchResponse, InstitutionFacetParameter> searchInstitutions(
    @QueryMap InstitutionFacetedSearchRequest searchRequest,
    @Param("facet") Set<InstitutionFacetParameter> facets
  );

  default FacetedSearchResponse<InstitutionSearchResponse, InstitutionFacetParameter> searchInstitutions(
    InstitutionFacetedSearchRequest searchRequest
  ) {
    return searchInstitutions(searchRequest, searchRequest.getFacets());
  }

  @RequestLine("GET /grscicoll/collection/search")
  @Headers("Accept: application/json")
  FacetedSearchResponse<CollectionSearchResponse, CollectionFacetParameter> searchCollections(
    @QueryMap CollectionDescriptorsSearchRequest searchRequest,
    @Param("facet") Set<CollectionFacetParameter> facets
  );

  default FacetedSearchResponse<CollectionSearchResponse, CollectionFacetParameter> searchCollections(
    CollectionDescriptorsSearchRequest searchRequest
  ) {
    return searchCollections(searchRequest, searchRequest.getFacets());
  }

  default List<CollectionsFullSearchResponse> searchCrossEntities(
    String query,
    boolean highlight,
    TypeParam type,
    List<Boolean> displayOnNHCPortal,
    List<Country> country,
    int limit
  ) {
    return searchCrossEntities(SearchRequest.of(query, highlight, type, displayOnNHCPortal, country, limit));
  }

  @AllArgsConstructor(staticName = "of")
  class SearchRequest {
    String q;
    boolean hl;
    TypeParam entityType;
    List<Boolean> displayOnNHCPortal;
    List<Country> country;
    int limit;
  }
}

