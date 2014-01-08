package org.gbif.registry.metasync.protocols.tapir.model.metadata;

import org.gbif.api.vocabulary.Language;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

public class LocalizedString {

  private final Map<Language, String> values = Maps.newHashMap();

  public void addValue(Language language, String value) {
    values.put(language, value);
  }

  public Map<Language, String> getValues() {
    return values;
  }

  @Override
  public String toString() {
    if (values.isEmpty()) {
      return "";
    }

    if (values.containsKey(Language.ENGLISH)) {
      return values.get(Language.ENGLISH);
    }

    List<Language> languages = new ArrayList<Language>(values.keySet());
    return values.get(languages.get(0));
  }
}
