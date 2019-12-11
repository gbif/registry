package org.gbif.registry.doi;

public enum DoiType {

  /**
   * DOI of this type has no specific prefixes.
   */
  DATASET,

  /**
   * DOI of this type has a special prefix "dl.".
   */
  DOWNLOAD,

  /**
   * DOI of this type has a special prefix "dp.".
   */
  DATA_PACKAGE
}
