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
package org.gbif.registry.persistence.mapper.collections.dto;

import org.gbif.api.vocabulary.collections.MasterSourceType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class InstitutionSearchDto extends BaseSearchDto {

  private List<String> types = new ArrayList<>();
  private List<String> institutionalGovernances = new ArrayList<>();
  private List<String> disciplines = new ArrayList<>();
  private BigDecimal latitude;
  private BigDecimal longitude;
  private Integer foundingDate;
  private Integer numberSpecimens;
  private MasterSourceType masterSource;
  private Integer occurrenceCount;
  private Integer typeSpecimenCount;

}
