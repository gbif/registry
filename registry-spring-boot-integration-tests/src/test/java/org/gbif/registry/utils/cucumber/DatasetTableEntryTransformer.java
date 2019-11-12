package org.gbif.registry.utils.cucumber;

import io.cucumber.datatable.TableEntryTransformer;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.DatasetType;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class DatasetTableEntryTransformer implements TableEntryTransformer<Dataset> {

  @Override
  public Dataset transform(Map<String, String> entry) {
    Dataset dataset = new Dataset();
    Optional.ofNullable(entry.get("publishingOrganizationKey"))
      .map(UUID::fromString)
      .ifPresent(dataset::setPublishingOrganizationKey);
    dataset.setDescription(entry.get("description"));
    dataset.setTitle(entry.get("title"));
    Optional.ofNullable(entry.get("installationKey"))
      .map(UUID::fromString)
      .ifPresent(dataset::setInstallationKey);
    Optional.ofNullable(entry.get("type"))
      .map(DatasetType::fromString)
      .ifPresent(dataset::setType);
    Optional.ofNullable(entry.get("logoUrl"))
      .map(URI::create)
      .ifPresent(dataset::setLogoUrl);
    Optional.ofNullable(entry.get("homepage"))
      .map(URI::create)
      .ifPresent(dataset::setHomepage);

    return dataset;
  }
}
