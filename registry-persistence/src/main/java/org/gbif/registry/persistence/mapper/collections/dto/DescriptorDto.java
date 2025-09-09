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

import org.gbif.api.v2.RankedName;
import org.gbif.api.vocabulary.Country;

import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class DescriptorDto {

  private long key;
  private Long descriptorGroupKey;
  private Country country;
  private Integer individualCount;
  private List<String> identifiedBy;
  private Date dateIdentified;
  private List<String> typeStatus;
  private List<String> recordedBy;
  private String discipline;
  private String objectClassificationName;
  private String biome;
  private List<String> issues;
  private List<VerbatimDto> verbatim;
  private String usageKey;
  private String usageName;
  private String usageRank;
  private List<RankedName> taxonClassification;
  private List<String> taxonKeys;
  private String kingdomKey;
  private String kingdomName;
  private String phylumKey;
  private String phylumName;
  private String classKey;
  private String className;
  private String orderKey;
  private String orderName;
  private String familyKey;
  private String familyName;
  private String genusKey;
  private String genusName;
  private String speciesKey;
  private String speciesName;
}
