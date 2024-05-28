package org.gbif.registry.ws.converter;

import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Country;
import org.springframework.core.convert.converter.Converter;

public class StringToCountryConverter implements Converter<String, Country> {
  @Override
  public Country convert(String source) {
    Country result = Country.fromIsoCode(source);
    if (result == null) {
      result = (Country) VocabularyUtils.lookupEnum(source, Country.class);
    }

    return result;
  }
}
