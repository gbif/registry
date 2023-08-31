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
package org.gbif.registry.persistence.mapper.params;

import org.gbif.api.vocabulary.InstallationType;

import java.util.UUID;

import javax.annotation.Nullable;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public class InstallationListParams extends BaseListParams {

  @Nullable private InstallationType type;
  @Nullable private UUID organizationKey;
  @Nullable private UUID endorsedByNodeKey;

  public static InstallationListParams from(BaseListParams params) {
    return BaseListParams.copy(InstallationListParams.builder().build(), params);
  }
}
