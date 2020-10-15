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

import java.io.Serializable;
import java.net.URI;
import java.util.Date;
import java.util.Objects;
import java.util.StringJoiner;

public class DerivedDataset implements Serializable {

  private DOI doi;
  private DOI originalDownloadDOI;
  private String description;
  private String citation;
  private String title;
  private URI target;
  private String createdBy;
  private String modifiedBy;
  private Date registrationDate;
  private Date created;
  private Date modified;

  public DOI getDoi() {
    return doi;
  }

  public void setDoi(DOI doi) {
    this.doi = doi;
  }

  public DOI getOriginalDownloadDOI() {
    return originalDownloadDOI;
  }

  public void setOriginalDownloadDOI(DOI originalDownloadDOI) {
    this.originalDownloadDOI = originalDownloadDOI;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getCitation() {
    return citation;
  }

  public void setCitation(String citation) {
    this.citation = citation;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public URI getTarget() {
    return target;
  }

  public void setTarget(URI target) {
    this.target = target;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public String getModifiedBy() {
    return modifiedBy;
  }

  public void setModifiedBy(String modifiedBy) {
    this.modifiedBy = modifiedBy;
  }

  public Date getRegistrationDate() {
    return registrationDate;
  }

  public void setRegistrationDate(Date registrationDate) {
    this.registrationDate = registrationDate;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public Date getModified() {
    return modified;
  }

  public void setModified(Date modified) {
    this.modified = modified;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DerivedDataset derivedDataset1 = (DerivedDataset) o;
    return Objects.equals(doi, derivedDataset1.doi)
        && Objects.equals(originalDownloadDOI, derivedDataset1.originalDownloadDOI)
        && Objects.equals(description, derivedDataset1.description)
        && Objects.equals(citation, derivedDataset1.citation)
        && Objects.equals(title, derivedDataset1.title)
        && Objects.equals(target, derivedDataset1.target)
        && Objects.equals(createdBy, derivedDataset1.createdBy)
        && Objects.equals(modifiedBy, derivedDataset1.modifiedBy)
        && Objects.equals(registrationDate, derivedDataset1.registrationDate)
        && Objects.equals(created, derivedDataset1.created)
        && Objects.equals(modified, derivedDataset1.modified);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        doi,
        originalDownloadDOI,
        description,
        citation,
        title,
        target,
        createdBy,
        modifiedBy,
        registrationDate,
        created,
        modified);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", DerivedDataset.class.getSimpleName() + "[", "]")
        .add("doi=" + doi)
        .add("originalDownloadDOI=" + originalDownloadDOI)
        .add("description=" + description)
        .add("citation='" + citation + "'")
        .add("title='" + title + "'")
        .add("target=" + target)
        .add("createdBy='" + createdBy + "'")
        .add("modifiedBy='" + modifiedBy + "'")
        .add("registrationDate=" + registrationDate)
        .add("created=" + created)
        .add("modified=" + modified)
        .toString();
  }
}
