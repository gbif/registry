package org.gbif.registry.utils.cucumber;

import io.cucumber.datatable.TableEntryTransformer;
import org.gbif.registry.domain.ws.LegacyOrganizationResponse;

import java.util.Map;

public class LegacyOrganizationResponseTableEntryTransformer implements TableEntryTransformer<LegacyOrganizationResponse> {

  @Override
  public LegacyOrganizationResponse transform(Map<String, String> entry) {
    LegacyOrganizationResponse result = new LegacyOrganizationResponse();

    result.setKey(entry.get("key"));
    result.setName(entry.get("name"));
    result.setNameLanguage(entry.get("nameLanguage"));
    result.setDescription(entry.get("description"));
    result.setDescriptionLanguage(entry.get("descriptionLanguage"));
    result.setHomepageURL(entry.get("homepageURL"));
    result.setPrimaryContactType(entry.get("primaryContactType"));
    result.setPrimaryContactName(entry.get("primaryContactName"));
    result.setPrimaryContactEmail(entry.get("primaryContactEmail"));
    result.setPrimaryContactAddress(entry.get("primaryContactAddress"));
    result.setPrimaryContactPhone(entry.get("primaryContactPhone"));
    result.setPrimaryContactDescription(entry.get("primaryContactDescription"));
    result.setNodeKey(entry.get("nodeKey"));
    result.setNodeName(entry.get("nodeName"));
    result.setNodeContactEmail(entry.get("nodeContactEmail"));

    return result;
  }
}
