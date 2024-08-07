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

import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.registry.persistence.mapper.collections.dto.InstitutionGeoJsonDto;
import org.gbif.registry.persistence.mapper.collections.dto.InstitutionMatchedDto;
import org.gbif.registry.persistence.mapper.collections.params.InstitutionListParams;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/** Mapper for {@link Institution} entities. */
@Repository
public interface InstitutionMapper
    extends BaseMapper<Institution>, LookupMapper<InstitutionMatchedDto> {

  List<Institution> list(@Param("params") InstitutionListParams searchParams);

  long count(@Param("params") InstitutionListParams searchParams);

  /** A simple suggest by title service. */
  List<KeyCodeNameResult> suggest(@Nullable @Param("q") String q);

  /**
   * Finds an institution by any of its identifiers.
   *
   * @return the keys of the institutions
   */
  List<UUID> findByIdentifier(@Nullable @Param("identifier") String identifier);

  void convertToCollection(
      @Param("institutionKey") UUID institutionKey, @Param("collectionKey") UUID collectionKey);

  List<InstitutionGeoJsonDto> listGeoJson(@Param("params") InstitutionListParams searchParams);

  List<UUID> getAllKeys();
}
