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
package org.gbif.registry.persistence.mapper;

import org.gbif.registry.persistence.mapper.dto.GrSciCollVocabConceptDto;
import org.gbif.registry.persistence.mapper.dto.GrsciCollConceptLinkDto;

import java.util.List;
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
   * Creates a new concept entry (insert if not exists).
   *
   * @param concept The concept to create.
   */
  void create(GrSciCollVocabConceptDto concept);

  /**
   * Updates the changeable fields (deprecated, deprecated_by, replaced_by_key).
   *
   * @param concept The concept with updated changeable fields.
   */
  void update(GrSciCollVocabConceptDto concept);

  /**
   * Gets a concept by vocabulary concept key.
   *
   * @param conceptKey The vocabulary concept key
   * @return The concept, or null if not found
   */
  GrSciCollVocabConceptDto getByConceptKey(@Param("conceptKey") Long conceptKey);

  /**
   * Gets the concept key for a given vocabulary name and concept name.
   *
   * @param vocabularyName The vocabulary name (e.g., "Discipline")
   * @param conceptName The concept name (e.g., "Botany")
   * @return The concept key, or null if not found
   */
  Long getConceptKeyByVocabularyAndName(
      @Param("vocabularyName") String vocabularyName,
      @Param("conceptName") String conceptName);

  /**
   * Gets all concepts (including deprecated) for a vocabulary.
   * Used to compare with incoming vocabulary releases.
   *
   * @param vocabularyName The vocabulary name
   * @return List of all concepts in the vocabulary
   */
  List<GrSciCollVocabConceptDto> getAllConceptsByVocabulary(
      @Param("vocabularyName") String vocabularyName);

  /**
   * Gets existing institution concept links for a specific vocabulary.
   *
   * @param vocabularyName The vocabulary name
   * @return List of entity-concept links
   */
  List<GrsciCollConceptLinkDto> getInstitutionConceptLinksByVocabulary(
      @Param("vocabularyName") String vocabularyName);

  /**
   * Gets existing collection concept links for a specific vocabulary.
   *
   * @param vocabularyName The vocabulary name
   * @return List of entity-concept links
   */
  List<GrsciCollConceptLinkDto> getCollectionConceptLinksByVocabulary(
      @Param("vocabularyName") String vocabularyName);

  /**
   * Finds the current active concept key, following replacement chain if necessary.
   *
   * @param conceptKey The starting concept key
   * @return The active concept key, or null if not found/deprecated without replacement
   */
  Long getActiveConceptKey(@Param("conceptKey") Long conceptKey);

  /**
   * Inserts a link between an institution and a concept.
   *
   * @param institutionKey The institution UUID
   * @param conceptKey The vocabulary concept key
   */
  void insertInstitutionConcept(
      @Param("institutionKey") UUID institutionKey,
      @Param("conceptKey") Long conceptKey);

  /**
   * Deletes all concept links for a given institution.
   *
   * @param institutionKey The institution UUID
   */
  void deleteInstitutionConcepts(@Param("institutionKey") UUID institutionKey);

  /**
   * Deletes a specific institution concept link.
   *
   * @param institutionKey The institution UUID
   * @param conceptKey The concept key
   */
  void deleteInstitutionConcept(
      @Param("institutionKey") UUID institutionKey,
      @Param("conceptKey") Long conceptKey);

  /**
   * Inserts a link between a collection and a concept.
   *
   * @param collectionKey The collection UUID
   * @param conceptKey The vocabulary concept key
   */
  void insertCollectionConcept(
      @Param("collectionKey") UUID collectionKey,
      @Param("conceptKey") Long conceptKey);

  /**
   * Deletes all concept links for a given collection.
   *
   * @param collectionKey The collection UUID
   */
  void deleteCollectionConcepts(@Param("collectionKey") UUID collectionKey);

  /**
   * Deletes a specific collection concept link.
   *
   * @param collectionKey The collection UUID
   * @param conceptKey The concept key
   */
  void deleteCollectionConcept(
      @Param("collectionKey") UUID collectionKey,
      @Param("conceptKey") Long conceptKey);

}
