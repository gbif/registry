package org.gbif.identity.email;

import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.Properties;
import javax.mail.Session;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;

/**
 * Guice module for emails related to the Identity service.
 * Requires: properties smtp.host, from, and bcc prefixed by identity.mail
 * Binds: {@link IdentityEmailManager}
 */
public class IdentityEmailModule extends AbstractModule {

  private static final String propertyPrefix = "identity.mail.";
  private static final String SMTP_SERVER = "smtp.host";
  private static final String EMAIL_FROM = "from";
  private static final String EMAIL_BCC = "bcc";

  private static final String CONFIRM_TEMPLATE = "urlTemplate.confirm";
  private static final String RESET_PASSWORD_TEMPLATE = "urlTemplate.resetPassword";

  private Properties filteredProperties;

  public IdentityEmailModule(Properties properties) {
    filteredProperties = PropertiesUtil.filterProperties(properties, propertyPrefix);
  }

  @Override
  protected void configure() {
    bind(IdentityEmailManager.class).to(IdentityEmailManagerImpl.class).in(Scopes.SINGLETON);
  }

  @Provides
  @Singleton
  private IdentityEmailManagerConfiguration provideIdentityEmailManagerConfiguration() {
    IdentityEmailManagerConfiguration config = new IdentityEmailManagerConfiguration();

    Properties props = new Properties();
    props.setProperty("mail.smtp.host", filteredProperties.getProperty(SMTP_SERVER));
    props.setProperty("mail.from", filteredProperties.getProperty(EMAIL_FROM));
    config.setSession(Session.getInstance(props, null));

    config.setBccAddresses(filteredProperties.getProperty(EMAIL_BCC));
    config.setConfirmUrlTemplate(filteredProperties.getProperty(CONFIRM_TEMPLATE));
    config.setResetPasswordUrlTemplate(filteredProperties.getProperty(RESET_PASSWORD_TEMPLATE));

    return config;
  }

}
