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
import java.util.UUID;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.SortOrder;
import org.gbif.api.vocabulary.collections.CollectionsSortField;
import org.gbif.api.vocabulary.collections.MasterSourceType;
import org.gbif.api.vocabulary.collections.Source;

@Getter
@Setter
@SuperBuilder
public abstract class ListParams {

  @Nullable Boolean highlight;
  @Nullable String query;
  @Nullable List<String> code;
  @Nullable List<String> name;
  @Nullable List<String> alternativeCode;
  @Nullable List<String> machineTagNamespace;
  @Nullable List<String> machineTagName;
  @Nullable List<String> machineTagValue;
  @Nullable List<IdentifierType> identifierType;
  @Nullable List<String> identifier;
  @Nullable List<Country> countries;
  @Nullable List<Country> regionCountries;
  @Nullable List<String> city;
  @Nullable List<String> fuzzyName;
  @Nullable List<Boolean> active;
  @Nullable List<MasterSourceType> masterSourceType;
  @Nullable List<RangeParam<Integer>> numberSpecimens;
  @Nullable List<Boolean> displayOnNHCPortal;
  @Nullable List<RangeParam<Integer>> occurrenceCount;
  @Nullable List<RangeParam<Integer>> typeSpecimenCount;
  @Nullable List<UUID> institutionKeys;
  @Nullable List<Source> source;
  @Nullable List<String> sourceId;
  @Nullable CollectionsSortField sortBy;
  @Nullable SortOrder sortOrder;
  @Nullable Boolean deleted;
  @Nullable List<UUID> replacedBy;
  @Nullable String contactUserId;
  @Nullable String contactEmail;
  @Nullable Pageable page;
  @Nullable Pageable facetPage;
  @Nullable Integer facetMinCount;
}
