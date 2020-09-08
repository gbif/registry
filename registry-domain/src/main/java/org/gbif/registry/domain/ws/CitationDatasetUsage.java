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

public class CitationDatasetUsage {

  private UUID datasetKey;
  private DOI datasetDoi;
  private Long numberRecords;

  public CitationDatasetUsage() {
  }

  public CitationDatasetUsage(UUID datasetKey, DOI datasetDoi, Long numberRecords) {
    this.datasetKey = datasetKey;
    this.datasetDoi = datasetDoi;
    this.numberRecords = numberRecords;
  }

  public UUID getDatasetKey() {
    return datasetKey;
  }

  public void setDatasetKey(UUID datasetKey) {
    this.datasetKey = datasetKey;
  }

  public DOI getDatasetDoi() {
    return datasetDoi;
  }

  public void setDatasetDoi(DOI datasetDoi) {
    this.datasetDoi = datasetDoi;
  }

  public Long getNumberRecords() {
    return numberRecords;
  }

  public void setNumberRecords(Long numberRecords) {
    this.numberRecords = numberRecords;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CitationDatasetUsage that = (CitationDatasetUsage) o;
    return Objects.equals(datasetKey, that.datasetKey) &&
        Objects.equals(datasetDoi, that.datasetDoi) &&
        Objects.equals(numberRecords, that.numberRecords);
  }

  @Override
  public int hashCode() {
    return Objects.hash(datasetKey, datasetDoi, numberRecords);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", CitationDatasetUsage.class.getSimpleName() + "[", "]")
        .add("datasetKey=" + datasetKey)
        .add("datasetDoi=" + datasetDoi)
        .add("numberRecords=" + numberRecords)
        .toString();
  }
}
