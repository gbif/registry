package org.gbif.identity.guice;

import org.gbif.api.service.common.IdentityService;
import org.gbif.api.service.common.UserService;
import org.gbif.identity.mybatis.UserMapper;
import org.gbif.service.guice.PrivateServiceModule;

import java.util.Properties;

public class IdentityMyBatisModule extends PrivateServiceModule {

  static final String PREFIX = "registry.db.";      // TODO: use identity?
  private final boolean exposeMapper;

  /**
   * Uses the given properties to configure the service.
   *
   * @param properties to use
   */
  public IdentityMyBatisModule(Properties properties, boolean exposeMapper) {
    super(PREFIX, properties);
    this.exposeMapper = exposeMapper;
  }

  /**
   * Uses the given properties to configure the service.
   *
   * @param properties to use
   */
  public IdentityMyBatisModule(Properties properties) {
    this(properties, false);
  }

  @Override
  protected void configureService() {
    // bind classes
    InternalIdentityMyBatisModule mod = new InternalIdentityMyBatisModule(getProperties());
    install(mod);

    // expose named datasource binding
    //expose(mod.getDatasourceKey());

    if(exposeMapper){
      expose(UserMapper.class);
    }

    // expose services
    expose(IdentityService.class);
    expose(UserService.class);
  }

}
