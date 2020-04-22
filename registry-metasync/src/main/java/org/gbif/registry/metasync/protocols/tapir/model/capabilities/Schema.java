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
package org.gbif.registry.metasync.protocols.tapir.model.capabilities;

import java.net.URI;
import java.util.List;

import org.apache.commons.digester3.annotations.rules.ObjectCreate;
import org.apache.commons.digester3.annotations.rules.SetNext;
import org.apache.commons.digester3.annotations.rules.SetProperty;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

@ObjectCreate(pattern = "response/capabilities/concepts/schema")
public class Schema {

  private static final String BASE_PATH = "response/capabilities/concepts/schema";

  @SetProperty(pattern = BASE_PATH, attributeName = "namespace")
  private URI namespace;

  @SetProperty(pattern = BASE_PATH, attributeName = "location")
  private URI location;

  @SetProperty(pattern = BASE_PATH, attributeName = "alias")
  private String alias;

  private final List<MappedConcept> concepts = Lists.newArrayList();

  public URI getNamespace() {
    return namespace;
  }

  public void setNamespace(URI namespace) {
    this.namespace = namespace;
  }

  public URI getLocation() {
    return location;
  }

  public void setLocation(URI location) {
    this.location = location;
  }

  public String getAlias() {
    return alias;
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }

  public List<MappedConcept> getConcepts() {
    return concepts;
  }

  @SetNext
  public void addMappedConcept(MappedConcept concept) {
    concepts.add(concept);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("namespace", namespace)
        .add("location", location)
        .add("alias", alias)
        .add("concepts", concepts)
        .toString();
  }
}
