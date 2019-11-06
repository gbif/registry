package org.gbif.registry.utils;

import java.util.Collections;
import java.util.List;

public class Requests {

  public static final String DATASET_NAME = "Test Dataset Registry2 Sjælland";
  public static final String DOI = "http://dx.doi.org/10.1234/timbo";
  public static final String DATASET_DESCRIPTION = "Description of Test Dataset";
  public static final String DATASET_HOMEPAGE_URL = "http://www.homepage.com";
  public static final String DATASET_LOGO_URL = "http://www.logo.com/1";
  public static final String DATASET_PRIMARY_CONTACT_TYPE = "administrative";
  public static final String DATASET_PRIMARY_CONTACT_NAME = "Jan Legind";
  public static final List<String> DATASET_PRIMARY_CONTACT_EMAIL = Collections.singletonList("elyk-kaarb@euskadi.eus");
  public static final List<String> DATASET_PRIMARY_CONTACT_PHONE = Collections.singletonList("90909090");
  public static final List<String> DATASET_PRIMARY_CONTACT_ADDRESS = Collections.singletonList("Universitetsparken 15, 2100, Denmark");
  // GBRDS Datasets only
  public static final String DATASET_PRIMARY_CONTACT_DESCRIPTION = "Data manager";
  public static final String DATASET_NAME_LANGUAGE = "fr";
  public static final String DATASET_DESCRIPTION_LANGUAGE = "es";
}
