package org.gbif.registry.gdpr;

import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.Properties;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;

/**
 * Guice module for Gdpr.
 */
public class GdprModule extends PrivateModule {

  private static final String PROPERTY_PREFIX = "gdpr.";

  private final Properties filteredProperties;

  public GdprModule(Properties properties) {
    filteredProperties = PropertiesUtil.filterProperties(properties, PROPERTY_PREFIX);
  }

  @Override
  protected void configure() {
    bind(GdprService.class).in(Scopes.SINGLETON);
    expose(GdprService.class);
    expose(GdprConfiguration.class);
  }

  @Provides
  @Singleton
  private GdprConfiguration provideGdprConfiguration() {
    return GdprConfiguration.from(filteredProperties);
  }

}
