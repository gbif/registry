package org.gbif.registry.persistence.mapper.handler;

import org.gbif.api.vocabulary.Country;
import org.gbif.mybatis.type.BaseEnumTypeHandler;
import org.gbif.mybatis.type.EnumConverter;

public class CountryNotNullTypeHandler extends BaseEnumTypeHandler<String, Country> {
  public CountryNotNullTypeHandler() {
    super(new CountryNotNullConverter());
  }

  public static class CountryNotNullConverter implements EnumConverter<String, Country> {
    public CountryNotNullConverter() {}

    @Override
    public String fromEnum(Country value) {
      return value != null ? value.getIso2LetterCode() : null;
    }

    @Override
    public Country toEnum(String key) {
      if (key == null) {
        return null;
      } else {
        Country c = Country.fromIsoCode(key);
        return c == null ? Country.UNKNOWN : c;
      }
    }
  }
}
