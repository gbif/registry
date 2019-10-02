package org.gbif.registry.doi;

import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.License;

public final class DataCiteConstants {

  private DataCiteConstants() {
  }

  public static final String ORCID_NAME_IDENTIFIER_SCHEME = "ORCID";
  public static final String CUSTOM_DOWNLOAD_TITLE = "GBIF Custom Occurrence Download";
  public static final String DOWNLOAD_FORMAT = "Compressed and UTF-8 encoded tab delimited file";
  public static final String DOWNLOAD_TITLE = "Occurrence Download";
  public static final String GBIF_PUBLISHER = "The Global Biodiversity Information Facility";
  public static final License DEFAULT_DOWNLOAD_LICENSE = License.CC_BY_NC_4_0;
  public static final String LICENSE_INFO = "Data from some individual datasets included in this download may be licensed under less restrictive terms.";
  public static final String ENGLISH = Language.ENGLISH.getIso3LetterCode();
  public static final String DWCA_FORMAT = "Darwin Core Archive";

}
