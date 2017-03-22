package org.gbif.registry.database;

import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.Properties;

import com.google.common.base.Throwables;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.io.Resources;

/**
 * Provides Guice utilities for easing testing.
 */
public class LiquibaseModules {

  private static String DEFAULT_REGISTRY_PREFIX = "registry.db.";
  private static String DEFAULT_REGISTRY_PROPERTIES_FILE = "registry-test.properties";

  private static Injector management;
  private static HikariDataSource managementDatasource;

  /**
   * @return A datasource that is for use in management activities such as Liquibase, or cleaning between tests.
   */
  public static HikariDataSource database() {
    if (managementDatasource == null) {
      managementDatasource = LiquibaseModules.management().getInstance(HikariDataSource.class);
    }
    return managementDatasource;
  }

  /**
   * Return a default injector.
   * @see {@link #management(String, String)}
   * @return
   */
  public static synchronized Injector management() {
    return management(DEFAULT_REGISTRY_PROPERTIES_FILE, DEFAULT_REGISTRY_PREFIX);
  }

  /**
   * @return An injector configured to issue a Datasource suitable for database management activities (Liquibase etc).
   */
  public static synchronized Injector management(String propertiesFile, String propertiesPrefix) {
    if (management == null) {
      try {
        final Properties p = new Properties();
        p.load(Resources.getResourceAsStream(propertiesFile));
        management = Guice.createInjector(new AbstractModule() {
          @Override
          protected void configure() {
          }

          @Provides
          @Singleton
          /**
           * Provides a hikari datasource that can issue connections for management activities, such as Liquibase or
           * clearing tables before tests run etc.
           * We provide an implementation specific datasource here so the pool can be properly closed at the end!
           */
          public HikariDataSource provideDs() {
            HikariConfig config = new HikariConfig(PropertiesUtil.filterProperties(p, propertiesPrefix));
            config.setConnectionTimeout(5000);
            config.setMaximumPoolSize(1);
            return new HikariDataSource(config);
          }
        });
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }
    return management;
  }
}
