package org.gbif.registry.cli.gdprnotification;

import org.gbif.registry.gdpr.GdprModule;
import org.gbif.registry.gdpr.email.GdprEmailModule;
import org.gbif.registry.persistence.guice.RegistryMyBatisModule;
import org.gbif.registry.surety.email.EmailManagerModule;
import org.gbif.ws.client.guice.GbifApplicationAuthModule;

import java.util.Properties;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Guice module for Gdpr notifications that adds all the necessary modules.
 */
public class GdprNotificationModule {

  private final GdprNotificationConfiguration config;

  public GdprNotificationModule(GdprNotificationConfiguration config) {
    this.config = config;
  }

  public Injector getInjector() {
    Properties mailProperties = config.gdprConfig.toProperties();

    return Guice.createInjector(new RegistryMyBatisModule(config.registry.toRegistryProperties()),
                                new GbifApplicationAuthModule(config.registry.user, config.registry.password),
                                new GdprModule(mailProperties),
                                new GdprEmailModule(mailProperties),
                                new EmailManagerModule(config.mailConfig.toMailProperties()));
  }
}
