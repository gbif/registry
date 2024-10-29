package org.gbif.registry.ws.converter;

import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Country;
import org.springframework.core.convert.converter.Converter;

public class CountryMessageConverter implements Converter<String, Country> {

  @Override
  public Country convert(String source) {
    return parseCountry(source);
  }

  private Country parseCountry(String param) {
    Country country = Country.fromIsoCode(param);
    if (country == null) {
      // if nothing found also try by enum name
      country = VocabularyUtils.lookupEnum(param, Country.class);
    }
    return country;
  }
}
