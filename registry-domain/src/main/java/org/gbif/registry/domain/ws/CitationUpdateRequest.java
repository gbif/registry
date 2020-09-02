package org.gbif.registry.domain.ws;

import java.io.Serializable;
import java.net.URI;

public class CitationUpdateRequest implements Serializable {

  private URI target;

  public URI getTarget() {
    return target;
  }

  public void setTarget(URI target) {
    this.target = target;
  }
}
