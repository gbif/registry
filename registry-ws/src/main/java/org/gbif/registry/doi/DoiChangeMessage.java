package org.gbif.registry.doi;

import org.gbif.api.model.common.DOI;
import org.gbif.common.messaging.api.Message;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.DoiStatus;

import java.net.URI;

/**
 * A message to request an update to a DOIs metadata and target URL in DataCite.
 * The DOI can be in any current state (registered, reserved, deleted) or even yet unknown to DataCite.
 *
 * TODO: move this message into postal service once stable!
 */
public class DoiChangeMessage implements Message {
  private static final String ROUTING_KEY = "doi.change";

  private final DOI doi;
  private final DoiStatus.Status status;
  private final DataCiteMetadata metadata;
  private final URI target;

  public DoiChangeMessage(DoiStatus.Status status, DOI doi, DataCiteMetadata metadata, URI target) {
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

  public DataCiteMetadata getMetadata() {
    return metadata;
  }

  public URI getTarget() {
    return target;
  }

  @Override
  public String getRoutingKey() {
    return ROUTING_KEY;
  }
}
