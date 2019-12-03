package org.gbif.registry.identity.service;

import org.gbif.api.model.ChallengeCode;
import org.gbif.api.model.common.GbifUser;
import org.gbif.registry.domain.mail.BaseEmailModel;
import org.gbif.registry.identity.surety.IdentityEmailManager;
import org.gbif.registry.mail.EmailSender;
import org.gbif.registry.mail.util.RegistryMailUtils;
import org.gbif.registry.surety.persistence.ChallengeCodeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * @see UserSuretyDelegate
 */
@Service
class UserSuretyDelegateImpl implements UserSuretyDelegate {

  private static final Logger LOG = LoggerFactory.getLogger(UserSuretyDelegateImpl.class);

  private final ChallengeCodeManager<Integer> challengeCodeManager;
  private EmailSender emailSender;
  private final IdentityEmailManager identityEmailManager;

  public UserSuretyDelegateImpl(
    @Qualifier("emailSender") EmailSender emailSender,
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
      LOG.error(RegistryMailUtils.NOTIFY_ADMIN,
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
    if (confirmationSucceeded) {
      try {
        BaseEmailModel emailModel = identityEmailManager.generateWelcomeEmailModel(user);
        emailSender.send(emailModel);
      } catch (IOException e) {
        LOG.error(RegistryMailUtils.NOTIFY_ADMIN,
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
      LOG.error(RegistryMailUtils.NOTIFY_ADMIN,
        "Error while trying to generate email to reset password of user " + user.getUserName(), e);
      return;
    }
    emailSender.send(emailModel);
  }
}
