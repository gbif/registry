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
package org.gbif.registry.domain.ws;

import org.gbif.api.model.common.DOI;

import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;

public class DerivedDatasetUsage {

  private UUID datasetKey;
  private DOI datasetDOI;
  private String datasetTitle;
  private DOI derivedDatasetDOI;
  private Long numberRecords;
  private String citation;

  public DerivedDatasetUsage() {}

  public DerivedDatasetUsage(
      UUID datasetKey,
      DOI datasetDOI,
      DOI derivedDatasetDOI,
      Long numberRecords,
      String datasetTitle) {
    this.datasetKey = datasetKey;
    this.datasetDOI = datasetDOI;
    this.derivedDatasetDOI = derivedDatasetDOI;
    this.numberRecords = numberRecords;
    this.datasetTitle = datasetTitle;
  }

  public UUID getDatasetKey() {
    return datasetKey;
  }

  public void setDatasetKey(UUID datasetKey) {
    this.datasetKey = datasetKey;
  }

  public DOI getDatasetDOI() {
    return datasetDOI;
  }

  public void setDatasetDOI(DOI datasetDOI) {
    this.datasetDOI = datasetDOI;
  }

  public String getDatasetTitle() {
    return datasetTitle;
  }

  public void setDatasetTitle(String datasetTitle) {
    this.datasetTitle = datasetTitle;
  }

  public DOI getDerivedDatasetDOI() {
    return derivedDatasetDOI;
  }

  public void setDerivedDatasetDOI(DOI derivedDatasetDOI) {
    this.derivedDatasetDOI = derivedDatasetDOI;
  }

  public Long getNumberRecords() {
    return numberRecords;
  }

  public void setNumberRecords(Long numberRecords) {
    this.numberRecords = numberRecords;
  }

  public String getCitation() {
    return citation;
  }

  public void setCitation(String citation) {
    this.citation = citation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DerivedDatasetUsage that = (DerivedDatasetUsage) o;
    return Objects.equals(datasetKey, that.datasetKey)
        && Objects.equals(datasetDOI, that.datasetDOI)
        && Objects.equals(datasetTitle, that.datasetTitle)
        && Objects.equals(derivedDatasetDOI, that.derivedDatasetDOI)
        && Objects.equals(citation, that.citation)
        && Objects.equals(numberRecords, that.numberRecords);
  }

  @Override
  public int hashCode() {
    return Objects.hash(datasetKey, datasetDOI, datasetTitle, derivedDatasetDOI, numberRecords);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", DerivedDatasetUsage.class.getSimpleName() + "[", "]")
        .add("datasetKey=" + datasetKey)
        .add("datasetDoi=" + datasetDOI)
        .add("datasetTitle=" + datasetTitle)
        .add("derivedDatasetDoi=" + derivedDatasetDOI)
        .add("numberRecords=" + numberRecords)
        .add("citation=" + citation)
        .toString();
  }
}
