package org.gbif.registry.metadata.parse.converter;

import org.gbif.api.vocabulary.MaintenanceUpdateFrequency;
import org.gbif.common.parsers.MaintenanceUpdateFrequencyParser;

/**
 * {@link org.apache.commons.beanutils.Converter} implementation that handles conversion
 * to and from <b>MaintenanceUpdateFrequency</b> ENUM objects.
 */
public class MaintenanceUpdateFrequencyConverter extends AbstractGbifParserConvert<MaintenanceUpdateFrequency> {

  /**
   * Construct a <b>MaintenanceUpdateFrequencyConverter</b> <i>Converter</i>.
   */
  public MaintenanceUpdateFrequencyConverter() {
    super(MaintenanceUpdateFrequency.class, MaintenanceUpdateFrequencyParser.getInstance());
  }
}
