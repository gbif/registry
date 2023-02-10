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

import org.gbif.api.annotation.Trim;
import org.gbif.api.model.collections.lookup.LookupParams;
import org.gbif.api.model.collections.lookup.LookupResult;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.service.collections.lookup.LookupService;
import org.gbif.registry.ws.resources.Docs;

import java.util.UUID;

import javax.annotation.Nullable;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@io.swagger.v3.oas.annotations.tags.Tag(
  name = "Lookup institutions and collections",
  description = "This API provides a service to lookup institutions and collections. It can be used to lookup for " +
    "institutions, collections or both at the same time. Besides the matches, the response also provides information " +
    "to help understand how the match was done.",
  extensions = @io.swagger.v3.oas.annotations.extensions.Extension(
    name = "Order", properties = @ExtensionProperty(name = "Order", value = "1300")))
@RestController
@RequestMapping(value = "grscicoll/lookup", produces = MediaType.APPLICATION_JSON_VALUE)
public class LookupResource {

  private final LookupService lookupService;

  public LookupResource(LookupService lookupService) {
    this.lookupService = lookupService;
  }

  // TODO: MatchStatus explanations.
  @Operation(
    operationId = "lookupCollectionsInstitutions",
    summary = "Lookup collections and institutions")
  @Docs.DefaultQParameter
  @Docs.DefaultHlParameter
  @Docs.DefaultOffsetLimitParameters
  @Parameters(
    value = {
      @Parameter(
        name = "datasetKey",
        description = "Institutions and collections can be linked manually to datasets by using occurrence mappings. " +
          "If the dataset key parameter is set it will be used to try to match an occurrence mapping that contains " +
          "that dataset. This manual mapping only happens if no exact matches were found",
        schema = @Schema(implementation = UUID.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "institutionCode",
        description = "The code of an institution",
        schema = @Schema(implementation = String.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "institutionId",
        description = "The identifier of an institution",
        schema = @Schema(implementation = String.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "ownerInstitutionCode",
        description = "The code of the owner institution. This parameter is only used to detect the cases when the " +
          "institution and the owner institution are different. If that happens, the match is not considered accepted",
        schema = @Schema(implementation = String.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "collectionCode",
        description = "The code of a collection",
        schema = @Schema(implementation = String.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "collectionId",
        description = "The identifier of a collection",
        schema = @Schema(implementation = String.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "country",
        description = "The 2-letter country code (as per ISO-3166-1) of the country.",
        schema = @Schema(implementation = Country.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "verbose",
        description = "If set, it returns the accepted matches and other alternatives that were also found. " +
          "Otherwise, it only returns the accepted ones",
        schema = @Schema(implementation = Boolean.class),
        in = ParameterIn.QUERY),
    })
  @ApiResponse(
    responseCode = "200",
    description = "Search successful")
  @ApiResponse(
    responseCode = "400",
    description = "Invalid search query provided")
  @Trim
  @GetMapping
  public LookupResult lookup(
      @Nullable @RequestParam(value = "datasetKey", required = false) UUID datasetKey,
      @Nullable @RequestParam(value = "institutionCode", required = false) @Trim
          String institutionCode,
      @Nullable @RequestParam(value = "institutionId", required = false) @Trim String institutionId,
      @Nullable @RequestParam(value = "ownerInstitutionCode", required = false) @Trim
          String ownerInstitutionCode,
      @Nullable @RequestParam(value = "collectionCode", required = false) @Trim
          String collectionCode,
      @Nullable @RequestParam(value = "collectionId", required = false) @Trim String collectionId,
      @Nullable Country country,
      @Nullable @RequestParam(value = "verbose", required = false) boolean verbose) {

    LookupParams params = new LookupParams();
    params.setDatasetKey(datasetKey);
    params.setInstitutionCode(institutionCode);
    params.setInstitutionId(institutionId);
    params.setOwnerInstitutionCode(ownerInstitutionCode);
    params.setCollectionCode(collectionCode);
    params.setCollectionId(collectionId);
    params.setCountry(country);
    params.setVerbose(verbose);

    return lookupService.lookup(params);
  }
}
