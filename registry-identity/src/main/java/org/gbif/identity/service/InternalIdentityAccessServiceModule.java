package org.gbif.identity.service;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.api.service.common.IdentityService;
import org.gbif.identity.inject.IdentityAccessModule;
import org.gbif.identity.mybatis.InternalIdentityMyBatisModule;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.Properties;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.inject.Inject;
import com.google.inject.PrivateModule;

import static org.gbif.identity.IdentityConstants.DB_PROPERTY_PREFIX;

/**
 * Guice module that only exposes {@link IdentityAccessService} for accessing users.
 * Internal module, this module should not be used directly. {@link IdentityAccessModule} should be used.
 */
public class InternalIdentityAccessServiceModule extends PrivateModule {

  private final Properties rawProperties;

  public InternalIdentityAccessServiceModule(Properties properties) {
    rawProperties = properties;
  }

  @Override
  protected void configure() {

    install(new InternalIdentityMyBatisModule(PropertiesUtil.filterProperties(rawProperties, DB_PROPERTY_PREFIX)));
    bind(UserSuretyDelegate.class).to(EmptyUserSuretyDelegate.class);
    bind(IdentityService.class).to(IdentityServiceImpl.class);
    bind(IdentityAccessService.class).to(InnerIdentityService.class);

    expose(IdentityAccessService.class);
  }

  /**
   * Inner implementation of {@link UserSuretyDelegate} that ignores all calls and returns
   * false on each boolean method.
   */
  private static class EmptyUserSuretyDelegate implements UserSuretyDelegate {
    @Override
    public boolean hasChallengeCode(Integer userKey) {
      return false;
    }

    @Override
    public boolean isValidChallengeCode(Integer userKey, UUID challengeCode) {
      return false;
    }

    @Override
    public void onNewUser(GbifUser user) {
    }

    @Override
    public boolean confirmUser(GbifUser user, UUID confirmationObject) {
      return false;
    }

    @Override
    public void onPasswordReset(GbifUser user) {
    }
  }

  /**
   * {@link IdentityAccessService} that wraps a {@link IdentityService} to only expose methods that are
   * allowed to be called.
   */
  private static class InnerIdentityService implements IdentityAccessService {

    private final IdentityService identityService;

    @Inject
    InnerIdentityService(IdentityService identityService) {
      this.identityService = identityService;
    }

    @Nullable
    @Override
    public GbifUser get(String s) {
      return identityService.get(s);
    }

    @Nullable
    @Override
    public GbifUser authenticate(String username, String password) {
      return identityService.authenticate(username, password);
    }
  }
}
