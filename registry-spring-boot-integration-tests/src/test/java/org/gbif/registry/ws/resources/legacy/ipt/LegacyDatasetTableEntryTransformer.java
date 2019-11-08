package org.gbif.registry.ws.resources.legacy.ipt;

import io.cucumber.datatable.TableEntryTransformer;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.registry.ws.model.LegacyDataset;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class LegacyDatasetTableEntryTransformer implements TableEntryTransformer<LegacyDataset> {

  @Override
  public LegacyDataset transform(Map<String, String> entry) {
    LegacyDataset dataset = new LegacyDataset();
    Optional.ofNullable(entry.get("organisationKey"))
      .map(UUID::fromString)
      .ifPresent(dataset::setPublishingOrganizationKey);
    dataset.setDescription(entry.get("description"));
    dataset.setTitle(entry.get("name"));
    Optional.ofNullable(entry.get("homepageUrl"))
      .map(URI::create)
      .ifPresent(dataset::setHomepage);
    Optional.ofNullable(entry.get("logoUrl"))
      .map(URI::create)
      .ifPresent(dataset::setLogoUrl);
    Optional.ofNullable(entry.get("iptKey"))
      .map(UUID::fromString)
      .ifPresent(dataset::setInstallationKey);
    Optional.ofNullable(entry.get("type"))
      .map(DatasetType::fromString)
      .ifPresent(dataset::setType);
    dataset.setPrimaryContactName(entry.get("primaryContactName"));

    return dataset;
  }
}
