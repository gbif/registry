package org.gbif.registry.cli.datasetupdater;

import org.gbif.utils.file.FileUtils;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("To run tests, configure yaml files to connect to local registry db and dataset keys file with actual keys")
public class DatasetUpdaterCommandIT {

  /**
   * Test checks dataset updater reinterprets single dataset from preferred metadata document.
   */
  @Test
  @Ignore
  public void testUpdateSingleDataset() throws IOException {
    InputStream dc = FileUtils.classpathStream("datasetupdater/dataset-updater.yaml");
    DatasetUpdaterConfiguration cfg = loadConfig(dc);
    DatasetUpdaterCommand command = new DatasetUpdaterCommand(cfg);
    command.doRun();
  }

  /**
   * Test checks dataset updater reinterprets list of datasets from their preferred metadata document.
   */
  @Test
  @Ignore
  public void testUpdateListOfDatasets() throws IOException {
    InputStream dc = FileUtils.classpathStream("datasetupdater/dataset-updater-list.yaml");
    DatasetUpdaterConfiguration cfg = loadConfig(dc);
    DatasetUpdaterCommand command = new DatasetUpdaterCommand(cfg);
    command.doRun();
  }

  private static DatasetUpdaterConfiguration loadConfig(InputStream is) {
    try {
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      return mapper.readValue(is, DatasetUpdaterConfiguration.class);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
