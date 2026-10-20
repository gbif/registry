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

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.forester.io.parsers.IteratingPhylogenyParser;
import org.forester.io.parsers.PhylogenyParser;
import org.forester.io.parsers.nexus.NexusPhylogeniesParser;
import org.forester.io.parsers.phyloxml.PhyloXmlParser;
import org.forester.io.parsers.util.ParserUtils;
import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.iterators.PhylogenyNodeIterator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * Finds and reads the phylogenetic trees published in a dataset archive. Archives are read from
 * the directory where the crawler unpacks them: {@code {datasetsDirectory}/{datasetKey}/}.
 */
@Slf4j
@Service
public class DatasetPhylogenyService {

  // Forester parses almost any text as Newick, so candidate files are selected by extension
  private static final Set<String> TREE_EXTENSIONS =
      Set.of(
          "nex", "nexus", "nxs", "tre", "tree", "trees", "nwk", "newick", "nh", "nhx", "phyloxml",
          "pxml");

  // .xml files are only trees when their root element is phyloxml (eml.xml and meta.xml are not)
  private static final String XML_EXTENSION = "xml";
  private static final String PHYLOXML_ROOT = "phyloxml";
  private static final String PHYLOXML_NAMESPACE = "http://www.phyloxml.org";

  // only the root element is read, DTDs and external entities are never resolved
  private static final XMLInputFactory XML_INPUT_FACTORY = createXmlInputFactory();

  // summaries are small, parsing the files is what's expensive
  private static final int SUMMARY_CACHE_SIZE = 1_000;

  private final Path datasetsDirectory;
  private final int maxTreesPerFile;

  // a modified or replaced file gets a new key, stale entries are evicted by size
  private final Cache<FileVersion, PhylogenyFile> summaries =
      CacheBuilder.newBuilder().maximumSize(SUMMARY_CACHE_SIZE).build();

  public DatasetPhylogenyService(
      @Value("${registry.phylogeny.datasetsDirectory}") String datasetsDirectory,
      @Value("${registry.phylogeny.maxTreesPerFile:100}") int maxTreesPerFile) {
    this.datasetsDirectory = Paths.get(datasetsDirectory).toAbsolutePath().normalize();
    this.maxTreesPerFile = maxTreesPerFile;
  }

  /** Lists and parses the phylogeny files of a dataset; empty if the dataset has no archive. */
  public List<PhylogenyFile> list(UUID datasetKey) {
    Path datasetDirectory = datasetDirectory(datasetKey);
    if (!Files.isDirectory(datasetDirectory)) {
      return new ArrayList<>();
    }
    try (Stream<Path> files = Files.list(datasetDirectory)) {
      return files
          .filter(DatasetPhylogenyService::isPhylogenyFile)
          .sorted(Comparator.comparing(p -> p.getFileName().toString()))
          .map(this::summary)
          .toList();
    } catch (IOException e) {
      throw new IllegalStateException("Error listing files of dataset " + datasetKey, e);
    }
  }

  /** Path of a phylogeny file of a dataset, if it exists. */
  public Optional<Path> getFile(UUID datasetKey, String fileName) {
    Path datasetDirectory = datasetDirectory(datasetKey);
    Path file = datasetDirectory.resolve(fileName).normalize();
    // only files directly inside the dataset directory, no path traversal
    if (!datasetDirectory.equals(file.getParent()) || !isPhylogenyFile(file)) {
      return Optional.empty();
    }
    return Optional.of(file);
  }

  /**
   * A single tree of a phylogeny file, by its position in the file. Empty if the file or the
   * tree doesn't exist.
   *
   * @throws IOException if the file can't be parsed
   */
  public Optional<Phylogeny> getTree(UUID datasetKey, String fileName, int index)
      throws IOException {
    Optional<Path> file = getFile(datasetKey, fileName);
    if (file.isEmpty() || index < 0) {
      return Optional.empty();
    }
    PhylogenyParser parser = createParser(file.get());
    if (parser == null) {
      throw new IOException("Unrecognised phylogeny format");
    }
    try {
      return parseTree(parser, file.get(), index);
    } catch (RuntimeException e) {
      throw new IOException(e.getMessage(), e);
    }
  }

  /**
   * A page of the names of the external nodes (tips) of a tree, in tree order. Empty if the file
   * or the tree doesn't exist.
   *
   * @throws IOException if the file can't be parsed
   */
  public Optional<PagingResponse<String>> getTipNames(
      UUID datasetKey, String fileName, int index, Pageable page) throws IOException {
    Optional<Phylogeny> tree = getTree(datasetKey, fileName, index);
    if (tree.isEmpty()) {
      return Optional.empty();
    }
    Phylogeny phylogeny = tree.get();
    List<String> names = new ArrayList<>();
    if (!phylogeny.isEmpty()) {
      long position = 0;
      long end = page.getOffset() + page.getLimit();
      for (PhylogenyNodeIterator it = phylogeny.iteratorExternalForward();
          it.hasNext() && position < end;
          position++) {
        String name = it.next().getName();
        if (position >= page.getOffset()) {
          names.add(name);
        }
      }
    }
    return Optional.of(
        new PagingResponse<>(page, (long) phylogeny.getNumberOfExternalNodes(), names));
  }

  /** Detects the format of a phylogeny file. */
  public static Optional<PhylogenyFormat> format(Path file) {
    try {
      return Optional.ofNullable(createParser(file)).map(DatasetPhylogenyService::format);
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  private Path datasetDirectory(UUID datasetKey) {
    return datasetsDirectory.resolve(datasetKey.toString());
  }

  private static boolean isPhylogenyFile(Path file) {
    // no symbolic links, they could point to files outside the dataset directory
    if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
      return false;
    }
    String extension = extension(file);
    return TREE_EXTENSIONS.contains(extension)
        || (XML_EXTENSION.equals(extension) && isPhyloXml(file));
  }

  private static String extension(Path file) {
    String name = file.getFileName().toString();
    int dot = name.lastIndexOf('.');
    return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
  }

  /**
   * Whether the root element is phyloXML. The reader detects the encoding and resolves namespace
   * prefixes; comments and processing instructions before the root are skipped.
   */
  private static boolean isPhyloXml(Path file) {
    XMLStreamReader reader = null;
    try (InputStream in = Files.newInputStream(file)) {
      reader = XML_INPUT_FACTORY.createXMLStreamReader(in);
      while (reader.hasNext()) {
        if (reader.next() == XMLStreamConstants.START_ELEMENT) {
          String namespace = reader.getNamespaceURI();
          // files without a namespace are accepted, forester parses them too
          return PHYLOXML_ROOT.equals(reader.getLocalName())
              && (namespace == null
                  || namespace.isEmpty()
                  || PHYLOXML_NAMESPACE.equals(namespace));
        }
      }
      return false;
    } catch (IOException | XMLStreamException e) {
      log.debug("Can't read XML file {}", file, e);
      return false;
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (XMLStreamException e) {
          log.debug("Error closing XML reader of {}", file, e);
        }
      }
    }
  }

  private static XMLInputFactory createXmlInputFactory() {
    XMLInputFactory factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    return factory;
  }

  private static PhylogenyParser createParser(Path file) throws IOException {
    // chooses Newick / NHX / Nexus / phyloXML from the suffix, then the file contents
    return ParserUtils.createParserDependingOnFileType(file.toFile(), true);
  }

  private static PhylogenyFormat format(PhylogenyParser parser) {
    if (parser instanceof NexusPhylogeniesParser) {
      return PhylogenyFormat.NEXUS;
    }
    if (parser instanceof PhyloXmlParser) {
      return PhylogenyFormat.PHYLOXML;
    }
    return PhylogenyFormat.NEWICK;
  }

  private PhylogenyFile summary(Path file) {
    FileVersion version;
    try {
      version = new FileVersion(file, Files.getLastModifiedTime(file), Files.size(file));
    } catch (IOException e) {
      // not cached, the file may be replaced while we read it
      return read(file);
    }
    try {
      return summaries.get(version, () -> read(file));
    } catch (ExecutionException e) {
      throw new IllegalStateException("Error reading phylogeny file " + file, e.getCause());
    }
  }

  private PhylogenyFile read(Path file) {
    PhylogenyFile phylogenyFile = new PhylogenyFile();
    phylogenyFile.setFileName(file.getFileName().toString());
    try {
      phylogenyFile.setSize(Files.size(file));
      PhylogenyParser parser = createParser(file);
      if (parser == null) {
        phylogenyFile.setError("Unrecognised phylogeny format");
        return phylogenyFile;
      }
      phylogenyFile.setFormat(format(parser));
      // one more than the maximum, to know if there are more trees
      List<Phylogeny> phylogenies = parseFile(parser, file, maxTreesPerFile + 1);
      if (phylogenies.size() > maxTreesPerFile) {
        phylogenies = phylogenies.subList(0, maxTreesPerFile);
        phylogenyFile.setTreesTruncated(true);
      }

      List<PhylogenyTree> trees = new ArrayList<>();
      for (int i = 0; i < phylogenies.size(); i++) {
        trees.add(toTree(i, phylogenies.get(i)));
      }
      phylogenyFile.setTrees(trees);

      if (trees.isEmpty()) {
        phylogenyFile.setError("The file does not contain any tree");
      } else if (phylogenies.stream().anyMatch(p -> p == null || p.isEmpty())) {
        phylogenyFile.setError("The file contains empty trees");
      } else {
        phylogenyFile.setValid(true);
      }
    } catch (Exception e) {
      log.debug("Invalid phylogeny file {}", file, e);
      phylogenyFile.setError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
    }
    return phylogenyFile;
  }

  /**
   * Parses at most {@code maxTrees} trees of a file. Nexus and Newick files are parsed one tree at a
   * time and stop there; phyloXML files are always parsed completely.
   */
  private static List<Phylogeny> parseFile(PhylogenyParser parser, Path file, int maxTrees)
      throws IOException {
    // a parser instance per call, ParserBasedPhylogenyFactory is synchronized on a singleton
    parser.setSource(file.toFile());
    List<Phylogeny> phylogenies = new ArrayList<>();
    if (parser instanceof IteratingPhylogenyParser iterating) {
      while (phylogenies.size() < maxTrees && iterating.hasNext()) {
        phylogenies.add(iterating.next());
      }
      return phylogenies;
    }
    Phylogeny[] parsed = parser.parse();
    if (parsed != null) {
      phylogenies.addAll(Arrays.asList(parsed).subList(0, Math.min(parsed.length, maxTrees)));
    }
    return phylogenies;
  }

  /**
   * Parses the tree at position {@code index} of a file. Nexus and Newick files are parsed one tree
   * at a time, keeping only the requested one; phyloXML files are always parsed completely.
   */
  private static Optional<Phylogeny> parseTree(PhylogenyParser parser, Path file, int index)
      throws IOException {
    // a parser instance per call, ParserBasedPhylogenyFactory is synchronized on a singleton
    parser.setSource(file.toFile());
    if (parser instanceof IteratingPhylogenyParser iterating) {
      // the preceding trees are discarded, a posterior sample can have thousands of them
      for (int i = 0; i < index; i++) {
        if (!iterating.hasNext()) {
          return Optional.empty();
        }
        iterating.next();
      }
      return iterating.hasNext() ? Optional.ofNullable(iterating.next()) : Optional.empty();
    }
    Phylogeny[] parsed = parser.parse();
    return parsed != null && index < parsed.length
        ? Optional.ofNullable(parsed[index])
        : Optional.empty();
  }

  private static PhylogenyTree toTree(int index, Phylogeny phylogeny) {
    PhylogenyTree tree = new PhylogenyTree();
    tree.setIndex(index);
    if (phylogeny == null || phylogeny.isEmpty()) {
      return tree;
    }
    tree.setName(phylogeny.getName());
    tree.setDescription(phylogeny.getDescription());
    tree.setType(phylogeny.getType());
    if (phylogeny.getIdentifier() != null) {
      tree.setIdentifier(phylogeny.getIdentifier().getValue());
      tree.setIdentifierProvider(phylogeny.getIdentifier().getProvider());
    }
    tree.setDistanceUnit(phylogeny.getDistanceUnit());
    tree.setRooted(phylogeny.isRooted());
    tree.setRerootable(phylogeny.isRerootable());
    tree.setCompletelyBinary(phylogeny.isCompletelyBinary());
    tree.setNodeCount(phylogeny.getNodeCount());
    tree.setExternalNodeCount(phylogeny.getNumberOfExternalNodes());
    tree.setInternalNodeCount(phylogeny.getNumberOfInternalNodes());
    tree.setBranchCount(phylogeny.getNumberOfBranches());
    tree.setRootChildCount(phylogeny.getRoot().getNumberOfDescendants());
    tree.setHeight(phylogeny.getHeight());
    return tree;
  }

  private record FileVersion(Path file, FileTime lastModified, long size) {}
}
