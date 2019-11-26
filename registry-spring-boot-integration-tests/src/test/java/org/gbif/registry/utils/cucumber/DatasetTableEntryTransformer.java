package org.gbif.registry.utils.cucumber;

import io.cucumber.datatable.TableEntryTransformer;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.Language;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class DatasetTableEntryTransformer implements TableEntryTransformer<Dataset> {

  @Override
  public Dataset transform(Map<String, String> entry) {
    Dataset dataset = new Dataset();
    Optional.ofNullable(entry.get("key"))
      .map(UUID::fromString)
      .ifPresent(dataset::setKey);
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
    Optional.ofNullable(entry.get("external"))
      .map(Boolean::parseBoolean)
      .ifPresent(dataset::setExternal);
    dataset.setCreatedBy(entry.get("createdBy"));
    dataset.setModifiedBy(entry.get("modifiedBy"));
    Optional.ofNullable(entry.get("language"))
      .map(Language::valueOf)
      .ifPresent(dataset::setLanguage);

    return dataset;
  }
}
