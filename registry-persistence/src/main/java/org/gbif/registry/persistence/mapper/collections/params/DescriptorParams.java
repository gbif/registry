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

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.vocabulary.Country;

import java.time.LocalDate;
import java.util.List;

import javax.annotation.Nullable;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DescriptorParams {

  @Nullable String query;
  @Nullable Long descriptorGroupKey;
  @Nullable List<String> usageKey;
  @Nullable List<String> usageName;
  @Nullable List<String> usageRank;
  @Nullable List<String> taxonKey;
  @Nullable List<Country> country;
  @Nullable List<RangeParam<Integer>> individualCount;
  @Nullable List<String> identifiedBy;
  @Nullable List<RangeParam<LocalDate>> dateIdentified;
  @Nullable List<String> typeStatus;
  @Nullable List<String> recordedBy;
  @Nullable List<String> discipline;
  @Nullable List<String> objectClassification;
  @Nullable List<String> biome;
  @Nullable List<String> biomeType;
  @Nullable List<String> issues;
  @Nullable Pageable page;
}
