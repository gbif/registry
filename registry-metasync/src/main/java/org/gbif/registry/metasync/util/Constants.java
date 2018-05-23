package org.gbif.registry.metasync.util;

/**
 * Constants used by the Metadata synchroniser.
 */
public final class Constants {

  public static final String ABCD_12_SCHEMA = "http://www.tdwg.org/schemas/abcd/1.2";
  public static final String ABCD_206_SCHEMA = "http://www.tdwg.org/schemas/abcd/2.06";

  // "Names" used in Machine Tags, but not yet stored in TagName in gbif-api.
  public static final String INSTALLATION_VERSION = "version";
  // END: "Names" used in Machine Tags

  private Constants() {
    throw new UnsupportedOperationException("Can't initialize class");
  }

}
