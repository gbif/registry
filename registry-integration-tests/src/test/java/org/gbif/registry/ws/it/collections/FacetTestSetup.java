package org.gbif.registry.ws.it.collections;

import org.gbif.registry.persistence.mapper.GrScicollVocabFacetMapper;
import org.gbif.registry.persistence.dto.GrSciCollVocabFacetDto;

/**
 * Utility class for setting up common facet data needed across multiple test classes.
 * This avoids code duplication between different test suites that need facet data.
 */
public class FacetTestSetup {

  /**
   * Sets up common facet data needed for GRSciColl tests.
   * Creates facets for:
   * - CollectionContentType: Biological, Archaeological, Paleontological, test1, test1.1, test1.1.1, test1.2
   * - PreservationType: SampleDried, StorageIndoors, StorageControlledAtmosphere, SampleCryopreserved, StorageOther
   * - InstitutionType: t1, t2, ty1, ty2, Museum, Herbarium
   * - Discipline: d1, d2, di1, di2, Botany, Archaeology, Anthropology
   *
   * @param grScicollVocabFacetMapper The FacetMapper to use for creating facets
   */
  public static void setupCommonFacets(GrScicollVocabFacetMapper grScicollVocabFacetMapper) {
    // Collection content type facets (regular ones)
    createFacetIfNotExists(grScicollVocabFacetMapper, "CollectionContentType", "Biological", "Biological");
    createFacetIfNotExists(grScicollVocabFacetMapper, "CollectionContentType", "Archaeological", "Archaeological");
    createFacetIfNotExists(grScicollVocabFacetMapper, "CollectionContentType", "Paleontological", "Paleontological");

    // Collection content type facets (test hierarchy for ConceptClientMock)
    createFacetIfNotExists(grScicollVocabFacetMapper, "CollectionContentType", "test1", "test1");
    createFacetIfNotExists(grScicollVocabFacetMapper, "CollectionContentType", "test1.1", "test1.test1.1");
    createFacetIfNotExists(grScicollVocabFacetMapper, "CollectionContentType", "test1.1.1", "test1.test1.1.test1.1.1");
    createFacetIfNotExists(grScicollVocabFacetMapper, "CollectionContentType", "test1.2", "test1.test1.2");

    // Preservation type facets
    createFacetIfNotExists(grScicollVocabFacetMapper, "PreservationType", "SampleDried", "SampleDried");
    createFacetIfNotExists(grScicollVocabFacetMapper, "PreservationType", "StorageIndoors", "StorageIndoors");
    createFacetIfNotExists(grScicollVocabFacetMapper, "PreservationType", "StorageControlledAtmosphere", "StorageControlledAtmosphere");
    createFacetIfNotExists(grScicollVocabFacetMapper, "PreservationType", "SampleCryopreserved", "SampleCryopreserved");
    createFacetIfNotExists(grScicollVocabFacetMapper, "PreservationType", "StorageOther", "StorageOther");

    // Institution type facets (for tests that need them)
    createFacetIfNotExists(grScicollVocabFacetMapper, "InstitutionType", "t1", "t1");
    createFacetIfNotExists(grScicollVocabFacetMapper, "InstitutionType", "t2", "t2");
    createFacetIfNotExists(grScicollVocabFacetMapper, "InstitutionType", "ty1", "ty1");
    createFacetIfNotExists(grScicollVocabFacetMapper, "InstitutionType", "ty2", "ty2");
    createFacetIfNotExists(grScicollVocabFacetMapper, "InstitutionType", "Museum", "Museum");
    createFacetIfNotExists(grScicollVocabFacetMapper, "InstitutionType", "Herbarium", "Herbarium");

    // Discipline facets (for tests that need them)
    createFacetIfNotExists(grScicollVocabFacetMapper, "Discipline", "d1", "d1");
    createFacetIfNotExists(grScicollVocabFacetMapper, "Discipline", "d2", "d2");
    createFacetIfNotExists(grScicollVocabFacetMapper, "Discipline", "di1", "di1");
    createFacetIfNotExists(grScicollVocabFacetMapper, "Discipline", "di2", "di2");
    createFacetIfNotExists(grScicollVocabFacetMapper, "Discipline", "Botany", "Botany");
    createFacetIfNotExists(grScicollVocabFacetMapper, "Discipline", "Archaeology", "Archaeology");
    createFacetIfNotExists(grScicollVocabFacetMapper, "Discipline", "Anthropology", "Anthropology");
  }

  /**
   * Creates a facet if it doesn't already exist.
   * Uses the getFacetIdByVocabularyAndName to check if it exists first.
   */
  private static void createFacetIfNotExists(GrScicollVocabFacetMapper grScicollVocabFacetMapper, String vocabularyName, String name, String path) {
    // Check if facet already exists
    Integer existingId = grScicollVocabFacetMapper.getFacetIdByVocabularyAndName(vocabularyName, name);
    if (existingId != null) {
      return; // Facet already exists, skip creation
    }

    // Create new facet
    GrSciCollVocabFacetDto facet = new GrSciCollVocabFacetDto();
    facet.setVocabularyName(vocabularyName);
    facet.setName(name);
    facet.setPath(path);
    grScicollVocabFacetMapper.create(facet);
  }

  /**
   * Cleans up all test facets. Should be called in @AfterEach or similar cleanup methods.
   */
  public static void cleanupTestFacets(GrScicollVocabFacetMapper grScicollVocabFacetMapper) {
    grScicollVocabFacetMapper.deleteByVocabularyName("CollectionContentType");
    grScicollVocabFacetMapper.deleteByVocabularyName("PreservationType");
    grScicollVocabFacetMapper.deleteByVocabularyName("InstitutionType");
    grScicollVocabFacetMapper.deleteByVocabularyName("Discipline");
  }
}
