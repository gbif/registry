package org.gbif.registry.cli.dwcadata;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import org.gbif.api.model.registry.Dataset;
import org.gbif.dwc.Archive;
import org.gbif.dwc.ArchiveFile;
import org.gbif.dwc.DwcFiles;

import org.gbif.dwc.record.Record;
import org.gbif.registry.ws.client.DatasetClient;

import org.gbif.utils.file.ClosableIterator;
import org.gbif.ws.client.ClientBuilder;

import org.gbif.ws.json.JacksonJsonObjectMapperProvider;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Command line tool to index DwC-A data from directories.
 * It reads configuration from a YAML file and processes each directory specified in the configuration.
 * For each directory, it checks if it contains DwC-A files and updates the dataset accordingly.
 */
@Slf4j
public class DwcaDataIndexer {

  private final DwcaIndexerConfiguration configuration;
  private final DatasetClient datasetClient;
  private final AtomicInteger processedCount = new AtomicInteger(0);
  private final Set<UUID> updatedDatasetKeys = new HashSet<>();

  public static void main(String[] args) {
    if (args.length == 0) {
      log.error("No configuration provided. Please specify the configuration file path.");
      return;
    }
    DwcaDataIndexer indexer = new DwcaDataIndexer(args[0]);
    indexer.indexDwcaData();
  }

  public DwcaDataIndexer(String configFilePath) {
      this.configuration = DwcaIndexerConfiguration.fromYaml(configFilePath);
      this.datasetClient = datasetClient(configuration);
  }

  private void indexDwcaData() {
    indexDwcaDataFromDirectories();
    indexDwcaDataFromArchives();
  }

  /**
   * Indexes DwC-A data based on the provided configuration.
   * It iterates through each base directory, checks for subdirectories, and processes them if they are not empty.
   *
   * @param configuration the DwcaIndexerConfiguration containing the necessary parameters
   */
  private void indexDwcaDataFromDirectories() {
    if (configuration.getBaseDirectories() == null) {
      log.warn("No base directories specified in the configuration.");
      return;
    }
    for (String baseDir : configuration.getBaseDirectories()) {
      log.info("Retrieving directories from: {}", baseDir);
      try (Stream<Path> paths = Files.list(Paths.get(baseDir))) {
        List<Path> subDirs = paths
          .filter(d -> Files.isDirectory(d) && isDirectoryNotEmpty(d) && isUUID(d.getFileName().toString()))
          .collect(Collectors.toList());
        log.info("Found {} non-empty subdirectories in base directory: {}", subDirs.size(), baseDir);
        subDirs.parallelStream().forEach(subDir -> {
          try {
            UUID datasetKey = UUID.fromString(subDir.getFileName().toString());
            if (updatedDatasetKeys.contains(datasetKey)) {
              log.info("Dataset {} already processed, sourced folder {}, skipping.", datasetKey, subDir);
            } else {
              updateDwcaData(datasetKey, DwcFiles.fromLocationSkipValidation(subDir));
            }
          } catch (Exception e) {
            log.error("Error processing directory {}", subDir, e);
          }
        });
      } catch (Exception e) {
        log.error("Error listing subdirectories in {}", baseDir, e);
      }
    }
    log.info("Indexing completed for all directories.");
  }


  private void updateDwcaData(UUID datasetKey, Archive archive) {
    Dataset dataset = datasetClient.get(datasetKey);
    if (dataset != null) {
      Dataset.DwcA dwcA = fromArchive(archive);
      updateOrInsertDwcaData(datasetClient, dataset, dwcA, configuration);
    } else {
      log.warn("Dataset with key {} not found, skipping update.", datasetKey);
    }
    int count = processedCount.incrementAndGet();
    log.info("Indexed DwC-A file from path: {} (Processed: {})", archive.getLocation(), count);
  }

  /**
   * Indexes DwC-A data based on the provided configuration.
   * It iterates through each base directory, checks for subdirectories, and processes them if they are not empty.
   *
   * @param configuration the DwcaIndexerConfiguration containing the necessary parameters
   */
  private void indexDwcaDataFromArchives() {
    if (configuration.getArchiveDirectories() == null) {
      log.warn("No archive directories specified in the configuration.");
      return;
    }
    for (String baseDir : configuration.getArchiveDirectories()) {
      log.info("Retrieving directories from archive directory: {}", baseDir);
      try (Stream<Path> paths = Files.list(Paths.get(baseDir))) {
        List<Path> subDirs = paths
          .filter(d -> Files.isDirectory(d) && isDirectoryNotEmpty(d) && isUUID(d.getFileName().toString()))
          .collect(Collectors.toList());
        log.info("Found {} non-empty subdirectories in archive directory: {}", subDirs.size(), baseDir);
        subDirs.parallelStream().forEach(subDir ->
            findLatestDwcaInDir(subDir).ifPresent(archivePath -> {
              try {
                UUID datasetKey = UUID.fromString(subDir.getFileName().toString());
                if (updatedDatasetKeys.contains(datasetKey)) {
                  log.info("Dataset {} already processed, source folder {}, skipping.", datasetKey,subDir);
                } else {
                  Path targetPath = Files.createDirectory(Paths.get(configuration.getUnpackDirectory(), datasetKey.toString()));
                  Archive archive = DwcFiles.fromCompressed(archivePath, targetPath);
                  updateDwcaData(datasetKey, archive);
                }
              } catch (Exception e) {
                log.error("Error processing archive file in directory {}", subDir, e);
              }
        }));
      } catch (Exception e) {
        log.error("Error listing subdirectories in {}", baseDir, e);
      }
    }
    log.info("Indexing completed for all archive directories.");
  }

  /**
   * Finds the latest DwC-A file in the given directory.
   * It looks for files with the ".dwca" extension and returns the one with the most recent last modified time.
   *
   * @param dir the directory to search for DwC-A files
   * @return an Optional containing the path of the latest DwC-A file, or empty if none found
   */
  private static Optional<Path> findLatestDwcaInDir(Path dir) {
    try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*.dwca")) {
      return StreamSupport.stream(files.spliterator(), false)
        .filter(Files::isRegularFile)
        .max(Comparator.comparingLong(path -> {
          try {
            return Files.getLastModifiedTime(path).toMillis();
          } catch (IOException e) {
            return Long.MIN_VALUE;
          }
        }));
    } catch (IOException e) {
      log.error("Error reading dir {}", dir, e);
      return Optional.empty();
    }
  }

  /**
   * Checks if the given string is a valid UUID.
   *
   * @param str the string to check
   * @return true if the string is a valid UUID, false otherwise
   */
  private static boolean isUUID(String str) {
    try {
      UUID.fromString(str);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Updates or inserts DwC-A data for the given dataset.
   * If the dataset does not have DwC-A data, it creates a new entry.
   * If it already exists, it updates the existing entry.
   *
   * @param datasetClient the DatasetClient to interact with the GBIF API
   * @param dataset the Dataset to update or insert DwC-A data for
   * @param dwcA the DwC-A data to be updated or inserted
   * @param configuration the DwcaIndexerConfiguration containing dry run flag
   */
  private static void updateOrInsertDwcaData(DatasetClient datasetClient, Dataset dataset, Dataset.DwcA dwcA, DwcaIndexerConfiguration configuration) {
      if (dataset.getDwca() == null) {
        log.info("Creating dataset DwC-A data {}.", dataset.getKey());
        if (!configuration.isDryRun()) {
          datasetClient.createDwcaData(dataset.getKey(), dwcA);
        }
      } else {
        log.info("Updating dataset DwC-A data {}.", dataset.getKey());
        if (!configuration.isDryRun()) {
          datasetClient.updateDwcaData(dataset.getKey(), dwcA);
        }
      }
  }

  /**
   * Checks if the given directory is not empty.
   *
   * @param dir the directory to check
   * @return true if the directory is not empty, false otherwise
   */
  private static boolean isDirectoryNotEmpty(Path dir) {
    try (Stream<Path> files = Files.list(dir)) {
      return files.findAny().isPresent();
    } catch (Exception e) {
      log.error("Error checking if directory {} is empty", dir, e);
      return false;
    }
  }

  /**
   * Creates a DatasetClient using the provided configuration.
   *
   * @param configuration the DwcaIndexerConfiguration containing the necessary parameters
   * @return a configured DatasetClient instance
   */
  private static DatasetClient datasetClient(DwcaIndexerConfiguration configuration) {
    return new ClientBuilder().withObjectMapper(JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport())
              .withAppKeyCredentials(configuration.getUsername(), configuration.getAppKeyName(), configuration.getPassword())
              .withUrl(configuration.getGbifApiUrl())
              .build(DatasetClient.class);
  }


  /**
   * Converts an Archive to a Dataset.DwcA object.
   *
   * @param archive the Archive to convert
   * @return a Dataset.DwcA object representing the archive
   */
  /**
   * Converts an Archive to a Dataset.DwcA object.
   *
   * @param archive the Archive to convert
   * @return a Dataset.DwcA object representing the archive
   */
  private static Dataset.DwcA fromArchive(Archive archive) {
    Dataset.DwcA dwca = new Dataset.DwcA();
    dwca.setCoreType(archive.getCore().getRowType().qualifiedName());
    if (archive.getExtensions() != null) {
      dwca.setExtensions(archive.getExtensions().stream()
        .filter(DwcaDataIndexer::hasRecords)
        .map(ext -> ext.getRowType().qualifiedName())
        .collect(Collectors.toList()));
    }
    return dwca;
  }

  /**
   * Checks if the given archive file has any records.
   *
   * @param archive the archive file to check
   * @return true if the archive has records, false otherwise
   */
  private static boolean hasRecords(ArchiveFile archive) {
    try {
      try (ClosableIterator<Record> it = archive.iterator()) {
        return it.hasNext();
      }
    } catch (Exception e) {
      log.error("Failed to check for records in archive", e);
      return false;
    }
  }

  /**
   * Configuration class for the DwcaDataIndexer.
   * It holds the base directories, username, app key name, password, and GBIF API URL.
   *
   * Example YAML configuration:
   * ```yaml
   * baseDirectories:
   *   - //home/crap/storage/unpacked/
   *   - //mnt/auto/crawler/unpacked/
   * username: admin_user
   * password: admin_user_password
   * appKeyName: appKey
   * gbifApiUrl: https://api.gbif-dev.org/v1
   * dryRun: true
   */
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @ToString
  public static class DwcaIndexerConfiguration {

    private String[] baseDirectories;

    private String[] archiveDirectories;

    private String unpackDirectory;

    private String username;

    private String appKeyName;

    private String password;

    private String gbifApiUrl;

    private boolean dryRun = false;

    /**
     * Reads the configuration from a YAML file.
     *
     * @param yamlFilePath the path to the YAML file
     * @return a DwcaIndexerConfiguration object populated with the data from the YAML file
     */
    @SneakyThrows
    public static DwcaIndexerConfiguration fromYaml(String yamlFilePath) {
      try(FileInputStream inputStream = new FileInputStream(yamlFilePath)) {
        Yaml yaml = new Yaml();
        return yaml.loadAs(inputStream, DwcaIndexerConfiguration.class);
      }
    }

  }
}
