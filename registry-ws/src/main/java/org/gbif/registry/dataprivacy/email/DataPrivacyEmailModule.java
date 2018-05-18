package org.gbif.registry.dataprivacy.email;

import org.gbif.registry.surety.email.EmailTemplateProcessor;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.Properties;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;

/**
 * Guice module for the dataprivacy emails.
 */
public class DataPrivacyEmailModule extends PrivateModule {

  private static final String PROPERTY_PREFIX = "dataPrivacy.";
  private static final String TEMPLATE = "dataPrivacy_notification_en.ftl";

  private final DataPrivacyEmailConfiguration config;

  public DataPrivacyEmailModule(Properties properties) {
    config = DataPrivacyEmailConfiguration.from(PropertiesUtil.filterProperties(properties, PROPERTY_PREFIX));
  }

  @Override
  protected void configure() {
    bind(DataPrivacyEmailManager.class).in(Scopes.SINGLETON);
    expose(DataPrivacyEmailManager.class);
    expose(DataPrivacyEmailConfiguration.class);
  }

  @Provides
  @Singleton
  private DataPrivacyEmailConfiguration provideDataPrivacyEmailConfiguration() {
    return config;
  }

  @Provides
  @Singleton
  private EmailTemplateProcessor provideTemplateProcessor() {
    return new EmailTemplateProcessor(locale -> config.getSubject(), locale -> TEMPLATE);
  }

}
