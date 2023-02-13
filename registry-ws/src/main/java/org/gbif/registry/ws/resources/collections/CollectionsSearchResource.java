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

import org.gbif.api.model.collections.search.CollectionsSearchResponse;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.domain.collections.TypeParam;
import org.gbif.registry.search.dataset.service.collections.CollectionsSearchService;
import org.gbif.registry.ws.resources.Docs;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@io.swagger.v3.oas.annotations.tags.Tag(
  name = "Search institutions and collections",
  description = "This API provides a service to search institutions and collections. It searches in both institutions " +
    "and collections and it highlights the matching fields (optional).",
  extensions = @io.swagger.v3.oas.annotations.extensions.Extension(
    name = "Order", properties = @ExtensionProperty(name = "Order", value = "1400")))
@RestController
@RequestMapping(value = "grscicoll/search", produces = MediaType.APPLICATION_JSON_VALUE)
public class CollectionsSearchResource {

  private final CollectionsSearchService collectionsSearchService;

  public CollectionsSearchResource(CollectionsSearchService collectionsSearchService) {
    this.collectionsSearchService = collectionsSearchService;
  }

  @Operation(
    operationId = "searchCollectionsInstitutions",
    summary = "Search collections and institutions")
  @Docs.DefaultQParameter
  @Docs.DefaultHlParameter
  @Docs.DefaultOffsetLimitParameters
  @Parameters(
    value = {
      @Parameter(
        name = "entityType",
        description = "Code of a GrSciColl institution or collection",
        schema = @Schema(implementation = String.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "displayOnNHCPortal",
        hidden = true),
      @Parameter(
        name = "country",
        description = "The 2-letter country code (as per ISO-3166-1) of the country.",
        schema = @Schema(implementation = Country.class),
        in = ParameterIn.QUERY,
        explode = Explode.FALSE)})
  @ApiResponse(
    responseCode = "200",
    description = "Search successful")
  @ApiResponse(
    responseCode = "400",
    description = "Invalid search query provided")
  @GetMapping
  public List<CollectionsSearchResponse> searchCollections(
      @RequestParam(value = "q", required = false) String query,
      @RequestParam(value = "hl", defaultValue = "false") boolean highlight,
      @RequestParam(value = "entityType", required = false) TypeParam type,
      @RequestParam(value = "displayOnNHCPortal", required = false) Boolean displayOnNHCPortal,
      Country country,
      @RequestParam(value = "limit", defaultValue = "20") int limit) {
    return collectionsSearchService.search(
        query, highlight, type, displayOnNHCPortal, country, limit);
  }
}
