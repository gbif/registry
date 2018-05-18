package org.gbif.registry.gdpr.email;

import org.gbif.registry.surety.email.EmailTemplateProcessor;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.Properties;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;

/**
 * Guice model for the gdpr emails.
 */
public class GdprEmailModule extends PrivateModule {

  private static final String PROPERTY_PREFIX = "gdpr.";
  private static final String TEMPLATE = "gdpr_notification_en.ftl";

  private final GdprEmailConfiguration config;

  public GdprEmailModule(Properties properties) {
    config = GdprEmailConfiguration.from(PropertiesUtil.filterProperties(properties, PROPERTY_PREFIX));
  }

  @Override
  protected void configure() {
    bind(GdprEmailManager.class).in(Scopes.SINGLETON);
    expose(GdprEmailManager.class);
    expose(GdprEmailConfiguration.class);
  }

  @Provides
  @Singleton
  private GdprEmailConfiguration provideGdprEmailConfiguration() {
    return config;
  }

  @Provides
  @Singleton
  private EmailTemplateProcessor provideTemplateProcessor() {
    return new EmailTemplateProcessor(locale -> config.getSubject(), locale -> TEMPLATE);
  }

}
