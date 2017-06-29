package org.gbif.identity.guice;

import org.gbif.identity.mybatis.IdentitySuretyTestHelper;
import org.gbif.identity.mybatis.UserMapper;
import org.gbif.identity.service.IdentityServiceModule;
import org.gbif.registry.surety.persistence.ChallengeCodeMapper;
import org.gbif.registry.surety.persistence.ChallengeCodeSupportMapper;

import java.util.Properties;

import com.google.inject.Provides;
import com.google.inject.name.Named;

/**
 * Modifies the real {@link IdentityServiceModule} to expose a utility class {@link IdentitySuretyTestHelper}
 * that makes testing related to challengeCode easier.
 *
 * Requires:
 *  - ChallengeCodeMapper
 *  - @Named(CHALLENGE_CODE_SUPPORT_MAPPER_TYPE_NAME) ChallengeCodeSupportMapper<Integer>
 * Binds:
 *  - IdentitySuretyTestHelper
 *  - all of {@link IdentityServiceModule}
 */
public class IdentityServiceTestModule extends IdentityServiceModule {

  public IdentityServiceTestModule(Properties properties) {
    super(properties);
  }

  @Override
  protected void configure() {
    super.configure();
    expose(IdentitySuretyTestHelper.class);
    expose(UserMapper.class);
  }

  @Provides
  private IdentitySuretyTestHelper provide(ChallengeCodeMapper challengeCodeMapper,
                                           @Named(CHALLENGE_CODE_SUPPORT_MAPPER_TYPE_NAME) ChallengeCodeSupportMapper<Integer> challengeCodeSupportMapper ) {
    return new IdentitySuretyTestHelper(challengeCodeMapper, challengeCodeSupportMapper);
  }
}