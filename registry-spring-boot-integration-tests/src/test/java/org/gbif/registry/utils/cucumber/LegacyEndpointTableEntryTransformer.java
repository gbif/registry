package org.gbif.registry.utils.cucumber;

import io.cucumber.datatable.TableEntryTransformer;
import org.gbif.api.vocabulary.EndpointType;
import org.gbif.registry.ws.model.LegacyEndpoint;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

public class LegacyEndpointTableEntryTransformer implements TableEntryTransformer<LegacyEndpoint> {

  @Override
  public LegacyEndpoint transform(Map<String, String> entry) {
    LegacyEndpoint endpoint = new LegacyEndpoint();
    Optional.ofNullable(entry.get("type"))
      .map(EndpointType::fromString)
      .ifPresent(endpoint::setType);
    Optional.ofNullable(entry.get("accessPointURL"))
      .map(URI::create)
      .ifPresent(endpoint::setUrl);
    endpoint.setDatasetKey(entry.get("resourceKey"));
    endpoint.setDescription(entry.get("description"));

    return endpoint;
  }
}
