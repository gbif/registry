package org.gbif.registry.surety;

import org.gbif.registry.surety.email.EmailSender;
import org.gbif.registry.surety.email.EmailManagerConfiguration;
import org.gbif.registry.surety.email.EmptyEmailSender;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/**
 * EmailSender Guice module for testing.
 * Binds the {@link EmailSender} to an empty implementation to avoid sending emails.
 */
public class EmailManagerTestModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(EmailSender.class).to(EmptyEmailSender.class).in(Scopes.SINGLETON);
    //bind configuration to have access to ResourceBundle
    bind(EmailManagerConfiguration.class).in(Scopes.SINGLETON);
  }

}
