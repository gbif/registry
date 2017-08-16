package org.gbif.registry.ws.client.guice;

import org.gbif.api.service.common.IdentityAccessService;

import java.util.Properties;

/**
 * Same as {@link RegistryWsClientModule} but exposes {@link IdentityAccessService}.
 */
public class RegistryWsClientModuleIdentity extends RegistryWsClientModule {

  public RegistryWsClientModuleIdentity(Properties properties) {
    super(properties);
  }

  @Override
  protected void configureClient() {
    super.configureClient();
    expose(IdentityAccessService.class);
  }
}
