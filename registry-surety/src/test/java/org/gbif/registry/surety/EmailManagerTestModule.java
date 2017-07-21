package org.gbif.registry.surety;

import org.gbif.registry.surety.email.EmailManager;
import org.gbif.registry.surety.email.EmailManagerConfiguration;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/**
 * EmailManager Guice module for testing.
 * Binds the {@link EmailManager} to an empty implementation to avoid sending emails.
 */
public class EmailManagerTestModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(EmailManager.class).to(EmptyEmailManager.class).in(Scopes.SINGLETON);
    //bind configuration to have access to ResourceBundle
    bind(EmailManagerConfiguration.class).in(Scopes.SINGLETON);
  }

}
