package org.gbif.identity.mybatis;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.identity.service.InternalIdentityServiceModule;
import org.gbif.mybatis.guice.MyBatisModule;
import org.gbif.mybatis.type.UuidTypeHandler;

import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import com.google.inject.Scopes;
import com.google.inject.name.Names;

import static org.gbif.identity.service.InternalIdentityServiceModule.CHALLENGE_CODE_SUPPORT_MAPPER_TYPE_LITERAL;
import static org.gbif.identity.service.InternalIdentityServiceModule.CHALLENGE_CODE_SUPPORT_MAPPER_TYPE_NAME;

/**
 * This Module should not be used directly, use the {@link InternalIdentityServiceModule} instead.
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

    addAlias("GbifUser").to(GbifUser.class);
    addAlias("UserRole").to(UserRole.class);
    addAlias("UUID").to(UUID.class);

    // expose UserMapper also as ChallengeCodeSupportMapper<Integer>
    bind(CHALLENGE_CODE_SUPPORT_MAPPER_TYPE_LITERAL)
            .annotatedWith(Names.named(CHALLENGE_CODE_SUPPORT_MAPPER_TYPE_NAME))
            .to(UserMapper.class).in(Scopes.SINGLETON);
  }

  @Override
  protected void bindTypeHandlers() {
    handleType(Set.class).with(UserRoleTypeHandler.class);
    handleType(UUID.class).with(UuidTypeHandler.class);
  }
}
