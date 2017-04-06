package org.gbif.identity.mybatis;

import org.gbif.api.model.common.User;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.identity.model.Session;
import org.gbif.identity.service.IdentityServiceModule;
import org.gbif.mybatis.guice.MyBatisModule;
import org.gbif.mybatis.type.UuidTypeHandler;

import java.util.Properties;
import java.util.Set;
import java.util.UUID;

/**
 * This Module should not be used directly, use the {@link IdentityServiceModule} instead.
 */
public class InternalIdentityMyBatisModule extends MyBatisModule {

  public static final String DATASOURCE_BINDING_NAME = "identity";

  public InternalIdentityMyBatisModule(Properties props) {
    super(DATASOURCE_BINDING_NAME, props);
  }

  @Override
  protected void bindManagers() {
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
