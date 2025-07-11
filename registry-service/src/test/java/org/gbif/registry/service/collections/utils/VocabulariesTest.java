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
package org.gbif.registry.service.collections.utils;

import org.gbif.api.model.registry.Dataset;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.client.ConceptClient;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VocabulariesTest {

  private ConceptClient conceptClient;

  @BeforeEach
  public void setUp() {
    conceptClient = mock(ConceptClient.class);
  }

  @Test
  public void testCheckDatasetVocabsValuesValid() {
    Dataset dataset = new Dataset();
    Set<String> categories = new HashSet<>();
    categories.add("Biological");
    categories.add("Taxonomic");
    dataset.setCategory(categories);

    when(conceptClient.getFromLatestRelease("DatasetCategory", "Biological", false, false))
        .thenReturn(createConceptView(1L, "Biological"));
    when(conceptClient.getFromLatestRelease("DatasetCategory", "Taxonomic", false, false))
        .thenReturn(createConceptView(2L, "Taxonomic"));

    // Should not throw exception
    assertDoesNotThrow(() -> Vocabularies.checkDatasetVocabsValues(conceptClient, dataset));
  }

  @Test
  public void testCheckDatasetVocabsValuesInvalid() {
    Dataset dataset = new Dataset();
    Set<String> categories = new HashSet<>();
    categories.add("InvalidCategory");
    dataset.setCategory(categories);

    when(conceptClient.getFromLatestRelease("DatasetCategory", "InvalidCategory", false, false))
        .thenReturn(null);

    // Should throw exception
    assertThrows(IllegalArgumentException.class, () -> {
      Vocabularies.checkDatasetVocabsValues(conceptClient, dataset);
    });
  }

  @Test
  public void testCheckDatasetVocabsValuesNullCategory() {
    Dataset dataset = new Dataset();
    dataset.setCategory(null);

    // Should not throw exception for null category
    assertDoesNotThrow(() -> Vocabularies.checkDatasetVocabsValues(conceptClient, dataset));
  }

  @Test
  public void testCheckDatasetVocabsValuesEmptyCategory() {
    Dataset dataset = new Dataset();
    dataset.setCategory(new HashSet<>());

    // Should not throw exception for empty category
    assertDoesNotThrow(() -> Vocabularies.checkDatasetVocabsValues(conceptClient, dataset));
  }

  @Test
  public void testCheckDatasetVocabsValuesDeprecatedCategory() {
    Dataset dataset = new Dataset();
    Set<String> categories = new HashSet<>();
    categories.add("DeprecatedCategory");
    dataset.setCategory(categories);

    when(conceptClient.getFromLatestRelease("DatasetCategory", "DeprecatedCategory", false, false))
        .thenReturn(createDeprecatedConceptView(3L, "DeprecatedCategory"));

    // Should throw exception for deprecated category
    assertThrows(IllegalArgumentException.class, () -> {
      Vocabularies.checkDatasetVocabsValues(conceptClient, dataset);
    });
  }

  private ConceptView createConceptView(Long id, String name) {
    org.gbif.vocabulary.model.Concept concept = new org.gbif.vocabulary.model.Concept();
    concept.setKey(id);
    concept.setName(name);
    return new ConceptView(concept);
  }

  private ConceptView createDeprecatedConceptView(Long id, String name) {
    org.gbif.vocabulary.model.Concept concept = new org.gbif.vocabulary.model.Concept();
    concept.setKey(id);
    concept.setName(name);
    concept.setDeprecated(java.time.ZonedDateTime.now());
    return new ConceptView(concept);
  }
} 