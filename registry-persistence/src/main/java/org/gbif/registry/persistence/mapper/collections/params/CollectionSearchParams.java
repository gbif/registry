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

import org.gbif.api.vocabulary.collections.AccessionStatus;
import org.gbif.api.vocabulary.collections.CollectionContentType;
import org.gbif.api.vocabulary.collections.PreservationType;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class CollectionSearchParams extends SearchParams {

  @Nullable UUID institutionKey;
  @Nullable List<CollectionContentType> contentTypes;
  @Nullable List<PreservationType> preservationTypes;
  @Nullable AccessionStatus accessionStatus;
  @Nullable Boolean personalCollection;
}
