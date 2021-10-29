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

import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.suggestions.Status;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.registry.persistence.mapper.collections.dto.ChangeSuggestionDto;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChangeSuggestionMapper {

  void create(ChangeSuggestionDto suggestion);

  ChangeSuggestionDto get(@Param("key") int key);

  ChangeSuggestionDto getByKeyAndType(
      @Param("key") int key, @Param("entityType") CollectionEntityType entityType);

  List<ChangeSuggestionDto> list(
      @Param("status") Status status,
      @Param("type") Type type,
      @Param("entityType") CollectionEntityType entityType,
      @Param("proposerEmail") String proposerEmail,
      @Param("entityKey") UUID entityKey,
      @Nullable @Param("page") Pageable page);

  long count(
      @Param("status") Status status,
      @Param("type") Type type,
      @Param("entityType") CollectionEntityType entityType,
      @Param("proposerEmail") String proposerEmail,
      @Param("entityKey") UUID entityKey);

  void update(ChangeSuggestionDto suggestion);
}
