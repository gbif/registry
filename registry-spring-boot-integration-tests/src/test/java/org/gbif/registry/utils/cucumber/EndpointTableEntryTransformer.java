package org.gbif.registry.utils.cucumber;

import io.cucumber.datatable.TableEntryTransformer;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.vocabulary.EndpointType;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

public class EndpointTableEntryTransformer implements TableEntryTransformer<Endpoint> {

  @Override
  public Endpoint transform(Map<String, String> entry) {
    Endpoint endpoint = new Endpoint();
    Optional.ofNullable(entry.get("type"))
      .map(EndpointType::fromString)
      .ifPresent(endpoint::setType);
    Optional.ofNullable(entry.get("url"))
      .map(URI::create)
      .ifPresent(endpoint::setUrl);

    return endpoint;
  }
}
