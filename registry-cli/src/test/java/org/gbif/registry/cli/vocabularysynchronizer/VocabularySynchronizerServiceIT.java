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

import org.gbif.registry.database.BaseDBTest;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration test for VocabularySynchronizerService that tests basic service functionality.
 */
@DisabledIfSystemProperty(named = "test.vocabulary.synchronizer", matches = "false")
public class VocabularySynchronizerServiceIT extends BaseDBTest {

  @Test
  public void testVocabularySynchronizerServiceConstruction() {
    // Given
    VocabularySynchronizerConfiguration config = new VocabularySynchronizerConfiguration();
    config.vocabulariesToProcess = Set.of("DatasetCategory", "CollectionType");
    config.apiRootUrl = "http://localhost:8080";
    config.poolSize = 1;
    config.queueName = "vocabulary-released-registry";

    // When
    VocabularySynchronizerService service = new VocabularySynchronizerService(config);

    // Then
    assertNotNull(service);
  }

  @Test
  public void testVocabularySynchronizerServiceWithSingleVocabulary() {
    // Given
    VocabularySynchronizerConfiguration config = new VocabularySynchronizerConfiguration();
    config.vocabulariesToProcess = Set.of("DatasetCategory");
    config.apiRootUrl = "http://localhost:8080";
    config.poolSize = 1;
    config.queueName = "vocabulary-released-registry";

    // When
    VocabularySynchronizerService service = new VocabularySynchronizerService(config);

    // Then
    assertNotNull(service);
  }
}
