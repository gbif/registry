package org.gbif.registry.cli.datasetupdater;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.License;
import org.gbif.utils.file.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@Ignore("To run tests, configure dataset-updater.yaml file to connect to local registry db")
public class DatasetUpdaterIT {

  private DatasetUpdaterConfiguration cfg;
  private DatasetUpdater updater;

  @Before
  public void setup() {
    cfg = loadConfig();
    updater = DatasetUpdater.build(cfg);

    // set dataset license to CC0!
    Dataset ds = updater.getDatasetService().get(UUID.fromString(cfg.key));
    ds.setLicense(License.CC0_1_0);
    updater.getDatasetService().update(ds);

    // assert license was set properly
    ds = updater.getDatasetService().get(UUID.fromString(cfg.key));
    assertEquals(License.CC0_1_0, ds.getLicense());
  }

  /**
   * Test checks single dataset reinterpreted from preferred metadata document by
   * ensuring license was updated properly.
   */
  @Test
  @Ignore
  public void testUpdateSingleDataset() {
    updater.update(UUID.fromString("38f06820-08c5-42b2-94f6-47cc3e83a54a"));
    Dataset ds = updater.getDatasetService().get(UUID.fromString(cfg.key));
    assertEquals(License.CC_BY_NC_4_0, ds.getLicense());
  }

  /**
   * Test checks list of datasets reinterpreted from preferred metadata document by
   * ensuring license was updated properly.
   */
  @Test
  @Ignore
  public void testUpdateListOfDatasets() {
    List<UUID> keys = Lists.newArrayList();
    keys.add(UUID.fromString("38f06820-08c5-42b2-94f6-47cc3e83a54a"));
    updater.update(keys);
    Dataset ds = updater.getDatasetService().get(UUID.fromString(cfg.key));
    assertEquals(License.CC_BY_NC_4_0, ds.getLicense());
  }

  private static DatasetUpdaterConfiguration loadConfig() {
    try {
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      InputStream dc = FileUtils.classpathStream("datasetupdater/dataset-updater.yaml");
      DatasetUpdaterConfiguration cfg = mapper.readValue(dc, DatasetUpdaterConfiguration.class);
      System.out.println(cfg);
      return cfg;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
