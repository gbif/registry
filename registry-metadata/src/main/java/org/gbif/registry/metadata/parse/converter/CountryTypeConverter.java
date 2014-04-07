package org.gbif.registry.metadata.parse.converter;

import org.gbif.api.vocabulary.Country;
import org.gbif.common.parsers.CountryParser;


/**
 * {@link org.apache.commons.beanutils.Converter} implementation that handles conversion
 * to and from <b>Country</b> ENUM objects.
 */
public class CountryTypeConverter extends AbstractGbifParserConvert<Country> {

  /**
   * Construct a <b>CountryTypeConverter</b> <i>Converter</i>.
   */
  public CountryTypeConverter() {
    super(Country.class, CountryParser.getInstance());
  }

}
