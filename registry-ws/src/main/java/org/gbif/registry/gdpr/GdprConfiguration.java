package org.gbif.registry.gdpr;

import java.util.Objects;
import java.util.Properties;

import org.apache.commons.lang3.BooleanUtils;

/**
 * Configuration for Gdpr.
 */
public class GdprConfiguration {

  private static final String GDPR_ENABLED_PROP = "enabled";
  private static final String GDPR_VERSION_PROP = "version";

  private final String version;
  private final boolean gdprEnabled;

  public static GdprConfiguration from(Properties filteredProperties) {
    return new GdprConfiguration(filteredProperties);
  }

  private GdprConfiguration(Properties filteredProperties) {
    version = Objects.requireNonNull(filteredProperties.getProperty(GDPR_VERSION_PROP));
    gdprEnabled = BooleanUtils.toBoolean(filteredProperties.getProperty(GDPR_ENABLED_PROP));
  }

  public String getVersion() {
    return version;
  }

  public boolean isGdprEnabled() {
    return gdprEnabled;
  }
}
