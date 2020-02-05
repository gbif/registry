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
package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.vocabulary.IdentifierType;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

/** Mappers that perform operations on network entities. */
public interface NetworkEntityMapper<T extends NetworkEntity> {

  /**
   * This gets the instance in question. Note that this does return deleted items.
   *
   * @param key of the network entity to fetch
   * @return either the requested network entity or {@code null} if it couldn't be found
   */
  T get(@Param("key") UUID key);

  String title(@Param("key") UUID key);

  void create(T entity);

  void delete(@Param("key") UUID key);

  void update(T entity);

  List<T> list(@Nullable @Param("page") Pageable page);

  List<T> search(@Nullable @Param("query") String query, @Nullable @Param("page") Pageable page);

  int count();

  int count(@Nullable @Param("query") String query);

  long countByIdentifier(
      @Nullable @Param("type") IdentifierType type, @Param("identifier") String identifier);

  List<T> listByIdentifier(
      @Nullable @Param("type") IdentifierType type,
      @Param("identifier") String identifier,
      @Param("page") Pageable page);
}
