package org.gbif.identity.inject;

import org.gbif.identity.service.InternalIdentityServiceModule;
import org.gbif.mybatis.guice.MyBatisModule;
import org.gbif.mybatis.type.UuidTypeHandler;
import org.gbif.registry.surety.InMemoryEmailManager;
import org.gbif.registry.surety.InMemoryEmailTestModule;
import org.gbif.registry.surety.email.EmailManager;
import org.gbif.registry.surety.persistence.ChallengeCodeMapper;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.Properties;
import java.util.UUID;

import com.google.inject.AbstractModule;
import com.google.inject.PrivateModule;
import com.google.inject.Scopes;

import static org.gbif.identity.IdentityConstants.DB_PROPERTY_PREFIX;

/**
 * Wires Identity related classes for testing purpose (for the identity module).
 *
 * The main goal is to bind {@link EmailManager} to {@link InMemoryEmailTestModule} and
 * expose {@link ChallengeCodeMapper} and {@link InternalIdentityServiceModule#CHALLENGE_CODE_SUPPORT_MAPPER_TYPE_NAME}.
 */
public class IdentityTestModule extends AbstractModule {

  private final Properties properties;

  public IdentityTestModule(Properties properties) {
    this.properties = properties;
  }

  @Override
  protected void configure() {
    install(new InMemoryEmailTestModule());
    //expose it directly
    bind(InMemoryEmailManager.class).in(Scopes.SINGLETON);

    install(new IdentityServiceTestModule(properties));
    install(new InternalTestPrivateModule(properties));
  }

  /**
   * We use a private module to avoid exposing the inner datasource.
   * Our goal is simply to expose a {@link ChallengeCodeMapper}.
   */
  private static class InternalTestPrivateModule extends PrivateModule {

    private final Properties properties;

    public InternalTestPrivateModule(Properties properties) {
      this.properties = properties;
    }

    @Override
    protected void configure() {
      install(new TestOnlyChallengeCodeMyBatisModule(properties));
      expose(ChallengeCodeMapper.class);
    }
  }

  /**
   * In order to run tests we need access to ChallengeCode directly so we create a MyBatis modukle
   * with only the {@link ChallengeCodeMapper}.
   */
  private static class TestOnlyChallengeCodeMyBatisModule extends MyBatisModule {
    private static final String DATASOURCE_BINDING_NAME = "test-identity";

    public TestOnlyChallengeCodeMyBatisModule(Properties props) {
      super(DATASOURCE_BINDING_NAME, PropertiesUtil.filterProperties(props, DB_PROPERTY_PREFIX));
    }

    @Override
    protected void bindManagers() {
      failFast(true);
    }

    @Override
    protected void bindMappers() {
      addMapperClass(ChallengeCodeMapper.class);
      addAlias("UUID").to(UUID.class);
    }

    @Override
    protected void bindTypeHandlers() {
      handleType(UUID.class).with(UuidTypeHandler.class);
    }

  }

}
