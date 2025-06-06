package org.gbif.registry.persistence.mapper;

import org.gbif.registry.persistence.dto.GrSciCollVocabFacetDto;

import java.util.UUID;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * Mapper for grscicoll_vocab_facet table operations.
 * Handles vocabulary facet definitions and their hierarchical relationships.
 */
@Repository
public interface GrScicollVocabFacetMapper {

  /**
   * Creates a new facet entry.
   *
   * @param facet The facet to create.
   */
  void create(GrSciCollVocabFacetDto facet);

    /**
   * Updates an existing facet entry.
   * The update is typically based on the ID of the GrSciCollVocabFacetDto.
   *
   * @param facet The facet entry with updated information.
   * @return The number of rows affected.
   */
  int update(GrSciCollVocabFacetDto facet);

  /**
   * Deletes all facets associated with a given vocabulary name.
   *
   * @param vocabularyName The name of the vocabulary whose facets should be deleted.
   */
  void deleteByVocabularyName(@Param("vocabularyName") String vocabularyName);

  /**
   * Gets the facet ID for a given vocabulary name and facet name.
   *
   * @param vocabularyName The vocabulary name (e.g., "Discipline")
   * @param facetName The facet name (e.g., "Botany")
   * @return The facet ID, or null if not found
   */
  Integer getFacetIdByVocabularyAndName(
      @Param("vocabularyName") String vocabularyName,
      @Param("facetName") String facetName);

  /**
   * Inserts a link between an institution and a facet.
   *
   * @param institutionKey The institution UUID
   * @param facetId The facet ID
   */
  void insertInstitutionFacetLink(
      @Param("institutionKey") UUID institutionKey,
      @Param("facetId") Integer facetId);

  /**
   * Deletes all facet links for a given institution.
   *
   * @param institutionKey The institution UUID
   */
  void deleteInstitutionFacetLinks(@Param("institutionKey") UUID institutionKey);

  /**
   * Inserts a link between a collection and a facet.
   *
   * @param collectionKey The collection UUID
   * @param facetId The facet ID
   */
  void insertCollectionFacetLink(
      @Param("collectionKey") UUID collectionKey,
      @Param("facetId") Integer facetId);

  /**
   * Deletes all facet links for a given collection.
   *
   * @param collectionKey The collection UUID
   */
  void deleteCollectionFacetLinks(@Param("collectionKey") UUID collectionKey);

}
