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
package org.gbif.registry.ws.phylogeny;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** Summary of a single tree, built from {@link org.forester.phylogeny.Phylogeny}. */
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PhylogenyTree {

  @Schema(description = "Position of the tree inside the file, starting at 0.")
  private int index;

  @Schema(description = "Name of the tree.")
  private String name;

  @Schema(description = "Description of the tree.")
  private String description;

  @Schema(description = "Type of the tree, e.g. gene_tree or species_tree (phyloXML only).")
  private String type;

  @Schema(description = "Identifier of the tree, e.g. a TreeBASE identifier.")
  private String identifier;

  @Schema(description = "Provider of the identifier of the tree.")
  private String identifierProvider;

  @Schema(description = "Unit of the branch lengths.")
  private String distanceUnit;

  @Schema(description = "Whether the tree is rooted.")
  private Boolean rooted;

  @Schema(description = "Whether the tree can be re-rooted.")
  private Boolean rerootable;

  @Schema(description = "Whether every internal node has exactly two descendants.")
  private Boolean completelyBinary;

  @Schema(description = "Total number of nodes.")
  private Integer nodeCount;

  @Schema(description = "Number of external nodes (tips/leaves).")
  private Integer externalNodeCount;

  @Schema(description = "Number of internal nodes.")
  private Integer internalNodeCount;

  @Schema(description = "Number of branches.")
  private Integer branchCount;

  @Schema(description = "Number of direct descendants of the root.")
  private Integer rootChildCount;

  @Schema(description = "Maximum distance from the root to a tip, using branch lengths.")
  private Double height;

  @Schema(description = "Names of the external nodes (tips/leaves).")
  private List<String> externalNodeNames;
}
