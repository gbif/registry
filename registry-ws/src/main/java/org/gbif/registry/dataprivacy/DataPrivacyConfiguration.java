package org.gbif.registry.dataprivacy;

import java.util.Objects;
import java.util.Properties;

import org.apache.commons.lang3.BooleanUtils;

/**
 * Configuration for data privacy.
 */
public class DataPrivacyConfiguration {

  private static final String DATA_PRIVACY_ENABLED_PROP = "enabled";
  private static final String DATA_PRIVACY_VERSION_PROP = "version";

  private final String version;
  private final boolean dataPrivacyEnabled;

  public static DataPrivacyConfiguration from(Properties filteredProperties) {
    return new DataPrivacyConfiguration(filteredProperties);
  }

  private DataPrivacyConfiguration(Properties filteredProperties) {
    version = Objects.requireNonNull(filteredProperties.getProperty(DATA_PRIVACY_VERSION_PROP));
    dataPrivacyEnabled = BooleanUtils.toBoolean(filteredProperties.getProperty(DATA_PRIVACY_ENABLED_PROP));
  }

  public String getVersion() {
    return version;
  }

  public boolean isDataPrivacyEnabled() {
    return dataPrivacyEnabled;
  }
}
