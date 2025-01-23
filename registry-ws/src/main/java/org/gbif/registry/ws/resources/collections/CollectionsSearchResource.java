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
package org.gbif.registry.ws.resources.collections;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.Arrays;
import java.util.List;
import org.gbif.api.documentation.CommonParameters;
import org.gbif.api.model.collections.request.CollectionDescriptorsSearchRequest;
import org.gbif.api.model.collections.request.InstitutionFacetedSearchRequest;
import org.gbif.api.model.collections.search.CollectionSearchResponse;
import org.gbif.api.model.collections.search.CollectionsFullSearchResponse;
import org.gbif.api.model.collections.search.FacetedSearchResponse;
import org.gbif.api.model.collections.search.InstitutionSearchResponse;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.collections.CollectionFacetParameter;
import org.gbif.registry.domain.collections.TypeParam;
import org.gbif.registry.service.collections.CollectionsSearchService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@io.swagger.v3.oas.annotations.tags.Tag(
    name = "Search institutions and collections",
    description =
        "This API provides a service to search institutions and collections. It searches in both institutions "
            + "and collections and it highlights the matching fields (optional).",
    extensions =
        @io.swagger.v3.oas.annotations.extensions.Extension(
            name = "Order",
            properties = @ExtensionProperty(name = "Order", value = "1400")))
@RestController
@RequestMapping(value = "grscicoll", produces = MediaType.APPLICATION_JSON_VALUE)
public class CollectionsSearchResource {

  private final CollectionsSearchService collectionsSearchService;

  public CollectionsSearchResource(CollectionsSearchService collectionsSearchService) {
    this.collectionsSearchService = collectionsSearchService;
  }

  @Operation(
      operationId = "searchCollectionsInstitutions",
      summary = "Search collections and institutions",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0101")))
  @CommonParameters.QParameter
  @CommonParameters.HighlightParameter
  @Pageable.OffsetLimitParameters
  @Parameters(
      value = {
        @Parameter(
            name = "entityType",
            description = "Code of a GrSciColl institution or collection",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY),
        @Parameter(name = "displayOnNHCPortal", hidden = true),
        @Parameter(
            name = "country",
            description = "The 2-letter country code (as per ISO-3166-1) of the country.",
            schema = @Schema(implementation = Country.class),
            in = ParameterIn.QUERY,
            explode = Explode.FALSE)
      })
  @ApiResponse(responseCode = "200", description = "Search successful")
  @ApiResponse(responseCode = "400", description = "Invalid search query provided")
  @GetMapping("search")
  public List<CollectionsFullSearchResponse> searchCrossEntities(
      @RequestParam(value = "q", required = false) String query,
      @RequestParam(value = "hl", defaultValue = "false") boolean highlight,
      @RequestParam(value = "entityType", required = false) TypeParam type,
      @RequestParam(value = "displayOnNHCPortal", required = false)
          List<Boolean> displayOnNHCPortal,
      Country[] country,
      @RequestParam(value = "limit", defaultValue = "20") int limit) {
    return collectionsSearchService.search(
        query,
        highlight,
        type,
        displayOnNHCPortal,
        country != null ? Arrays.asList(country) : null,
        limit);
  }

  @Operation(
      operationId = "searchInstitutions",
      summary = "Search across institutions",
      description = "Searches for institutions.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0110")))
  @CommonParameters.HighlightParameter
  @InstitutionResource.InstitutionSearchParameters
  @ApiResponse(responseCode = "200", description = "Search successful")
  @ApiResponse(responseCode = "400", description = "Invalid search query provided")
  @GetMapping("institution/search")
  public PagingResponse<InstitutionSearchResponse> searchInstitutions(
      InstitutionFacetedSearchRequest searchRequest) {
    return collectionsSearchService.searchInstitutions(searchRequest);
  }

  @Operation(
      operationId = "searchCollections",
      summary = "Search across collections",
      description = "Searches for collections",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0120")))
  @CollectionResource.CollectionSearchParameters
  @CommonParameters.HighlightParameter
  @ApiResponse(responseCode = "200", description = "Search successful")
  @ApiResponse(responseCode = "400", description = "Invalid search query provided")
  @GetMapping("collection/search")
  public FacetedSearchResponse<CollectionSearchResponse, CollectionFacetParameter>
      searchCollections(CollectionDescriptorsSearchRequest searchRequest) {
    return collectionsSearchService.searchCollections(searchRequest);
  }
}
