package org.gbif.registry.utils.cucumber;

import io.cucumber.datatable.TableEntryTransformer;
import org.gbif.api.model.registry.Tag;

import java.util.Map;

public class TagTableEntryTransformer implements TableEntryTransformer<Tag> {

  @Override
  public Tag transform(Map<String, String> entry) {
    Tag result = new Tag();
    result.setValue(entry.get("value"));

    return result;
  }
}
