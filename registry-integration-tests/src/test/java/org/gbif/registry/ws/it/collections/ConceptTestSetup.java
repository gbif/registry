package org.gbif.registry.ws.it.collections;

import org.gbif.registry.persistence.mapper.GrScicollVocabConceptMapper;
import org.gbif.registry.persistence.mapper.dto.GrSciCollVocabConceptDto;

/**
 * Utility class for setting up common concept data needed across multiple test classes.
 * This avoids code duplication between different test suites that need concept data.
 */
public class ConceptTestSetup {

  /**
   * Sets up common concept data needed for GRSciColl tests.
   * Creates facets for:
   * - CollectionContentType: Biological, Archaeological, Paleontological, test1, test1.1, test1.1.1, test1.2
   * - PreservationType: SampleDried, StorageIndoors, StorageControlledAtmosphere, SampleCryopreserved, StorageOther
   * - InstitutionType: t1, t2, ty1, ty2, Museum, Herbarium
   * - Discipline: d1, d2, di1, di2, Botany, Archaeology, Anthropology
   *
   * @param grScicollVocabConceptMapper The FacetMapper to use for creating facets
   */
  public static void setupCommonConcepts(GrScicollVocabConceptMapper grScicollVocabConceptMapper) {
    // Collection content type concepts (regular ones)
    createConceptIfNotExists(grScicollVocabConceptMapper, "CollectionContentType", "Biological", "Biological");
    createConceptIfNotExists(grScicollVocabConceptMapper, "CollectionContentType", "Archaeological", "Archaeological");
    createConceptIfNotExists(grScicollVocabConceptMapper, "CollectionContentType", "Paleontological", "Paleontological");

    // Collection content type concepts (test hierarchy for ConceptClientMock)
    createConceptIfNotExists(grScicollVocabConceptMapper, "CollectionContentType", "test1", "test1");
    createConceptIfNotExists(grScicollVocabConceptMapper, "CollectionContentType", "test1.1", "test1.test1.1");
    createConceptIfNotExists(grScicollVocabConceptMapper, "CollectionContentType", "test1.1.1", "test1.test1.1.test1.1.1");
    createConceptIfNotExists(grScicollVocabConceptMapper, "CollectionContentType", "test1.2", "test1.test1.2");

    // Preservation type concepts
    createConceptIfNotExists(grScicollVocabConceptMapper, "PreservationType", "SampleDried", "SampleDried");
    createConceptIfNotExists(grScicollVocabConceptMapper, "PreservationType", "StorageIndoors", "StorageIndoors");
    createConceptIfNotExists(grScicollVocabConceptMapper, "PreservationType", "StorageControlledAtmosphere", "StorageControlledAtmosphere");
    createConceptIfNotExists(grScicollVocabConceptMapper, "PreservationType", "SampleCryopreserved", "SampleCryopreserved");
    createConceptIfNotExists(grScicollVocabConceptMapper, "PreservationType", "StorageOther", "StorageOther");

    // Institution type concepts (for tests that need them)
    createConceptIfNotExists(grScicollVocabConceptMapper, "InstitutionType", "t1", "t1");
    createConceptIfNotExists(grScicollVocabConceptMapper, "InstitutionType", "t2", "t2");
    createConceptIfNotExists(grScicollVocabConceptMapper, "InstitutionType", "ty1", "ty1");
    createConceptIfNotExists(grScicollVocabConceptMapper, "InstitutionType", "ty2", "ty2");
    createConceptIfNotExists(grScicollVocabConceptMapper, "InstitutionType", "Museum", "Museum");
    createConceptIfNotExists(grScicollVocabConceptMapper, "InstitutionType", "Herbarium", "Herbarium");

    // Discipline concepts (for tests that need them)
    createConceptIfNotExists(grScicollVocabConceptMapper, "Discipline", "d1", "d1");
    createConceptIfNotExists(grScicollVocabConceptMapper, "Discipline", "d2", "d2");
    createConceptIfNotExists(grScicollVocabConceptMapper, "Discipline", "di1", "di1");
    createConceptIfNotExists(grScicollVocabConceptMapper, "Discipline", "di2", "di2");
    createConceptIfNotExists(grScicollVocabConceptMapper, "Discipline", "Botany", "Botany");
    createConceptIfNotExists(grScicollVocabConceptMapper, "Discipline", "Archaeology", "Archaeology");
    createConceptIfNotExists(grScicollVocabConceptMapper, "Discipline", "Anthropology", "Anthropology");
  }

  /**
   * Creates a concept if it doesn't already exist.
   * Uses the getFacetIdByVocabularyAndName to check if it exists first.
   */
  private static void createConceptIfNotExists(
    GrScicollVocabConceptMapper grScicollVocabConceptMapper, String vocabularyName, String name, String path) {
    // Check if concept already exists
    Integer existingId = grScicollVocabConceptMapper.getConceptIdByVocabularyAndName(vocabularyName, name);
    if (existingId != null) {
      return; // Concept already exists, skip creation
    }

    // Create new concept
    GrSciCollVocabConceptDto conceptDto = new GrSciCollVocabConceptDto();
    conceptDto.setVocabularyName(vocabularyName);
    conceptDto.setName(name);
    conceptDto.setPath(path);
    grScicollVocabConceptMapper.create(conceptDto);
  }

  /**
   * Cleans up all test concepts. Should be called in @AfterEach or similar cleanup methods.
   */
  public static void cleanupTestConcepts(GrScicollVocabConceptMapper grScicollVocabConceptMapper) {
    grScicollVocabConceptMapper.deleteByVocabularyName("CollectionContentType");
    grScicollVocabConceptMapper.deleteByVocabularyName("PreservationType");
    grScicollVocabConceptMapper.deleteByVocabularyName("AccessionStatus");
    grScicollVocabConceptMapper.deleteByVocabularyName("InstitutionType");
    grScicollVocabConceptMapper.deleteByVocabularyName("Discipline");
    grScicollVocabConceptMapper.deleteByVocabularyName("InstitutionalGovernance");
  }
}
