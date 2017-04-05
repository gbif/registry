package org.gbif.identity.guice;

import org.gbif.service.guice.PrivateServiceModule;

import java.util.Properties;

/**
 *
 */
public class IdentityEmailModule extends PrivateServiceModule {

  public IdentityEmailModule(String propertyPrefix, Properties properties) {
    super(propertyPrefix, properties);
  }

  @Override
  protected void configureService() {

  }
}
