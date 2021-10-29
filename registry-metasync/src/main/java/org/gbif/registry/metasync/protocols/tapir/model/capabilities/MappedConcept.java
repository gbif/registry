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
package org.gbif.registry.metasync.protocols.tapir.model.capabilities;

import org.apache.commons.digester3.annotations.rules.ObjectCreate;
import org.apache.commons.digester3.annotations.rules.SetProperty;

import com.google.common.base.Objects;

@ObjectCreate(pattern = "response/capabilities/concepts/schema/mappedConcept")
public class MappedConcept {

  private static final String BASE_PATH = "response/capabilities/concepts/schema/mappedConcept";

  @SetProperty(pattern = BASE_PATH, attributeName = "id")
  private String id;

  @SetProperty(pattern = BASE_PATH, attributeName = "searchable")
  private boolean searchable = true;

  @SetProperty(pattern = BASE_PATH, attributeName = "required")
  private boolean required;

  @SetProperty(pattern = BASE_PATH, attributeName = "alias")
  private String alias;

  @SetProperty(pattern = BASE_PATH, attributeName = "dataType")
  private String dataType = "http://www.w3.org/2001/XMLSchema#string";

  public String getDataType() {
    return dataType;
  }

  public void setDataType(String dataType) {
    this.dataType = dataType;
  }

  public String getAlias() {
    return alias;
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }

  public boolean isRequired() {
    return required;
  }

  public void setRequired(boolean required) {
    this.required = required;
  }

  public boolean isSearchable() {
    return searchable;
  }

  public void setSearchable(boolean searchable) {
    this.searchable = searchable;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("id", id)
        .add("searchable", searchable)
        .add("required", required)
        .add("alias", alias)
        .add("dataType", dataType)
        .toString();
  }
}
