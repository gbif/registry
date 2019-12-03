package org.gbif.registry.utils.cucumber;

import io.cucumber.datatable.TableEntryTransformer;
import org.gbif.registry.domain.ws.LegacyEndpointResponse;

import java.util.Map;

public class LegacyEndpointResponseTableEntryTransformer implements TableEntryTransformer<LegacyEndpointResponse> {

  @Override
  public LegacyEndpointResponse transform(Map<String, String> entry) {
    LegacyEndpointResponse endpoint = new LegacyEndpointResponse();
    endpoint.setKey(entry.get("key"));
    endpoint.setOrganisationKey(entry.get("organisationKey"));
    endpoint.setResourceKey(entry.get("resourceKey"));
    endpoint.setDescription(entry.get("description"));
    endpoint.setDescriptionLanguage(entry.get("descriptionLanguage"));
    endpoint.setType(entry.get("type"));
    endpoint.setTypeDescription(entry.get("typeDescription"));
    endpoint.setAccessPointURL(entry.get("accessPointURL"));

    return endpoint;
  }
}
