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

import java.util.List;
import java.util.Set;
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
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("grscicoll")
public interface CollectionsSearchClient {

  @RequestMapping(
      value = "search",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  List<CollectionsFullSearchResponse> searchCrossEntities(
      @SpringQueryMap SearchRequest searchRequest);

  @RequestMapping(
      value = "institution/search",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  FacetedSearchResponse<InstitutionSearchResponse, InstitutionFacetParameter> searchInstitutions(
      @SpringQueryMap InstitutionFacetedSearchRequest searchRequest,
      @RequestParam(value = "facet", required = false) Set<InstitutionFacetParameter> facets);

  default FacetedSearchResponse<InstitutionSearchResponse, InstitutionFacetParameter>
      searchInstitutions(InstitutionFacetedSearchRequest searchRequest) {
    return searchInstitutions(searchRequest, searchRequest.getFacets());
  }

  @RequestMapping(
      value = "collection/search",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  FacetedSearchResponse<CollectionSearchResponse, CollectionFacetParameter> searchCollections(
      @SpringQueryMap CollectionDescriptorsSearchRequest searchRequest,
      @RequestParam(value = "facet", required = false) Set<CollectionFacetParameter> facets);

  default FacetedSearchResponse<CollectionSearchResponse, CollectionFacetParameter>
      searchCollections(@SpringQueryMap CollectionDescriptorsSearchRequest searchRequest) {
    return searchCollections(searchRequest, searchRequest.getFacets());
  }

  default List<CollectionsFullSearchResponse> searchCrossEntities(
      @RequestParam(value = "q", required = false) String query,
      @RequestParam(value = "hl", defaultValue = "false") boolean highlight,
      @RequestParam(value = "entityType", required = false) TypeParam type,
      @RequestParam(value = "displayOnNHCPortal", required = false)
          List<Boolean> displayOnNHCPortal,
      @SpringQueryMap List<Country> country,
      @RequestParam(value = "limit", defaultValue = "20") int limit) {
    return searchCrossEntities(
        SearchRequest.of(query, highlight, type, displayOnNHCPortal, country, limit));
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
