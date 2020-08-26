package org.gbif.registry.domain.ws;

import java.net.URI;
import java.util.List;
import java.util.UUID;

public class CitationRequest {

  private String title;

  private URI url;

  // TODO: 26/08/2020 or DOIs
  private List<UUID> relatedDatasets;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public URI getUrl() {
    return url;
  }

  public void setUrl(URI url) {
    this.url = url;
  }

  public List<UUID> getRelatedDatasets() {
    return relatedDatasets;
  }

  public void setRelatedDatasets(List<UUID> relatedDatasets) {
    this.relatedDatasets = relatedDatasets;
  }
}
