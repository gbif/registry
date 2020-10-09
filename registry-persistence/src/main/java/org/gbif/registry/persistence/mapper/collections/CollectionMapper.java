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

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.persistence.ContactableMapper;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/** Mapper for {@link Collection} entities. */
@Repository
public interface CollectionMapper
    extends BaseMapper<Collection>, ContactableMapper, LookupMapper<Collection> {

  List<Collection> list(
      @Nullable @Param("institutionKey") UUID institutionKey,
      @Nullable @Param("contactKey") UUID contactKey,
      @Nullable @Param("query") String query,
      @Nullable @Param("code") String code,
      @Nullable @Param("name") String name,
      @Nullable @Param("alternativeCode") String alternativeCode,
      @Nullable @Param("machineTagNamespace") String machineTagNamespace,
      @Nullable @Param("machineTagName") String machineTagName,
      @Nullable @Param("machineTagValue") String machineTagValue,
      @Nullable @Param("identifierType") IdentifierType identifierType,
      @Nullable @Param("identifier") String identifier,
      @Nullable @Param("page") Pageable page);

  long count(
      @Nullable @Param("institutionKey") UUID institutionKey,
      @Nullable @Param("contactKey") UUID contactKey,
      @Nullable @Param("query") String query,
      @Nullable @Param("code") String code,
      @Nullable @Param("name") String name,
      @Nullable @Param("alternativeCode") String alternativeCode,
      @Nullable @Param("machineTagNamespace") String machineTagNamespace,
      @Nullable @Param("machineTagName") String machineTagName,
      @Nullable @Param("machineTagValue") String machineTagValue,
      @Nullable @Param("identifierType") IdentifierType identifierType,
      @Nullable @Param("identifier") String identifier);

  /** A simple suggest by title service. */
  List<KeyCodeNameResult> suggest(@Nullable @Param("q") String q);

  /** @return the collections marked as deleted */
  List<Collection> deleted(@Param("page") Pageable page);

  /** @return the count of the collections marked as deleted. */
  long countDeleted();

  /**
   * Finds a collection by any of its identifiers.
   *
   * @return the keys of the collections
   */
  List<UUID> findByIdentifier(@Nullable @Param("identifier") String identifier);

  /**
   * Gets the institution key of the specified collection.
   *
   * @param collectionKey key of the collection whose identifier key we want to get
   * @return institution key
   */
  UUID getInstitutionKey(@Param("collectionKey") UUID collectionKey);
}
