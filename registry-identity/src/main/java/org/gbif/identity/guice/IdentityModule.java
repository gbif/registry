package org.gbif.identity.guice;

import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.api.service.common.IdentityService;
import org.gbif.identity.service.InternalIdentityServiceModule;
import org.gbif.registry.surety.persistence.ChallengeCodeMapper;
import org.gbif.registry.surety.persistence.ChallengeCodeSupportMapper;

import java.util.Properties;

import com.google.inject.AbstractModule;

/**
 * Main Guice module of the identity module.
 *
 * Requires:
 *  - Database properties prefixed by {@link org.gbif.identity.IdentityConstants#DB_PROPERTY_PREFIX}
 *  - {@link ChallengeCodeMapper}, {@link ChallengeCodeSupportMapper<Integer>}
 *
 * Exposes:
 *  - {@link IdentityService}
 *  - {@link IdentityAccessService}
 *
 */
public class IdentityModule extends AbstractModule {

  private final Properties properties;

  public IdentityModule(Properties properties) {
    this.properties = properties;
  }

  @Override
  protected void configure() {
    install(new InternalIdentityServiceModule(properties));
  }
}