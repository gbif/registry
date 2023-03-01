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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

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
