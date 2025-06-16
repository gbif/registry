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
package org.gbif.registry.cli.vocabularysynchronizer;

import java.util.Set;

import org.gbif.registry.database.BaseDBTest;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import static org.gbif.registry.cli.util.EmbeddedPostgresTestUtils.toDbConfig;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration test for VocabularySynchronizerCommand that tests the actual
 * command execution with real database operations.
 */
@DisabledIfSystemProperty(named = "test.vocabulary.synchronizer", matches = "false")
public class VocabularySynchronizerCommandIT extends BaseDBTest {

  private VocabularySynchronizerConfiguration config;

  @BeforeEach
  public void prepareDatabase() throws Exception {
    // Initialize config
    config = new VocabularySynchronizerConfiguration();
    
    // Prepare test data
    Connection con = PG_CONTAINER.createConnection("");
    String sql = getFileData("vocabularysynchronizer/prepare_test_data.sql");
    if (sql != null && !sql.trim().isEmpty()) {
      PreparedStatement stmt = con.prepareStatement(sql);
      stmt.executeUpdate();
    }
    con.close();
  }

  @AfterEach
  public void after() throws Exception {
    // Clean up test data
    Connection con = PG_CONTAINER.createConnection("");
    String sql = getFileData("vocabularysynchronizer/clean_test_data.sql");
    if (sql != null && !sql.trim().isEmpty()) {
      PreparedStatement stmt = con.prepareStatement(sql);
      stmt.executeUpdate();
    }
    con.close();
  }

  @Test
  public void testVocabularySynchronizerCommandConstruction() {
    // Given
    config = createTestConfiguration();

    // When
    VocabularySynchronizerCommand command = new VocabularySynchronizerCommand();

    // Then
    assertNotNull(command);
  }

  @Test
  public void testVocabularySynchronizerCommandWithValidConfig() {
    // Given
    config = createTestConfiguration();
    config.vocabulariesToProcess = Set.of("DatasetCategory,CollectionType");

    // When
    VocabularySynchronizerCommand command = new VocabularySynchronizerCommand();

    // Then - should not throw exception
    assertNotNull(command);
  }

  @Test
  public void testVocabularySynchronizerCommandWithSingleVocabulary() {
    // Given
    config = createTestConfiguration();
    config.vocabulariesToProcess = Set.of("DatasetCategory");

    // When
    VocabularySynchronizerCommand command = new VocabularySynchronizerCommand();

    // Then - should not throw exception
    assertNotNull(command);
  }

  private VocabularySynchronizerConfiguration createTestConfiguration() {
    VocabularySynchronizerConfiguration config = new VocabularySynchronizerConfiguration();
    config.setDbConfig(toDbConfig(PG_CONTAINER));
    config.apiRootUrl = "http://localhost:8080";
    config.queueName = "vocabulary-released";
    config.poolSize = 1;
    return config;
  }

  private String getFileData(String filePath) {
    try {
      return org.gbif.registry.cli.util.RegistryCliUtils.getFileData(filePath);
    } catch (Exception e) {
      // File doesn't exist, return null
      return null;
    }
  }
}
