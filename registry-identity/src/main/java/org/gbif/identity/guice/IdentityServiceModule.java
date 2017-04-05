package org.gbif.identity.guice;

import org.gbif.api.service.common.IdentityService;
import org.gbif.api.service.common.UserService;
import org.gbif.identity.email.IdentityEmailManager;
import org.gbif.identity.mybatis.IdentityServiceImpl;
import org.gbif.identity.mybatis.UserServiceImpl;
import org.gbif.service.guice.PrivateServiceModule;

import java.util.Properties;

import com.google.inject.Scopes;

/**
 * Guice bindings for Identity service.
 * Requires {@link IdentityEmailManager}
 */
public class IdentityServiceModule extends PrivateServiceModule {

  static final String PREFIX = "registry.db.";      // TODO: use identity?

  /**
   * Uses the given properties to configure the service.
   *
   * @param properties to use
   */
  public IdentityServiceModule(Properties properties) {
    super(PREFIX, properties);
  }

  @Override
  protected void configureService() {
    // bind classes
    InternalIdentityMyBatisModule mod = new InternalIdentityMyBatisModule(getProperties());
    install(mod);

    bind(IdentityService.class).to(IdentityServiceImpl.class).in(Scopes.SINGLETON);
    bind(UserService.class).to(UserServiceImpl.class).in(Scopes.SINGLETON);

    // expose named datasource binding
    //expose(mod.getDatasourceKey());

    // expose services
    expose(IdentityService.class);
    expose(UserService.class);
  }

}
