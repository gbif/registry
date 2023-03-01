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
package org.gbif.registry.domain.ws;

import io.swagger.v3.oas.annotations.media.Schema;

import org.gbif.api.model.common.DOI;

import java.io.Serializable;
import java.net.URI;
import java.util.Date;
import java.util.Objects;
import java.util.StringJoiner;

public class DerivedDataset implements Serializable {

  @Schema(
    description = "The DOI of the derived dataset."
  )
  private DOI doi;

  @Schema(
    description = "The DOI of the source (large) download which has been filtered."
  )
  private DOI originalDownloadDOI;

  @Schema(
    description = "Description of the derived dataset, such as how it was filtered."
  )
  private String description;

  @Schema(
    description = "The citation for the derived dataset."
  )
  private String citation;

  @Schema(
    description = "The human title of the derived dataset."
  )
  private String title;

  @Schema(
    description = "The URL where the derived dataset is deposited."
  )
  private URI sourceUrl;

  @Schema(
    description = "The GBIF user who created the derived dataset."
  )
  private String createdBy;

  @Schema(
    description = "The GBIF user who last modified the derived dataset."
  )
  private String modifiedBy;

  @Schema(
    description = "" // TODO
  )
  private Date registrationDate;

  @Schema(
    description = "The time the derived dataset was created."
  )
  private Date created;

  @Schema(
    description = "The time the derived dataset was last modified."
  )
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

  public URI getSourceUrl() {
    return sourceUrl;
  }

  public void setSourceUrl(URI sourceUrl) {
    this.sourceUrl = sourceUrl;
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
        && Objects.equals(sourceUrl, derivedDataset1.sourceUrl)
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
        sourceUrl,
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
        .add("sourceUrl=" + sourceUrl)
        .add("createdBy='" + createdBy + "'")
        .add("modifiedBy='" + modifiedBy + "'")
        .add("registrationDate=" + registrationDate)
        .add("created=" + created)
        .add("modified=" + modified)
        .toString();
  }
}
