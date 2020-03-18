/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.registry.cli.datasetupdater;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.License;
import org.gbif.utils.file.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Parameterized.class)
public class DatasetUpdaterCommandIT {

  private static final UUID DATASET_KEY = UUID.fromString("38f06820-08c5-42b2-94f6-47cc3e83a54a");

  private DatasetUpdaterConfiguration cfg;

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {"datasetupdater/dataset-updater.yaml"}, {"datasetupdater/dataset-updater-list.yaml"}
        });
  }

  public DatasetUpdaterCommandIT(String configFile) {
    loadConfig(configFile);
  }

  @Before
  public void prepareDatabase() throws Exception {
    Connection con =
        prepareConnection(cfg.db.serverName, cfg.db.databaseName, cfg.db.user, cfg.db.password);
    String sql = getFileData("datasetupdater/prepare_dataset.sql");

    PreparedStatement stmt = con.prepareStatement(sql);
    stmt.executeUpdate();
    con.close();
  }

  @After
  public void after() throws Exception {
    Connection con =
        prepareConnection(cfg.db.serverName, cfg.db.databaseName, cfg.db.user, cfg.db.password);
    String sql = getFileData("datasetupdater/clean_dataset.sql");

    PreparedStatement stmt = con.prepareStatement(sql);
    stmt.executeUpdate();
    con.close();
  }

  /**
   * Test checks single dataset/list of datasets reinterpreted from preferred metadata document by
   * ensuring license was updated properly.
   */
  @Test
  public void testUpdateSingleDataset() {
    DatasetUpdaterCommand command = new DatasetUpdaterCommand(cfg);
    command.doRun();
    Dataset dataset = command.getDatasetUpdater().getDatasetResource().get(DATASET_KEY);
    assertNotNull(dataset);
    assertEquals(License.CC_BY_4_0, dataset.getLicense());
  }

  private Connection prepareConnection(
      String host, String databaseName, String user, String password) throws Exception {
    return DriverManager.getConnection(
        String.format("jdbc:postgresql://%s/%s", host, databaseName), user, password);
  }

  private String getFileData(String filename) throws Exception {
    //noinspection ConstantConditions
    byte[] bytes =
        Files.readAllBytes(
            Paths.get(ClassLoader.getSystemClassLoader().getResource(filename).getFile()));

    return new String(bytes);
  }

  private void loadConfig(String configFile) {
    try {
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      InputStream is = FileUtils.classpathStream(configFile);
      cfg = mapper.readValue(is, DatasetUpdaterConfiguration.class);
      System.out.println(cfg);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
