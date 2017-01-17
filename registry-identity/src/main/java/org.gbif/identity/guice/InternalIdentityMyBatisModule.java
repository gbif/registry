package org.gbif.identity.guice;

import org.gbif.api.model.common.User;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.service.common.UserService;

import org.gbif.api.vocabulary.UserRole;
import org.gbif.identity.model.Session;
import org.gbif.identity.mybatis.IdentityServiceImpl;
import org.gbif.identity.mybatis.SessionMapper;
import org.gbif.identity.mybatis.UserMapper;
import org.gbif.identity.mybatis.UserRoleTypeHandler;
import org.gbif.identity.mybatis.UserServiceImpl;
import org.gbif.mybatis.guice.MyBatisModule;

import java.util.Properties;
import java.util.Set;

import com.google.inject.Scopes;

/**
 * This Module should not be used, use the {@link IdentityMyBatisModule} instead.
 */
class InternalIdentityMyBatisModule extends MyBatisModule {

  public static final String DATASOURCE_BINDING_NAME = "registry";

  public InternalIdentityMyBatisModule(Properties props) {
    super(DATASOURCE_BINDING_NAME, props);
  }

  @Override
  protected void bindManagers() {
    bind(UserService.class).to(UserServiceImpl.class).in(Scopes.SINGLETON);
    bind(IdentityService.class).to(IdentityServiceImpl.class).in(Scopes.SINGLETON);
  }

  @Override
  protected void bindMappers() {
    addMapperClass(UserMapper.class);
    addMapperClass(SessionMapper.class);
    addAlias("User").to(User.class);
    addAlias("UserRole").to(UserRole.class);
    addAlias("Session").to(Session.class);
  }

  @Override
  protected void bindTypeHandlers() {
    handleType(Set.class).with(UserRoleTypeHandler.class);
  }
}
