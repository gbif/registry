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
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.util.HttpURI;

import java.io.Serializable;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

public class CitationCreationRequest implements Serializable {

  private DOI originalDownloadDOI;
  private String title;
  private String creator;
  private URI target;
  private Date registrationDate;
  private Map<String, Long> relatedDatasets = new HashMap<>();

  public DOI getOriginalDownloadDOI() {
    return originalDownloadDOI;
  }

  public void setOriginalDownloadDOI(DOI originalDownloadDOI) {
    this.originalDownloadDOI = originalDownloadDOI;
  }

  @NotNull
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  @Null(groups = {PrePersist.class})
  public String getCreator() {
    return creator;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  @NotNull
  @HttpURI
  public URI getTarget() {
    return target;
  }

  public void setTarget(URI target) {
    this.target = target;
  }

  public Date getRegistrationDate() {
    return registrationDate;
  }

  public void setRegistrationDate(Date registrationDate) {
    this.registrationDate = registrationDate;
  }

  public Map<String, Long> getRelatedDatasets() {
    return relatedDatasets;
  }

  public void setRelatedDatasets(Map<String, Long> relatedDatasets) {
    this.relatedDatasets = relatedDatasets;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CitationCreationRequest that = (CitationCreationRequest) o;
    return Objects.equals(originalDownloadDOI, that.originalDownloadDOI)
        && Objects.equals(title, that.title)
        && Objects.equals(creator, that.creator)
        && Objects.equals(target, that.target)
        && Objects.equals(registrationDate, that.registrationDate)
        && Objects.equals(relatedDatasets, that.relatedDatasets);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        originalDownloadDOI, title, creator, target, registrationDate, relatedDatasets);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", CitationCreationRequest.class.getSimpleName() + "[", "]")
        .add("originalDownloadDOI=" + originalDownloadDOI)
        .add("title='" + title + "'")
        .add("creator='" + creator + "'")
        .add("target=" + target)
        .add("registrationDate=" + registrationDate)
        .add("relatedDatasets=" + relatedDatasets)
        .toString();
  }
}
