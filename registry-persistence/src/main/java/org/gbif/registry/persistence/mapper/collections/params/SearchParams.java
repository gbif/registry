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

import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TypeStatus;

import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchParams {

  @Nullable String q;
  Boolean highlight;
  String type;
  Boolean displayOnNHCPortal;
  @Nullable List<Country> countries;
  @Nullable List<Country> regionCountries;
  @Nullable String city;

  Integer limit;
  Integer offset;

  // collection fields

  // descriptors fields
  List<String> usageName;
  List<Integer> usageKey;
  List<Rank> usageRank;
  List<Integer> taxonKey;
  @Nullable List<Country> descriptorCountry;
  @Nullable RangeParam individualCount;
  @Nullable List<String> identifiedBy;
  @Nullable
  Date dateIdentified;
  @Nullable Date dateIdentifiedFrom;
  @Nullable Date dateIdentifiedBefore;
  @Nullable List<TypeStatus> typeStatus;
  @Nullable List<String> recordedBy;
  @Nullable List<String> discipline;
  @Nullable List<String> objectClassification;
  @Nullable List<String> biome;
  @Nullable List<String> biomeType;
  @Nullable List<String> issues;
}
