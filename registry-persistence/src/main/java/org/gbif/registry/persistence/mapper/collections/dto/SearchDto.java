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

import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Rank;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SearchDto extends BaseSearchDto {

  private float score;
  private String type;
  private UUID institutionKey;
  private String institutionCode;
  private String institutionName;

  // descriptors fields
  private Long descriptorKey;
  private Long descriptorGroupKey;
  private Long descriptorUsageKey;
  private String descriptorUsageName;
  private Rank descriptorUsageRank;
  private Country descriptorCountry;
  private Integer descriptorIndividualCount;
  private List<String> descriptorIdentifiedBy;
  private Date descriptorDateIdentified;
  private List<String> descriptorTypeStatus;
  private List<String> descriptorRecordedBy;
  private String descriptorDiscipline;
  private String descriptorObjectClassification;
  private List<String> descriptorIssues;
}
