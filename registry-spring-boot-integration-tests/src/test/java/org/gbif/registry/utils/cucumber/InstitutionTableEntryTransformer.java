package org.gbif.registry.utils.cucumber;

import io.cucumber.datatable.TableEntryTransformer;
import org.gbif.api.model.collections.Institution;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

public class InstitutionTableEntryTransformer
  implements TableEntryTransformer<Institution> {

  @Override
  public Institution transform(Map<String, String> entry) {
    Institution result = new Institution();

    result.setCode(entry.get("code"));
    result.setName(entry.get("name"));
    result.setDescription(entry.get("description"));
    Optional.ofNullable(entry.get("homepage"))
      .map(URI::create)
      .ifPresent(result::setHomepage);

    return result;
  }
}
