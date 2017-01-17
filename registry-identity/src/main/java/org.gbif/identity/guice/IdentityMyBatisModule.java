package org.gbif.identity.guice;

import org.gbif.api.service.common.IdentityService;
import org.gbif.api.service.common.UserService;
import org.gbif.service.guice.PrivateServiceModule;

import java.util.Properties;

public class IdentityMyBatisModule extends PrivateServiceModule {

  private static final String PREFIX = "registry.db.";

  /**
   * Uses the given properties to configure the service.
   *
   * @param properties to use
   */
  public IdentityMyBatisModule(Properties properties) {
    super(PREFIX, properties);
  }

  @Override
  protected void configureService() {
    // bind classes
    InternalIdentityMyBatisModule mod = new InternalIdentityMyBatisModule(getProperties());
    install(mod);

    // expose named datasource binding
    expose(mod.getDatasourceKey());

    // expose services
    expose(IdentityService.class);
  }
}
