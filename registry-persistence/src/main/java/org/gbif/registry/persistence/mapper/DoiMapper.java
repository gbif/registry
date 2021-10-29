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

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.registry.domain.doi.DoiType;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/** MyBatis mapper to store DOIs and their status in the registry db. */
@Repository
public interface DoiMapper {

  DoiData get(@Param("doi") DOI doi);

  DoiType getType(@Param("doi") DOI doi);

  List<Map<String, Object>> list(
      @Nullable @Param("status") DoiStatus status,
      @Nullable @Param("type") DoiType type,
      @Nullable @Param("page") Pageable page);

  String getMetadata(@Param("doi") DOI doi);

  void create(@Param("doi") DOI doi, @Param("type") DoiType type);

  void update(@Param("doi") DOI doi, @Param("doiData") DoiData doiData, @Param("xml") String xml);

  void delete(@Param("doi") DOI doi);
}
