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
package org.gbif.registry.cli.datasetupdater;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.License;
import org.gbif.registry.cli.util.PostgresDBExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.gbif.registry.cli.util.EmbeddedPostgresTestUtils.LIQUIBASE_MASTER_FILE;
import static org.gbif.registry.cli.util.EmbeddedPostgresTestUtils.toDbConfig;
import static org.gbif.registry.cli.util.RegistryCliUtils.getFileData;
import static org.gbif.registry.cli.util.RegistryCliUtils.loadConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DatasetUpdaterCommandIT {

  private static final UUID DATASET_KEY = UUID.fromString("38f06820-08c5-42b2-94f6-47cc3e83a54a");

  @RegisterExtension
  static PostgresDBExtension database =
      PostgresDBExtension.builder().liquibaseChangeLogFile(LIQUIBASE_MASTER_FILE).build();

  private DatasetUpdaterConfiguration getConfig(String configFile) {
    DatasetUpdaterConfiguration cfg = loadConfig(configFile, DatasetUpdaterConfiguration.class);
    cfg.db = toDbConfig(database.getPostgresContainer());
    return cfg;
  }

  @BeforeEach
  public void prepareDatabase() throws Exception {
    Connection con = database.getDatasoruce().getConnection();
    String sql = getFileData("datasetupdater/prepare_dataset.sql");

    PreparedStatement stmt = con.prepareStatement(sql);
    stmt.executeUpdate();
    con.close();
  }

  @AfterEach
  public void after() throws Exception {
    Connection con = database.getDatasoruce().getConnection();
    String sql = getFileData("datasetupdater/clean_dataset.sql");

    PreparedStatement stmt = con.prepareStatement(sql);
    stmt.executeUpdate();
    con.close();
  }

  /**
   * Test checks single dataset/list of datasets reinterpreted from preferred metadata document by
   * ensuring license was updated properly.
   */
  @ParameterizedTest
  @ValueSource(
      strings = {"datasetupdater/dataset-updater.yaml", "datasetupdater/dataset-updater-list.yaml"})
  public void testUpdate(String configFile) {
    DatasetUpdaterCommand command = new DatasetUpdaterCommand(getConfig(configFile));
    command.doRun();
    Dataset dataset = command.getDatasetUpdater().getDatasetResource().get(DATASET_KEY);
    assertNotNull(dataset);
    assertEquals(License.CC_BY_4_0, dataset.getLicense());
  }
}
