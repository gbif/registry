package org.gbif.registry.utils.cucumber;

import io.cucumber.datatable.TableEntryTransformer;
import org.gbif.registry.ws.model.LegacyOrganizationBriefResponse;

import java.util.Map;

public class LegacyOrganizationBriefResponseTableEntryTransformer implements TableEntryTransformer<LegacyOrganizationBriefResponse> {

  @Override
  public LegacyOrganizationBriefResponse transform(Map<String, String> entry) {
    LegacyOrganizationBriefResponse result = new LegacyOrganizationBriefResponse();

    result.setKey(entry.get("key"));
    result.setName(entry.get("name"));

    return result;
  }
}
