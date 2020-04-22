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

import org.apache.commons.digester3.annotations.rules.ObjectCreate;
import org.apache.commons.digester3.annotations.rules.SetProperty;
import org.joda.time.DateTime;

import com.google.common.base.Objects;

@ObjectCreate(pattern = "response/capabilities/archives/archive")
public class Archive {

  private static final String BASE_PATH = "response/capabilities/archives/archive";

  @SetProperty(pattern = BASE_PATH, attributeName = "format")
  private String format;

  @SetProperty(pattern = BASE_PATH, attributeName = "location")
  private URI location;

  @SetProperty(pattern = BASE_PATH, attributeName = "creation")
  private DateTime creation;

  @SetProperty(pattern = BASE_PATH, attributeName = "compression")
  private String compression;

  @SetProperty(pattern = BASE_PATH, attributeName = "numberOfRecords")
  private int numberOfRecords;

  @SetProperty(pattern = BASE_PATH, attributeName = "outputModel")
  private URI outputModel;

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public URI getLocation() {
    return location;
  }

  public void setLocation(URI location) {
    this.location = location;
  }

  public DateTime getCreation() {
    return creation;
  }

  public void setCreation(DateTime creation) {
    this.creation = creation;
  }

  public String getCompression() {
    return compression;
  }

  public void setCompression(String compression) {
    this.compression = compression;
  }

  public int getNumberOfRecords() {
    return numberOfRecords;
  }

  public void setNumberOfRecords(int numberOfRecords) {
    this.numberOfRecords = numberOfRecords;
  }

  public URI getOutputModel() {
    return outputModel;
  }

  public void setOutputModel(URI outputModel) {
    this.outputModel = outputModel;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("format", format)
        .add("location", location)
        .add("creation", creation)
        .add("compression", compression)
        .add("numberOfRecords", numberOfRecords)
        .add("outputModel", outputModel)
        .toString();
  }
}
