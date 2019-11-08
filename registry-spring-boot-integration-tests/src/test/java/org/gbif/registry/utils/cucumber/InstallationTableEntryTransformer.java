package org.gbif.registry.utils.cucumber;

import io.cucumber.datatable.TableEntryTransformer;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.vocabulary.InstallationType;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InstallationTableEntryTransformer implements TableEntryTransformer<Installation> {

  @Override
  public Installation transform(Map<String, String> entry) {
    Installation installation = new Installation();

    Optional.ofNullable(entry.get("organisationKey"))
      .map(UUID::fromString)
      .ifPresent(installation::setOrganizationKey);
    installation.setTitle(entry.get("title"));
    Optional.ofNullable(entry.get("type"))
      .map(InstallationType::fromString)
      .ifPresent(installation::setType);
    installation.setDescription(entry.get("description"));

    return installation;
  }
}
