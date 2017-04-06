package org.gbif.identity.service;

import org.gbif.api.service.common.IdentityService;
import org.gbif.api.service.common.UserService;
import org.gbif.identity.mybatis.InternalIdentityMyBatisModule;
import org.gbif.identity.mybatis.UserServiceImpl;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.Properties;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;

/**
 * Identity Service Module using mybatis as source for data.
 * This module is private to avoid exposing the mybatis layer.
 *
 * Requires: properties identity.db.*
 * Binds: {@link IdentityService}
 */
public class IdentityServiceModule extends PrivateModule {

  public static final String PROPERTY_PREFIX = "identity.db.";

  private final Properties filteredProperties;

  public IdentityServiceModule(Properties properties) {
    filteredProperties = PropertiesUtil.filterProperties(properties, PROPERTY_PREFIX);
  }

  @Override
  protected void configure() {
    // bind classes
    install(new InternalIdentityMyBatisModule(filteredProperties));
    bind(IdentityService.class).to(IdentityServiceImpl.class).in(Scopes.SINGLETON);
    bind(UserService.class).to(UserServiceImpl.class).in(Scopes.SINGLETON);

    expose(IdentityService.class);
    expose(UserService.class);
  }
}
