package org.gbif.registry.ws.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "legacyEndpointResponses")
public class LegacyEndpointListWrapper {

  private List<LegacyEndpointResponse> legacyEndpointResponses;

  public LegacyEndpointListWrapper() {
  }

  public LegacyEndpointListWrapper(List<LegacyEndpointResponse> legacyEndpointResponses) {
    this.legacyEndpointResponses = legacyEndpointResponses;
  }

  @XmlElement(name = "service")
  public List<LegacyEndpointResponse> getLegacyEndpointResponses() {
    return legacyEndpointResponses;
  }

  public void setLegacyEndpointResponses(List<LegacyEndpointResponse> legacyEndpointResponses) {
    this.legacyEndpointResponses = legacyEndpointResponses;
  }
}
