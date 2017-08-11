package org.gbif.registry.surety.email;

import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.Properties;
import javax.mail.Session;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;

/**
 * Guice module for emails related to the Identity service.
 *
 * Requires:
 * - properties smtp.host, from, and bcc prefixed by mail.
 * Binds:
 *  - {@link EmailSender}
 */
public class EmailManagerModule extends AbstractModule {

  private static final String PROPERTY_PREFIX = "mail.";
  private static final String SMTP_SERVER = "smtp.host";
  private static final String EMAIL_FROM = "from";
  private static final String EMAIL_BCC = "bcc";

  private final Properties filteredProperties;

  public EmailManagerModule(Properties properties) {
    filteredProperties = PropertiesUtil.filterProperties(properties, PROPERTY_PREFIX);
  }

  @Override
  protected void configure() {
    bind(EmailSender.class).to(EmailSenderImpl.class).in(Scopes.SINGLETON);
  }

  @Provides
  @Singleton
  private EmailManagerConfiguration provideIdentityEmailManagerConfiguration() {
    EmailManagerConfiguration config = new EmailManagerConfiguration();

    Properties props = new Properties();
    props.setProperty("mail.smtp.host", filteredProperties.getProperty(SMTP_SERVER));
    props.setProperty("mail.from", filteredProperties.getProperty(EMAIL_FROM));
    config.setSession(Session.getInstance(props, null));
    config.setBccAddresses(filteredProperties.getProperty(EMAIL_BCC));
    return config;
  }

}
