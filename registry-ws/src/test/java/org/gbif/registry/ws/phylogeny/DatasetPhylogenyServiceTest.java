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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.forester.phylogeny.Phylogeny;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatasetPhylogenyServiceTest {

  private static final UUID DATASET_KEY = UUID.randomUUID();

  @TempDir Path baseDirectory;

  private DatasetPhylogenyService service;
  private Path datasetDirectory;

  @BeforeEach
  public void setUp() throws IOException {
    service = new DatasetPhylogenyService(baseDirectory.toString());
    datasetDirectory = Files.createDirectory(baseDirectory.resolve(DATASET_KEY.toString()));

    try (InputStream in =
        DatasetPhylogenyServiceTest.class.getResourceAsStream("/phylogeny/example_tree.nex")) {
      Files.copy(in, datasetDirectory.resolve("example_tree.nex"));
    }
    Files.writeString(datasetDirectory.resolve("tree.nwk"), "((A:1,B:2)AB:3,C:4);");
    Files.writeString(datasetDirectory.resolve("broken.tre"), "garbage ((( not a tree");
    Files.writeString(
        datasetDirectory.resolve("tree.xml"),
        "<?xml version=\"1.0\"?><phyloxml xmlns=\"http://www.phyloxml.org\">"
            + "<phylogeny rooted=\"true\"><name>px</name><clade>"
            + "<clade><name>A</name></clade><clade><name>B</name></clade>"
            + "</clade></phylogeny></phyloxml>");
    // files of the archive that are not phylogenies
    Files.writeString(
        datasetDirectory.resolve("eml.xml"),
        "<?xml version=\"1.0\"?><eml:eml xmlns:eml=\"eml://ecoinformatics.org/eml-2.1.1\"/>");
    Files.writeString(datasetDirectory.resolve("occurrence.txt"), "id\tscientificName\n1\tPuma");
  }

  @Test
  public void testList() {
    Map<String, PhylogenyFile> files =
        service.list(DATASET_KEY).stream()
            .collect(Collectors.toMap(PhylogenyFile::getFileName, Function.identity()));

    assertEquals(4, files.size());
    assertFalse(files.containsKey("eml.xml"));
    assertFalse(files.containsKey("occurrence.txt"));

    PhylogenyFile nexus = files.get("example_tree.nex");
    assertTrue(nexus.isValid());
    assertEquals(PhylogenyFormat.NEXUS, nexus.getFormat());
    assertEquals(1, nexus.getTrees().size());
    PhylogenyTree tree = nexus.getTrees().get(0);
    assertEquals("example_tree", tree.getName());
    assertTrue(tree.getRooted());
    assertEquals(9, tree.getExternalNodeCount());
    assertEquals(3, tree.getRootChildCount());
    assertEquals("Human", tree.getExternalNodeNames().get(0));
    assertEquals(0.51, tree.getHeight(), 0.0001);

    PhylogenyFile newick = files.get("tree.nwk");
    assertTrue(newick.isValid());
    assertEquals(PhylogenyFormat.NEWICK, newick.getFormat());
    assertEquals(List.of("A", "B", "C"), newick.getTrees().get(0).getExternalNodeNames());

    PhylogenyFile phyloXml = files.get("tree.xml");
    assertTrue(phyloXml.isValid());
    assertEquals(PhylogenyFormat.PHYLOXML, phyloXml.getFormat());
    assertEquals("px", phyloXml.getTrees().get(0).getName());

    PhylogenyFile broken = files.get("broken.tre");
    assertFalse(broken.isValid());
    assertNotNull(broken.getError());
  }

  @Test
  public void testJson() {
    PhylogenyFile file =
        service.list(DATASET_KEY).stream()
            .filter(f -> f.getFileName().equals("tree.nwk"))
            .findFirst()
            .orElseThrow();
    JsonNode json = new ObjectMapper().valueToTree(file);

    assertEquals("tree.nwk", json.get("fileName").asText());
    assertTrue(json.get("isValid").asBoolean());
    assertFalse(json.has("valid"));
    assertEquals(3, json.get("trees").get(0).get("externalNodeCount").asInt());
  }

  @Test
  public void testListDatasetWithoutArchive() {
    assertTrue(service.list(UUID.randomUUID()).isEmpty());
  }

  @Test
  public void testGetFile() {
    assertEquals(
        datasetDirectory.resolve("example_tree.nex").toAbsolutePath().normalize(),
        service.getFile(DATASET_KEY, "example_tree.nex").orElseThrow());
    assertTrue(service.getFile(DATASET_KEY, "tree.xml").isPresent());

    // not phylogenies
    assertTrue(service.getFile(DATASET_KEY, "eml.xml").isEmpty());
    assertTrue(service.getFile(DATASET_KEY, "occurrence.txt").isEmpty());
    assertTrue(service.getFile(DATASET_KEY, "missing.nex").isEmpty());
    // outside the dataset directory
    assertTrue(service.getFile(DATASET_KEY, "../" + DATASET_KEY + "/example_tree.nex").isPresent());
    assertTrue(service.getFile(DATASET_KEY, "../other.nex").isEmpty());
    assertTrue(service.getFile(DATASET_KEY, "/etc/passwd.nex").isEmpty());
  }

  @Test
  public void testGetTree() throws IOException {
    Phylogeny tree = service.getTree(DATASET_KEY, "example_tree.nex", 0).orElseThrow();
    assertEquals("example_tree", tree.getName());
    assertEquals(
        "((Human:0.12,Chimpanzee:0.1):0.08,(Gorilla:0.15,(Orangutan:0.18,Gibbon:0.2):0.07):0.06,"
            + "((Mouse:0.22,Rat:0.21):0.09,(Chicken:0.3,Lizard:0.28):0.11):0.1);",
        PhylogenyFormat.NEWICK.write(tree));
    assertTrue(PhylogenyFormat.NEXUS.write(tree).startsWith("#NEXUS"));
    assertTrue(PhylogenyFormat.PHYLOXML.write(tree).contains("<name>example_tree</name>"));

    assertTrue(service.getTree(DATASET_KEY, "example_tree.nex", 1).isEmpty());
    assertTrue(service.getTree(DATASET_KEY, "example_tree.nex", -1).isEmpty());
    assertTrue(service.getTree(DATASET_KEY, "missing.nex", 0).isEmpty());
    assertTrue(service.getTree(DATASET_KEY, "occurrence.txt", 0).isEmpty());
    assertThrows(IOException.class, () -> service.getTree(DATASET_KEY, "broken.tre", 0));
  }
}
