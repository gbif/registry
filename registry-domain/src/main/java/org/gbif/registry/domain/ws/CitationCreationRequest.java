package org.gbif.registry.domain.ws;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.PrePersist;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class CitationCreationRequest implements Serializable {

  private DOI originalDownloadDOI;
  private String title;
  private String creator;
  private URI target;
  private Date registrationDate;
  private List<String> relatedDatasets;

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

  @NotNull
  public List<String> getRelatedDatasets() {
    return Collections.unmodifiableList(relatedDatasets);
  }

  public void setRelatedDatasets(List<String> relatedDatasets) {
    this.relatedDatasets = relatedDatasets;
  }
}
