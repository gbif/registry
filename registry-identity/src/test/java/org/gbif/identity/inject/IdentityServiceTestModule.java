package org.gbif.identity.inject;

import org.gbif.identity.mybatis.IdentitySuretyTestHelper;
import org.gbif.identity.mybatis.UserMapper;
import org.gbif.identity.service.InternalIdentityServiceModule;
import org.gbif.registry.surety.persistence.ChallengeCodeMapper;
import org.gbif.registry.surety.persistence.ChallengeCodeSupportMapper;

import java.util.Properties;

import com.google.inject.Provides;
import com.google.inject.name.Named;

/**
 * Modifies the real {@link InternalIdentityServiceModule} to expose a utility class {@link IdentitySuretyTestHelper}
 * that makes testing related to challengeCode easier.
 *
 * Requires:
 *  - ChallengeCodeMapper
 *  - @Named(CHALLENGE_CODE_SUPPORT_MAPPER_TYPE_NAME) ChallengeCodeSupportMapper<Integer>
 * Binds:
 *  - IdentitySuretyTestHelper
 *  - all of {@link InternalIdentityServiceModule}
 */
public class IdentityServiceTestModule extends InternalIdentityServiceModule {

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