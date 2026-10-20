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

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;

import org.forester.io.parsers.phyloxml.PhyloXmlParser;
import org.forester.phylogeny.Phylogeny;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatasetPhylogenyServiceTest {

  private static final UUID DATASET_KEY = UUID.randomUUID();
  private static final int MAX_TREES_PER_FILE = 2;

  @TempDir Path baseDirectory;

  private DatasetPhylogenyService service;
  private Path datasetDirectory;

  @BeforeEach
  public void setUp() throws IOException {
    service = new DatasetPhylogenyService(baseDirectory.toString(), MAX_TREES_PER_FILE);
    datasetDirectory = Files.createDirectory(baseDirectory.resolve(DATASET_KEY.toString()));

    try (InputStream in =
        DatasetPhylogenyServiceTest.class.getResourceAsStream("/phylogeny/example_tree.nex")) {
      Files.copy(in, datasetDirectory.resolve("example_tree.nex"));
    }
    Files.writeString(datasetDirectory.resolve("tree.nwk"), "((A:1,B:2)AB:3,C:4);");
    Files.writeString(
        datasetDirectory.resolve("posterior.trees"),
        "#NEXUS\nBEGIN TREES;\n"
            + "  TREE t0 = ((A:1,B:1):1,C:2);\n"
            + "  TREE t1 = ((A:1,C:1):1,B:2);\n"
            + "  TREE t2 = ((B:1,C:1):1,A:2);\n"
            + "END;\n");
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

    assertEquals(5, files.size());
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
    assertEquals(0.51, tree.getHeight(), 0.0001);

    PhylogenyFile newick = files.get("tree.nwk");
    assertTrue(newick.isValid());
    assertEquals(PhylogenyFormat.NEWICK, newick.getFormat());
    assertEquals(3, newick.getTrees().get(0).getExternalNodeCount());
    assertFalse(newick.isTreesTruncated());

    PhylogenyFile posterior = files.get("posterior.trees");
    assertTrue(posterior.isValid());
    assertEquals(MAX_TREES_PER_FILE, posterior.getTrees().size());
    assertTrue(posterior.isTreesTruncated());

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
    assertFalse(json.get("trees").get(0).has("externalNodeNames"));
  }

  @Test
  public void testListIsRefreshedWhenFileChanges() throws IOException {
    Path file = datasetDirectory.resolve("tree.nwk");
    assertEquals(3, newickExternalNodeCount());

    Files.writeString(file, "((A:1,B:2)AB:3,(C:4,D:5):1);");
    // the modification time can have a coarse resolution
    Files.setLastModifiedTime(file, FileTime.from(Instant.now().plusSeconds(10)));
    assertEquals(4, newickExternalNodeCount());
  }

  private int newickExternalNodeCount() {
    return service.list(DATASET_KEY).stream()
        .filter(f -> f.getFileName().equals("tree.nwk"))
        .findFirst()
        .orElseThrow()
        .getTrees()
        .get(0)
        .getExternalNodeCount();
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
  public void testPhyloXmlDetection() throws IOException {
    String declaration = "<?xml version=\"1.0\"?>";
    String tree = "<phylogeny><clade><clade/><clade/></clade></phylogeny>";

    Files.write(
        datasetDirectory.resolve("utf16.xml"),
        ("<?xml version=\"1.0\" encoding=\"UTF-16\"?>"
                + "<phyloxml xmlns=\"http://www.phyloxml.org\">" + tree + "</phyloxml>")
            .getBytes(StandardCharsets.UTF_16));
    Files.writeString(
        datasetDirectory.resolve("prefixed.xml"),
        declaration + "<px:phyloxml xmlns:px=\"http://www.phyloxml.org\"/>");
    Files.writeString(
        datasetDirectory.resolve("long-comment.xml"),
        declaration + "<!-- " + "x".repeat(4096) + " --><phyloxml>" + tree + "</phyloxml>");
    // DTDs and external entities are not resolved
    Files.writeString(
        datasetDirectory.resolve("external-dtd.xml"),
        declaration
            + "<!DOCTYPE phyloxml SYSTEM \"http://localhost:1/phyloxml.dtd\">"
            + "<phyloxml>" + tree + "</phyloxml>");
    Files.writeString(
        datasetDirectory.resolve("external-entity.xml"),
        declaration
            + "<!DOCTYPE phyloxml [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>"
            + "<phyloxml>&x;</phyloxml>");

    // not phylogenies
    Files.writeString(
        datasetDirectory.resolve("commented.xml"),
        declaration + "<!-- <phyloxml --><eml:eml xmlns:eml=\"eml://ecoinformatics.org/eml-2.1.1\"/>");
    Files.writeString(
        datasetDirectory.resolve("other-namespace.xml"),
        declaration + "<phyloxml xmlns=\"http://example.org/other\"/>");
    Files.writeString(datasetDirectory.resolve("malformed.xml"), "not xml <<< <phyloxml");

    for (String fileName :
        List.of(
            "utf16.xml", "prefixed.xml", "long-comment.xml", "external-dtd.xml",
            "external-entity.xml")) {
      assertTrue(service.getFile(DATASET_KEY, fileName).isPresent(), fileName);
    }
    for (String fileName : List.of("commented.xml", "other-namespace.xml", "malformed.xml")) {
      assertTrue(service.getFile(DATASET_KEY, fileName).isEmpty(), fileName);
    }
  }

  @Test
  public void testSymbolicLinksAreIgnored() throws IOException {
    Path outside = Files.writeString(baseDirectory.resolve("secret.txt"), "not a tree");
    Files.createSymbolicLink(datasetDirectory.resolve("link.nwk"), outside);

    assertTrue(service.getFile(DATASET_KEY, "link.nwk").isEmpty());
    assertTrue(service.list(DATASET_KEY).stream().noneMatch(f -> f.getFileName().equals("link.nwk")));
  }

  @Test
  public void testGetTree() throws Exception {
    Phylogeny tree = service.getTree(DATASET_KEY, "example_tree.nex", 0).orElseThrow();
    assertEquals("example_tree", tree.getName());
    assertEquals(
        "((Human:0.12,Chimpanzee:0.1):0.08,(Gorilla:0.15,(Orangutan:0.18,Gibbon:0.2):0.07):0.06,"
            + "((Mouse:0.22,Rat:0.21):0.09,(Chicken:0.3,Lizard:0.28):0.11):0.1);",
        PhylogenyFormat.NEWICK.write(tree));
    assertTrue(PhylogenyFormat.NEXUS.write(tree).startsWith("#NEXUS"));
    assertPhyloXmlDocument(PhylogenyFormat.PHYLOXML.write(tree), "example_tree", 9);

    assertTrue(service.getTree(DATASET_KEY, "example_tree.nex", 1).isEmpty());
    assertTrue(service.getTree(DATASET_KEY, "example_tree.nex", -1).isEmpty());
    assertTrue(service.getTree(DATASET_KEY, "missing.nex", 0).isEmpty());
    assertTrue(service.getTree(DATASET_KEY, "occurrence.txt", 0).isEmpty());
    assertThrows(IOException.class, () -> service.getTree(DATASET_KEY, "broken.tre", 0));

    // trees after the listing limit can be retrieved
    assertEquals("t2", service.getTree(DATASET_KEY, "posterior.trees", 2).orElseThrow().getName());
    assertTrue(service.getTree(DATASET_KEY, "posterior.trees", 3).isEmpty());
  }

  /** A complete phyloXML document, readable by a phyloXML parser. */
  private static void assertPhyloXmlDocument(String xml, String name, int externalNodeCount)
      throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    Element root =
        factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml))).getDocumentElement();
    assertEquals("phyloxml", root.getLocalName());
    assertEquals("http://www.phyloxml.org", root.getNamespaceURI());

    PhyloXmlParser parser = PhyloXmlParser.createPhyloXmlParser();
    parser.setSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    Phylogeny[] parsed = parser.parse();
    assertEquals(1, parsed.length);
    assertEquals(name, parsed[0].getName());
    assertEquals(externalNodeCount, parsed[0].getNumberOfExternalNodes());
  }

  @Test
  public void testGetTipNames() throws IOException {
    PagingResponse<String> all =
        service
            .getTipNames(DATASET_KEY, "example_tree.nex", 0, new PagingRequest(0, 20))
            .orElseThrow();
    assertEquals(9, all.getCount());
    assertEquals(9, all.getResults().size());
    assertEquals("Human", all.getResults().get(0));
    assertTrue(all.isEndOfRecords());

    PagingResponse<String> page =
        service.getTipNames(DATASET_KEY, "tree.nwk", 0, new PagingRequest(1, 1)).orElseThrow();
    assertEquals(3, page.getCount());
    assertEquals(List.of("B"), page.getResults());
    assertFalse(page.isEndOfRecords());

    assertTrue(
        service.getTipNames(DATASET_KEY, "tree.nwk", 1, new PagingRequest(0, 20)).isEmpty());
    assertThrows(
        IOException.class,
        () -> service.getTipNames(DATASET_KEY, "broken.tre", 0, new PagingRequest(0, 20)));
  }
}
