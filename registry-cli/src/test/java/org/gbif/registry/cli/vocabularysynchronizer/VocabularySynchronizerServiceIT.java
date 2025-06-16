package org.gbif.registry.cli.vocabularysynchronizer;

import java.util.Set;

import org.gbif.registry.database.BaseDBTest;

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
    config.queueName = "vocabulary-released";

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
    config.queueName = "vocabulary-released";

    // When
    VocabularySynchronizerService service = new VocabularySynchronizerService(config);

    // Then
    assertNotNull(service);
  }
}
 