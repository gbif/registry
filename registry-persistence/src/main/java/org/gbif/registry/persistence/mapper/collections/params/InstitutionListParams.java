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
package org.gbif.registry.persistence.mapper.collections.params;

import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.gbif.api.vocabulary.collections.InstitutionFacetParameter;

@Getter
@Setter
@SuperBuilder
public class InstitutionListParams extends ListParams {

  @Nullable List<String> types;

  @Nullable List<String> institutionalGovernances;

  @Nullable List<String> disciplines;

  // facets
  @Nullable InstitutionFacetParameter facet;

  public boolean isArrayFieldFacet() {
    return facet == InstitutionFacetParameter.TYPE
      || facet == InstitutionFacetParameter.DISCIPLINE
      || facet == InstitutionFacetParameter.INSTITUTIONAL_GOVERNANCE;
  }
}
