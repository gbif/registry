package org.gbif.identity.service;

import org.gbif.api.model.common.GbifUser;
import org.gbif.identity.surety.IdentityEmailManager;
import org.gbif.registry.surety.SuretyConstants;
import org.gbif.registry.surety.email.BaseEmailModel;
import org.gbif.registry.surety.email.EmailSender;
import org.gbif.registry.surety.model.ChallengeCode;
import org.gbif.registry.surety.persistence.ChallengeCodeManager;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @see UserSuretyDelegate
 */
class UserSuretyDelegateImpl implements UserSuretyDelegate {

  private static final Logger LOG = LoggerFactory.getLogger(UserSuretyDelegateImpl.class);

  private final ChallengeCodeManager<Integer> challengeCodeManager;
  private final EmailSender emailSender;
  private final IdentityEmailManager identityEmailManager;

  @Inject
  UserSuretyDelegateImpl(EmailSender emailSender,
                         ChallengeCodeManager<Integer> challengeCodeManager,
                         IdentityEmailManager identityEmailManager) {
    this.emailSender = emailSender;
    this.challengeCodeManager = challengeCodeManager;
    this.identityEmailManager = identityEmailManager;
  }

  @Override
  public boolean hasChallengeCode(Integer userKey) {
    return challengeCodeManager.hasChallengeCode(userKey);
  }

  @Override
  public boolean isValidChallengeCode(Integer userKey, UUID challengeCode) {
    return challengeCodeManager.isValidChallengeCode(userKey, challengeCode);
  }

  @Override
  public void onNewUser(GbifUser user) {
    ChallengeCode challengeCode = challengeCodeManager.create(user.getKey());
    BaseEmailModel emailModel;
    try {
      emailModel = identityEmailManager.generateNewUserEmailModel(user, challengeCode);
    } catch (IOException e) {
      LOG.error(SuretyConstants.NOTIFY_ADMIN,
              "Error while trying to generate email to confirm user " + user.getUserName(), e);
      return;
    }
    emailSender.send(emailModel);
  }

  @Override
  public boolean confirmUser(GbifUser user, UUID confirmationObject) {
    Boolean confirmationSucceeded = Optional.ofNullable(user.getKey())
            .map(keyVal -> challengeCodeManager.isValidChallengeCode(keyVal, confirmationObject)
                           && challengeCodeManager.remove(keyVal))
            .orElse(Boolean.FALSE);
    if(confirmationSucceeded){
      try {
        BaseEmailModel emailModel = identityEmailManager.generateWelcomeEmailModel(user);
        emailSender.send(emailModel);
      } catch (IOException e) {
        LOG.error(SuretyConstants.NOTIFY_ADMIN,
                "Error while trying to generate welcome email for user " + user.getUserName(), e);
      }
    }
    return confirmationSucceeded;
  }

  @Override
  public void onPasswordReset(GbifUser user) {
    ChallengeCode challengeCode = challengeCodeManager.create(user.getKey());
    BaseEmailModel emailModel;
    try {
      emailModel = identityEmailManager.generateResetPasswordEmailModel(user, challengeCode);
    } catch (IOException e) {
      LOG.error(SuretyConstants.NOTIFY_ADMIN,
              "Error while trying to generate email to reset password of user " + user.getUserName(), e);
      return;
    }
    emailSender.send(emailModel);
  }

}
