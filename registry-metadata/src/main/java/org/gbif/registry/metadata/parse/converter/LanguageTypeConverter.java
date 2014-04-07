package org.gbif.registry.metadata.parse.converter;

import org.gbif.api.vocabulary.Language;
import org.gbif.common.parsers.LanguageParser;


/**
 * {@link org.apache.commons.beanutils.Converter} implementation that handles conversion
 * to and from <b>Language</b> ENUM objects.
 */
public class LanguageTypeConverter extends AbstractGbifParserConvert<Language> {

  /**
   * Construct a <b>LanguageTypeConverter</b> <i>Converter</i>.
   */
  public LanguageTypeConverter() {
    super(Language.class, LanguageParser.getInstance());
  }
}
