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
package org.gbif.registry.persistence.mapper.collections.external;

import java.util.Objects;
import java.util.UUID;

public class MachineTagDto {

  private UUID entityKey;
  private String namespace;
  private String name;
  private String value;

  public UUID getEntityKey() {
    return entityKey;
  }

  public void setEntityKey(UUID entityKey) {
    this.entityKey = entityKey;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MachineTagDto that = (MachineTagDto) o;
    return Objects.equals(entityKey, that.entityKey)
        && Objects.equals(namespace, that.namespace)
        && Objects.equals(name, that.name)
        && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entityKey, namespace, name, value);
  }
}
