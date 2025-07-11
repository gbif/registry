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

import org.gbif.registry.persistence.mapper.collections.dto.CollectionSearchDto;
import org.gbif.registry.persistence.mapper.collections.dto.FacetDto;
import org.gbif.registry.persistence.mapper.collections.dto.InstitutionSearchDto;
import org.gbif.registry.persistence.mapper.collections.dto.SearchDto;
import org.gbif.registry.persistence.mapper.collections.params.DescriptorsListParams;
import org.gbif.registry.persistence.mapper.collections.params.FullTextSearchParams;
import org.gbif.registry.persistence.mapper.collections.params.InstitutionListParams;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

public interface CollectionsSearchMapper {

  List<SearchDto> search(@Nullable @Param("params") FullTextSearchParams params);

  List<InstitutionSearchDto> searchInstitutions(
      @Nullable @Param("params") InstitutionListParams listParams);

  long countInstitutions(@Nullable @Param("params") InstitutionListParams listParams);

  List<CollectionSearchDto> searchCollections(@Nullable @Param("params") DescriptorsListParams params);

  long countCollections(@Nullable @Param("params") DescriptorsListParams listParams);

  List<FacetDto> collectionFacet(@Nullable @Param("params") DescriptorsListParams params);

  long collectionFacetCardinality(@Nullable @Param("params") DescriptorsListParams params);

  List<FacetDto> institutionFacet(@Nullable @Param("params") InstitutionListParams params);

  long institutionFacetCardinality(@Nullable @Param("params") InstitutionListParams params);
}
