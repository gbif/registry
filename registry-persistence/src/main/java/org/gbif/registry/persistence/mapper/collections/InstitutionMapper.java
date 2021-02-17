/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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

import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.registry.persistence.ContactableMapper;
import org.gbif.registry.persistence.mapper.collections.dto.InstitutionMatchedDto;
import org.gbif.registry.persistence.mapper.collections.params.InstitutionSearchParams;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/** Mapper for {@link Institution} entities. */
@Repository
public interface InstitutionMapper
    extends BaseMapper<Institution>,
        ContactableMapper,
        LookupMapper<InstitutionMatchedDto>,
        OccurrenceMappeableMapper,
        MergeableMapper {

  List<Institution> list(
      @Param("params") InstitutionSearchParams searchParams,
      @Nullable @Param("page") Pageable page);

  long count(@Param("params") InstitutionSearchParams searchParams);

  /** A simple suggest by title service. */
  List<KeyCodeNameResult> suggest(@Nullable @Param("q") String q);

  /** @return the institutions marked as deleted */
  List<Institution> deleted(@Param("page") Pageable page);

  /** @return the count of the institutions marked as deleted. */
  long countDeleted();

  /**
   * Finds an institution by any of its identifiers.
   *
   * @return the keys of the institutions
   */
  List<UUID> findByIdentifier(@Nullable @Param("identifier") String identifier);

  void convertToCollection(
      @Param("institutionKey") UUID institutionKey, @Param("collectionKey") UUID collectionKey);

  List<Institution> findPossibleDuplicates(@Param("entity") Institution institution);
}
