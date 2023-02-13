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
package org.gbif.registry.ws.resources;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

public class Docs {
  /**
   * Default key parameter for entity requests.
   */
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Parameter(
    name = "key",
    description = "The key of the entity (dataset, organization, network etc.)",
    in = ParameterIn.PATH)
  public @interface DefaultEntityKeyParameter {}

  /**
   * Default "q=" search parameter
   */
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Parameter(
    name = "q",
    description =
      "Simple full text search parameter. The value for this parameter can be a simple word or a phrase. Wildcards are not supported",
    schema = @Schema(implementation = String.class),
    in = ParameterIn.QUERY)
  public @interface DefaultQParameter {}

  /**
   * Default highlight parameter
   */
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Parameter(
    name = "hl",
    description = "Set `hl=true` to highlight terms matching the query when in fulltext search fields. The highlight " +
      "will be an emphasis tag of class `gbifH1`.",
    schema = @Schema(implementation = Boolean.class),
    in = ParameterIn.QUERY)
  public @interface DefaultHlParameter {}

  /**
   * The usual paging (limit and offset) parameters
   */
  @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @Parameters(
    value = {
      @Parameter(
        name = "limit",
        description = "Controls the number of results in the page. Using too high a value will be overwritten with the " +
          "default maximum threshold, depending on the service. Sensible defaults are used so this may be omitted.",
        schema = @Schema(implementation = Integer.class, minimum = "0"),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "offset",
        description = "Determines the offset for the search results. A limit of 20 and offset of 40 will get the third " +
          "page of 20 results. Some services have a maximum offset.",
        schema = @Schema(implementation = Integer.class, minimum = "0"),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "page",
        hidden = true
      )
    }
  )
  public @interface DefaultOffsetLimitParameters {}

  /**
   * The usual (search) facet parameters
   */
  @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @Parameters(
    value = {
      @Parameter(
        name = "facet",
        description =
          "A facet name used to retrieve the most frequent values for a field. Facets are allowed for all the parameters except for: eventDate, geometry, lastInterpreted, locality, organismId, stateProvince, waterBody. This parameter may by repeated to request multiple facets, as in this example /occurrence/search?facet=datasetKey&facet=basisOfRecord&limit=0",
        schema = @Schema(implementation = String.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "facetMincount",
        description =
          "Used in combination with the facet parameter. Set facetMincount={#} to exclude facets with a count less than {#}, e.g. /search?facet=type&limit=0&facetMincount=10000 only shows the type value 'OCCURRENCE' because 'CHECKLIST' and 'METADATA' have counts less than 10000.",
        schema = @Schema(implementation = Integer.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "facetMultiselect",
        description =
          "Used in combination with the facet parameter. Set facetMultiselect=true to still return counts for values that are not currently filtered, e.g. /search?facet=type&limit=0&type=CHECKLIST&facetMultiselect=true still shows type values 'OCCURRENCE' and 'METADATA' even though type is being filtered by type=CHECKLIST",
        schema = @Schema(implementation = Boolean.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "facetLimit",
        description =
          "Facet parameters allow paging requests using the parameters facetOffset and facetLimit",
        schema = @Schema(implementation = Integer.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "facetOffset",
        description =
          "Facet parameters allow paging requests using the parameters facetOffset and facetLimit",
        schema = @Schema(implementation = Integer.class, minimum = "0"),
        in = ParameterIn.QUERY)
    }
  )
  public @interface DefaultFacetParameters {}

  /**
   * Documents responses to every read-only operation on subentities: comments, tags, machine tags, etc.
   */
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @ApiResponses({
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
    @ApiResponse(responseCode = "404", description = "Entity or subentity not found", content = @Content),
    @ApiResponse(responseCode = "5XX", description = "System failure – try again", content = @Content)})
  public @interface DefaultUnsuccessfulReadResponses {}

  /**
   * Documents responses to every write operation on subentities: comments, tags, machine tags, etc.
   */
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @ApiResponses({
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)})
  public @interface DefaultUnsuccessfulWriteResponses {}
}
