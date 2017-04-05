package org.gbif.identity.guice;

import org.gbif.api.model.common.User;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.identity.model.Session;
import org.gbif.identity.mybatis.SessionMapper;
import org.gbif.identity.mybatis.UserMapper;
import org.gbif.identity.mybatis.UserRoleTypeHandler;
import org.gbif.mybatis.guice.MyBatisModule;
import org.gbif.mybatis.type.UuidTypeHandler;

import java.util.Properties;
import java.util.Set;
import java.util.UUID;

/**
 * This Module should not be used, use the {@link IdentityServiceModule} instead.
 */
class InternalIdentityMyBatisModule extends MyBatisModule {

  public static final String DATASOURCE_BINDING_NAME = "identity";

  public InternalIdentityMyBatisModule(Properties props) {
    super(DATASOURCE_BINDING_NAME, props);
  }

  @Override
  protected void bindManagers() {
//    bind(UserService.class).to(UserServiceImpl.class).in(Scopes.SINGLETON);
//    bind(IdentityService.class).to(IdentityServiceImpl.class).in(Scopes.SINGLETON);
    failFast(true);
  }

  @Override
  protected void bindMappers() {
    addMapperClass(UserMapper.class);
    addMapperClass(SessionMapper.class);

    addAlias("User").to(User.class);
    addAlias("UserRole").to(UserRole.class);
    addAlias("Session").to(Session.class);
    addAlias("UUID").to(UUID.class);
  }

  @Override
  protected void bindTypeHandlers() {
    handleType(Set.class).with(UserRoleTypeHandler.class);
    handleType(UUID.class).with(UuidTypeHandler.class);
  }
}
