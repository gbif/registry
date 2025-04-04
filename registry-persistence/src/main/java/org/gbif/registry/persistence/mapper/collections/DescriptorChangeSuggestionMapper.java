package org.gbif.registry.persistence.mapper.collections;

import java.util.List;

import java.util.UUID;

import org.apache.ibatis.annotations.Param;

import org.gbif.api.model.collections.descriptors.DescriptorChangeSuggestion;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.collections.suggestions.Status;
import org.gbif.api.model.common.paging.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface DescriptorChangeSuggestionMapper {

  /**
   * Inserts a new change suggestion record.
   * The generated key is set into the suggestion object.
   *
   * @param suggestion the DescriptorChangeSugestion object to insert
   */
  void createSuggestion(DescriptorChangeSuggestion suggestion);

  /**
   * Finds a change suggestion by its primary key.
   *
   * @param key the primary key of the suggestion
   * @return the DescriptorChangeSugestion object or null if not found
   */
  DescriptorChangeSuggestion findByKey(@Param("key") long key);

  /**
   * Updates an existing change suggestion record.
   *
   * @param suggestion the DescriptorChangeSugestion object with updated values
   */
  void updateSuggestion(DescriptorChangeSuggestion suggestion);

  /**
   * Retrieves a paginated list of descriptor change suggestions based on provided filters.
   *
   * @param pageable Pagination details (limit, offset, etc.)
   * @param status Filter by status (PENDING, APPROVED, DISCARDED)
   * @param type Filter by type (CREATE, UPDATE, DELETE)
   * @param proposerEmail Filter by proposer's email
   * @param collectionKey Filter by collection key
   * @return List of descriptor change suggestions
   */
  List<DescriptorChangeSuggestion> list(@Param("page") Pageable pageable,
    @Param("status") Status status,
    @Param("type") Type type,
    @Param("proposerEmail") String proposerEmail,
    @Param("collectionKey") UUID collectionKey);


  /**
   * Counts the total number of descriptor change suggestions based on provided filters.
   *
   * @param status Filter by status
   * @param type Filter by type
   * @param proposerEmail Filter by proposer's email
   * @param collectionKey Filter by collection key
   * @return Total number of matching suggestions
   */
  long count(@Param("status") Status status,
    @Param("type") Type type,
    @Param("proposerEmail") String proposerEmail,
    @Param("collectionKey") UUID collectionKey);

  /**
   * Deletes a descriptor change suggestion by its primary key.
   *
   * @param key the primary key of the suggestion to delete
   */
  void deleteSuggestion(@Param("key") long key);
}
