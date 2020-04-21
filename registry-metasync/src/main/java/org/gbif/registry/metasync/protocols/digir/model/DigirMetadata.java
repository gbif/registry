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
package org.gbif.registry.metasync.protocols.digir.model;

import java.net.URI;
import java.util.List;

import org.apache.commons.digester3.annotations.rules.BeanPropertySetter;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;
import org.apache.commons.digester3.annotations.rules.SetNext;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

@ObjectCreate(pattern = "response/content/metadata/provider")
public class DigirMetadata {

  @BeanPropertySetter(pattern = "response/content/metadata/provider/name")
  private String name;

  @BeanPropertySetter(pattern = "response/content/metadata/provider/accessPoint")
  private URI accessPoint;

  @BeanPropertySetter(pattern = "response/content/metadata/provider/implementation")
  private String implementation;

  private DigirHost host;
  private List<DigirResource> resources = Lists.newArrayList();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public URI getAccessPoint() {
    return accessPoint;
  }

  public void setAccessPoint(URI accessPoint) {
    this.accessPoint = accessPoint;
  }

  public String getImplementation() {
    return implementation;
  }

  public void setImplementation(String implementation) {
    this.implementation = implementation;
  }

  public DigirHost getHost() {
    return host;
  }

  @SetNext
  public void setHost(DigirHost host) {
    this.host = host;
  }

  public List<DigirResource> getResources() {
    return resources;
  }

  public void setResources(List<DigirResource> resources) {
    this.resources = resources;
  }

  @SetNext
  public void addResource(DigirResource resource) {
    resources.add(resource);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("name", name)
        .add("accessPoint", accessPoint)
        .add("implementation", implementation)
        .add("host", host)
        .add("resources", resources)
        .toString();
  }
}
