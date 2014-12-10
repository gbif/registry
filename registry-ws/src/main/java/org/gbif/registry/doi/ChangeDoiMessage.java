package org.gbif.registry.doi;

import org.gbif.api.model.common.DOI;
import org.gbif.common.messaging.api.Message;
import org.gbif.doi.service.DoiStatus;

import java.net.URI;
import java.util.Objects;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * A message to request an update to a DOIs metadata and target URL in DataCite.
 * The DOI can be in any current state (registered, reserved, deleted) or even yet unknown to DataCite.
 *
 * TODO: move this message into postal service once stable!
 */
public class ChangeDoiMessage implements Message {
  private static final String ROUTING_KEY = "doi.change";

  private final DOI doi;
  private final DoiStatus.Status status;
  private final String metadata;
  private final URI target;

  @JsonCreator
  public ChangeDoiMessage(@JsonProperty("status") DoiStatus.Status status, @JsonProperty("doi") DOI doi,
    @JsonProperty("metadata") String metadata, @JsonProperty("target") URI target) {
    this.status = status;
    this.doi = doi;
    this.metadata = metadata;
    this.target = target;
  }

  public DOI getDoi() {
    return doi;
  }

  /**
   * @return the desired status this doi should be updated to
   */
  public DoiStatus.Status getStatus() {
    return status;
  }

  /**
   * @return the metadata as datacite xml
   */
  public String getMetadata() {
    return metadata;
  }

  public URI getTarget() {
    return target;
  }

  @Override
  public String getRoutingKey() {
    return ROUTING_KEY;
  }

  @Override
  public int hashCode() {
    return Objects.hash(doi, status, metadata, target);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ChangeDoiMessage other = (ChangeDoiMessage) obj;
    return Objects.equals(this.doi, other.doi) && Objects.equals(this.status, other.status) && Objects
      .equals(this.metadata, other.metadata) && Objects.equals(this.target, other.target);
  }
}
