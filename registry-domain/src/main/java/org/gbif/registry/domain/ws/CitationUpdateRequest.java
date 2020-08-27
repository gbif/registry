package org.gbif.registry.domain.ws;

import java.net.URI;

public class CitationUpdateRequest {

  private URI target;

  public URI getTarget() {
    return target;
  }

  public void setTarget(URI target) {
    this.target = target;
  }
}
