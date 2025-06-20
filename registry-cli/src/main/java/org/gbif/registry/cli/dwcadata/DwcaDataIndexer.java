package org.gbif.registry.cli.dwcadata;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import org.gbif.api.model.registry.Dataset;
import org.gbif.dwc.Archive;
import org.gbif.dwc.DwcFiles;

import org.gbif.registry.ws.client.DatasetClient;

import org.gbif.ws.client.ClientBuilder;

import org.gbif.ws.json.JacksonJsonObjectMapperProvider;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Command line tool to index DwC-A data from directories.
 * It reads configuration from a YAML file and processes each directory specified in the configuration.
 * For each directory, it checks if it contains DwC-A files and updates the dataset accordingly.
 */
@Slf4j
public class DwcaDataIndexer {

  public static void main(String[] args) {
    if (args.length == 0) {
      log.error("No configuration provided. Please specify the configuration file path.");
      return;
    }
    DwcaIndexerConfiguration configuration = DwcaIndexerConfiguration.fromYaml(args[0]);
    indexDwcaData(configuration);
  }

  /**
   * Indexes DwC-A data based on the provided configuration.
   * It iterates through each base directory, checks for subdirectories, and processes them if they are not empty.
   *
   * @param configuration the DwcaIndexerConfiguration containing the necessary parameters
   */
  private static void indexDwcaData(DwcaIndexerConfiguration configuration) {
    DatasetClient datasetClient = datasetClient(configuration);
    for (String baseDir : configuration.getBaseDirectories()) {
      try (Stream<Path> paths = Files.list(Paths.get(baseDir))) {
        paths.filter(d -> Files.isDirectory(d) && isDirectoryNotEmpty(d)).forEach(subDir -> {
          try {
            Archive archive = DwcFiles.fromLocationSkipValidation(subDir);
            UUID datasetKey = UUID.fromString(subDir.getFileName().toString());
            Dataset dataset = datasetClient.get(datasetKey);
            if (dataset != null) {
              Dataset.DwcA dwcA = fromArchive(archive);
              updateOrInsertDwcaData(datasetClient, dataset, dwcA, configuration);
            } else {
              log.warn("Dataset with key {} not found, skipping update.", datasetKey);
            }
            log.info("Indexed DwC-A file from directory: {}", subDir);
          } catch (Exception e) {
            log.error("Error processing directory {}", subDir, e);
          }
        });
      } catch (Exception e) {
        log.error("Error listing subdirectories in {}", baseDir, e);
      }
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
  private static Dataset.DwcA fromArchive(Archive archive) {
    Dataset.DwcA dwca = new Dataset.DwcA();
    dwca.setRowType(archive.getCore().getRowType().qualifiedName());
    dwca.setExtensions(archive.getExtensions().stream()
        .map(ext -> ext.getRowType().qualifiedName())
        .collect(Collectors.toList()));
    return dwca;
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
