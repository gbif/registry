package org.gbif.registry.surety;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Constants used by the surety modules
 */
public class SuretyConstants {

  /**
   * Utility class.
   */
  private SuretyConstants() {}

  public static final String PROPERTY_PREFIX = "surety.";
  public static final Marker NOTIFY_ADMIN = MarkerFactory.getMarker("NOTIFY_ADMIN");

}
