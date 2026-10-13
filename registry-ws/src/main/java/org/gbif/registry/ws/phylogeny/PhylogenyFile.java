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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** A file of a dataset archive that is published as a phylogenetic tree. */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PhylogenyFile {

  @Schema(description = "Name of the file inside the dataset archive.")
  private String fileName;

  @Schema(description = "Size of the file in bytes.")
  private Long size;

  @Schema(description = "Detected format of the file: NEXUS, NEWICK or PHYLOXML.")
  private PhylogenyFormat format;

  @JsonProperty("isValid")
  @Schema(description = "Whether the file could be parsed and contains at least one non-empty tree.")
  private boolean valid;

  @Schema(description = "Reason why the file is not valid.")
  private String error;

  @Schema(description = "Trees contained in the file. A Nexus or phyloXML file can contain several trees.")
  private List<PhylogenyTree> trees = new ArrayList<>();
}
