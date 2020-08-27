package org.gbif.registry.domain.ws;

import org.gbif.api.model.common.DOI;

import java.net.URI;
import java.util.Collections;
import java.util.List;

public class CitationCreationRequest {

  private String title;

  private URI target;

  private List<DOI> relatedDatasets;

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

  public List<DOI> getRelatedDatasets() {
    return Collections.unmodifiableList(relatedDatasets);
  }

  public void setRelatedDatasets(List<DOI> relatedDatasets) {
    this.relatedDatasets = relatedDatasets;
  }
}
