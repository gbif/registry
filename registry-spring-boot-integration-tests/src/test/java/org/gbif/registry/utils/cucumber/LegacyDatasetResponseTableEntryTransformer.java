package org.gbif.registry.utils.cucumber;

import io.cucumber.datatable.TableEntryTransformer;
import org.gbif.registry.ws.model.LegacyDatasetResponse;

import java.util.Map;

public class LegacyDatasetResponseTableEntryTransformer implements TableEntryTransformer<LegacyDatasetResponse> {

  @Override
  public LegacyDatasetResponse transform(Map<String, String> entry) {
    LegacyDatasetResponse result = new LegacyDatasetResponse();

    result.setKey(entry.get("key"));
    result.setDescription(entry.get("description"));
    result.setDescriptionLanguage(entry.get("descriptionLanguage"));
    result.setHomepageURL(entry.get("homepageURL"));
    result.setName(entry.get("name"));
    result.setNameLanguage(entry.get("nameLanguage"));
    result.setOrganisationKey(entry.get("organisationKey"));

    return result;
  }
}
