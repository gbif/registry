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

import org.gbif.api.vocabulary.Country;
import org.gbif.registry.persistence.mapper.collections.dto.SearchDto;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

public interface CollectionsSearchMapper {

  List<SearchDto> search(
      @Nullable @Param("q") String query,
      @Param("highlight") boolean highlight,
      @Nullable @Param("type") String type,
      @Nullable @Param("displayOnNHCPortal") Boolean displayOnNHCPortal,
      @Nullable @Param("country") Country country,
      @Param("limit") int limit);
}
