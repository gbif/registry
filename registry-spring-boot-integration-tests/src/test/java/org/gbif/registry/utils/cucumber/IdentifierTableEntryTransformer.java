package org.gbif.registry.utils.cucumber;

import io.cucumber.datatable.TableEntryTransformer;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.vocabulary.IdentifierType;

import java.util.Map;
import java.util.Optional;

public class IdentifierTableEntryTransformer implements TableEntryTransformer<Identifier> {

  @Override
  public Identifier transform(Map<String, String> entry) {
    Identifier result = new Identifier();
    result.setIdentifier(entry.get("identifier"));
    Optional.ofNullable(entry.get("identifierType"))
      .map(IdentifierType::fromString)
      .ifPresent(result::setType);

    return result;
  }
}
