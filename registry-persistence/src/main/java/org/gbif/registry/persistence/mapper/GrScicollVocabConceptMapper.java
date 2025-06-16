package org.gbif.registry.persistence.mapper;

import org.gbif.registry.persistence.mapper.dto.GrSciCollVocabConceptDto;

import java.util.UUID;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * Mapper for grscicoll_vocab_concept table operations.
 * Handles vocabulary concept definitions and their hierarchical relationships.
 */
@Repository
public interface GrScicollVocabConceptMapper {

  /**
   * Creates a new concept entry.
   *
   * @param concept The concept to create.
   */
  void create(GrSciCollVocabConceptDto concept);

    /**
   * Updates an existing concept entry.
   * The update is typically based on the ID of the GrSciCollVocabConceptDto.
   *
   * @param concept The concept entry with updated information.
   * @return The number of rows affected.
   */
  int update(GrSciCollVocabConceptDto concept);

  /**
   * Deletes all concepts associated with a given vocabulary name.
   *
   * @param vocabularyName The name of the vocabulary whose concepts should be deleted.
   */
  void deleteByVocabularyName(@Param("vocabularyName") String vocabularyName);

  /**
   * Gets the concept ID for a given vocabulary name and concept name.
   *
   * @param vocabularyName The vocabulary name (e.g., "Discipline")
   * @param conceptName The concept name (e.g., "Botany")
   * @return The concept ID, or null if not found
   */
  Integer getConceptIdByVocabularyAndName(
      @Param("vocabularyName") String vocabularyName,
      @Param("conceptName") String conceptName);

  /**
   * Inserts a link between an institution and a concept.
   *
   * @param institutionKey The institution UUID
   * @param conceptId The concept ID
   */
  void insertInstitutionConcept(
      @Param("institutionKey") UUID institutionKey,
      @Param("conceptId") Integer conceptId);

  /**
   * Deletes all concept links for a given institution.
   *
   * @param institutionKey The institution UUID
   */
  void deleteInstitutionConcepts(@Param("institutionKey") UUID institutionKey);

  /**
   * Inserts a link between a collection and a concept.
   *
   * @param collectionKey The collection UUID
   * @param conceptId The concept ID
   */
  void insertCollectionConcept(
      @Param("collectionKey") UUID collectionKey,
      @Param("conceptId") Integer conceptId);

  /**
   * Deletes all concept links for a given collection.
   *
   * @param collectionKey The collection UUID
   */
  void deleteCollectionConcepts(@Param("collectionKey") UUID collectionKey);

}
