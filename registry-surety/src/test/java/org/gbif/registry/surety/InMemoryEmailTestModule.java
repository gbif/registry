package org.gbif.registry.surety;

import org.gbif.registry.surety.email.EmailSender;
import org.gbif.registry.surety.email.EmailManagerConfiguration;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/**
 *
 */
public class InMemoryEmailTestModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(EmailSender.class).to(InMemoryEmailManager.class).in(Scopes.SINGLETON);
    //bind configuration to have access to ResourceBundle
    bind(EmailManagerConfiguration.class).in(Scopes.SINGLETON);
  }
}