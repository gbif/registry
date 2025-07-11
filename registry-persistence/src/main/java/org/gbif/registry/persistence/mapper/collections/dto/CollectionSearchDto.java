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

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CollectionSearchDto extends SearchDto {

  private List<String> contentTypes = new ArrayList<>();
  private boolean personalCollection;
  private List<String> preservationTypes = new ArrayList<>();
  private String accessionStatus;
  private Integer numberSpecimens;
  private String taxonomicCoverage;
  private String geographicCoverage;
  private MasterSourceType masterSource;
  private String department;
  private String division;
  private Integer occurrenceCount;
  private Integer typeSpecimenCount;
  private String temporalCoverage;
  private Float queryRank;
  private Float queryDescriptorRank;
}
