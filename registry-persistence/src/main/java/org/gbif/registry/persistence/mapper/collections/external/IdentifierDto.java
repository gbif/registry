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
package org.gbif.registry.persistence.mapper.collections.external;

import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.gbif.api.vocabulary.IdentifierType;

@Setter
@Getter
public class IdentifierDto {

  private UUID entityKey;
  private IdentifierType type;
  private String identifier;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    IdentifierDto that = (IdentifierDto) o;
    return Objects.equals(entityKey, that.entityKey)
        && type == that.type
        && Objects.equals(identifier, that.identifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entityKey, type, identifier);
  }
}
