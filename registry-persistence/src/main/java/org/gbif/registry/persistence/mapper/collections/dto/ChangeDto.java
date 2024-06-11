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

/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChangeDto implements Serializable {
  private String fieldName;
  private Class<?> fieldType;
  private String fieldGenericTypeName;
  private transient Object suggested;
  private transient Object previous;
  private Date created;
  private String author;
  private boolean overwritten;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ChangeDto changeDto = (ChangeDto) o;
    return overwritten == changeDto.overwritten
        && Objects.equals(fieldName, changeDto.fieldName)
        && Objects.equals(fieldType, changeDto.fieldType)
        && Objects.equals(fieldGenericTypeName, changeDto.fieldGenericTypeName)
        && Objects.equals(suggested, changeDto.suggested)
        && Objects.equals(previous, changeDto.previous)
        && Objects.equals(created, changeDto.created)
        && Objects.equals(author, changeDto.author);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        fieldName,
        fieldType,
        fieldGenericTypeName,
        suggested,
        previous,
        created,
        author,
        overwritten);
  }
}
