package org.gbif.registry.dataprivacy;

import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.Properties;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;

/**
 * Guice module for data privacy.
 */
public class DataPrivacyModule extends PrivateModule {

  private static final String PROPERTY_PREFIX = "dataPrivacy.";

  private final Properties filteredProperties;

  public DataPrivacyModule(Properties properties) {
    filteredProperties = PropertiesUtil.filterProperties(properties, PROPERTY_PREFIX);
  }

  @Override
  protected void configure() {
    bind(DataPrivacyService.class).in(Scopes.SINGLETON);
    expose(DataPrivacyService.class);
    expose(DataPrivacyConfiguration.class);
  }

  @Provides
  @Singleton
  private DataPrivacyConfiguration provideDataPrivacyConfiguration() {
    return DataPrivacyConfiguration.from(filteredProperties);
  }

}
