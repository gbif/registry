package org.gbif.registry.cli.dataprivacynotification;

import org.gbif.registry.dataprivacy.DataPrivacyModule;
import org.gbif.registry.dataprivacy.email.DataPrivacyEmailModule;
import org.gbif.registry.persistence.guice.RegistryMyBatisModule;
import org.gbif.registry.surety.email.EmailManagerModule;
import org.gbif.ws.client.guice.GbifApplicationAuthModule;

import java.util.Properties;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Guice module for data privacy notifications that adds all the necessary modules.
 */
public class DataPrivacyNotificationModule {

  private final DataPrivacyNotificationConfiguration config;

  public DataPrivacyNotificationModule(DataPrivacyNotificationConfiguration config) {
    this.config = config;
  }

  public Injector getInjector() {
    Properties mailProperties = config.dataPrivacyConfig.toProperties();

    return Guice.createInjector(new RegistryMyBatisModule(config.registry.toRegistryProperties()),
                                new GbifApplicationAuthModule(config.registry.user, config.registry.password),
                                new DataPrivacyModule(mailProperties),
                                new DataPrivacyEmailModule(mailProperties),
                                new EmailManagerModule(config.mailConfig.toMailProperties()));
  }
}
