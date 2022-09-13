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
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.collections.MasterSourceType;

import java.util.UUID;

import javax.annotation.Nullable;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public abstract class SearchParams {

  @Nullable UUID contactKey;
  @Nullable String query;
  @Nullable String code;
  @Nullable String name;
  @Nullable String alternativeCode;
  @Nullable String machineTagNamespace;
  @Nullable String machineTagName;
  @Nullable String machineTagValue;
  @Nullable IdentifierType identifierType;
  @Nullable String identifier;
  @Nullable Country country;
  @Nullable String city;
  @Nullable String fuzzyName;
  @Nullable Boolean active;
  @Nullable MasterSourceType masterSourceType;
  @Nullable RangeParam numberSpecimens;
}
