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

import java.io.IOException;

import org.forester.phylogeny.Phylogeny;
import org.springframework.http.MediaType;

/** Phylogenetic tree file formats supported by the forester parsers. */
public enum PhylogenyFormat {
  NEXUS(MediaType.TEXT_PLAIN),
  NEWICK(MediaType.TEXT_PLAIN),
  PHYLOXML(MediaType.APPLICATION_XML);

  private final MediaType mediaType;

  PhylogenyFormat(MediaType mediaType) {
    this.mediaType = mediaType;
  }

  public MediaType getMediaType() {
    return mediaType;
  }

  /** Writes a single tree as a complete document in this format. */
  public String write(Phylogeny phylogeny) throws IOException {
    return switch (this) {
      case NEXUS -> phylogeny.toNexus();
      case NEWICK -> phylogeny.toNewHampshire();
      case PHYLOXML -> phylogeny.toPhyloXML(0);
    };
  }
}
