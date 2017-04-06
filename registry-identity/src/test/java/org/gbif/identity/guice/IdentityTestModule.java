package org.gbif.identity.guice;

import org.gbif.identity.email.IdentityEmailManager;
import org.gbif.identity.email.InMemoryIdentityEmailManager;
import org.gbif.identity.service.IdentityServiceModule;

import java.util.Properties;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/**
 * Wires Identity modules for testing purpose.
 * The main target is to bind {@link IdentityEmailManager} to {@link InMemoryIdentityEmailManager}
 */
public class IdentityTestModule extends AbstractModule {

  private final Properties properties;

  public IdentityTestModule(Properties properties) {
    this.properties = properties;
  }

  @Override
  protected void configure() {
    bind(IdentityEmailManager.class)
            .to(InMemoryIdentityEmailManager.class)
            .in(Scopes.SINGLETON);
    install(new IdentityServiceModule(properties));
  }
}
