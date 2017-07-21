package org.gbif.identity.inject;

import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.identity.service.InternalIdentityAccessServiceModule;

import java.util.Properties;

import com.google.inject.AbstractModule;

/**
 * Main Guice module to access identity (login, get user).
 *
 * Requires:
 *  - Database properties prefixed by {@link org.gbif.identity.IdentityConstants#DB_PROPERTY_PREFIX}
 *
 * Exposes:
 *  - {@link IdentityAccessService}
 *
 */
public class IdentityAccessModule extends AbstractModule {

  private final Properties properties;

  public IdentityAccessModule(Properties properties) {
    this.properties = properties;
  }

  @Override
  protected void configure() {
    install(new InternalIdentityAccessServiceModule(properties));
  }
}