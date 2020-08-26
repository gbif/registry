package org.gbif.registry.domain.ws;

import org.gbif.api.model.common.DOI;

public class CitationResponse {

  private DOI assignedDOI;

  private String citation;

  public DOI getAssignedDOI() {
    return assignedDOI;
  }

  public void setAssignedDOI(DOI assignedDOI) {
    this.assignedDOI = assignedDOI;
  }

  public String getCitation() {
    return citation;
  }

  public void setCitation(String citation) {
    this.citation = citation;
  }
}
