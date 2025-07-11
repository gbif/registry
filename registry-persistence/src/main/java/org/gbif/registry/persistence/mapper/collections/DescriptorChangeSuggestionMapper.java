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
package org.gbif.registry.persistence.mapper.collections;

import org.gbif.api.model.collections.descriptors.DescriptorChangeSuggestion;
import org.gbif.api.model.collections.suggestions.Status;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.vocabulary.Country;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Param;
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
   * @param country Filter by country
   * @return List of descriptor change suggestions
   */
  List<DescriptorChangeSuggestion> list(@Param("page") Pageable pageable,
    @Param("status") Status status,
    @Param("type") Type type,
    @Param("proposerEmail") String proposerEmail,
    @Param("collectionKey") UUID collectionKey,
    @Param("country") Country country);


  /**
   * Counts the total number of descriptor change suggestions based on provided filters.
   *
   * @param status Filter by status
   * @param type Filter by type
   * @param proposerEmail Filter by proposer's email
   * @param collectionKey Filter by collection key
   * @param country Filter by country
   * @return Total number of matching suggestions
   */
  long count(@Param("status") Status status,
    @Param("type") Type type,
    @Param("proposerEmail") String proposerEmail,
    @Param("collectionKey") UUID collectionKey,
    @Param("country") Country country);
}
