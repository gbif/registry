/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.identity.service;

import org.gbif.api.model.ChallengeCode;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.occurrence.Download;
import org.gbif.registry.mail.BaseEmailModel;
import org.gbif.registry.mail.EmailSender;
import org.gbif.registry.mail.identity.IdentityEmailManager;
import org.gbif.registry.mail.util.RegistryMailUtils;
import org.gbif.registry.surety.ChallengeCodeManager;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** @see UserSuretyDelegate */
@Service
public class UserSuretyDelegateImpl implements UserSuretyDelegate {

  private static final Logger LOG = LoggerFactory.getLogger(UserSuretyDelegateImpl.class);

  private final ChallengeCodeManager<Integer> challengeCodeManager;
  private final EmailSender emailSender;
  private final IdentityEmailManager identityEmailManager;

  public UserSuretyDelegateImpl(
      EmailSender emailSender,
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
  public boolean isValidChallengeCode(Integer userKey, String email, UUID challengeCode) {
    return challengeCodeManager.isValidChallengeCode(userKey, challengeCode, email);
  }

  @Override
  public void onDeleteUser(GbifUser user, List<Download> downloads) {
    BaseEmailModel emailModel;
    try {
      emailModel = identityEmailManager.generateDeleteUserEmailModel(user, downloads);
    } catch (IOException e) {
      LOG.error(
          RegistryMailUtils.NOTIFY_ADMIN,
          "Error while trying to generate email to delete user " + user.getUserName(),
          e);
      return;
    }
    emailSender.send(emailModel);
  }

  @Override
  public void onNewUser(GbifUser user) {
    ChallengeCode challengeCode = challengeCodeManager.create(user.getKey());
    BaseEmailModel emailModel;
    try {
      emailModel = identityEmailManager.generateNewUserEmailModel(user, challengeCode);
    } catch (IOException e) {
      LOG.error(
          RegistryMailUtils.NOTIFY_ADMIN,
          "Error while trying to generate email to confirm user " + user.getUserName(),
          e);
      return;
    }
    emailSender.send(emailModel);
  }

  @Override
  public boolean confirmUser(GbifUser user, UUID confirmationObject) {
    boolean confirmationSucceeded = false;

    if (user.getKey() != null
        && challengeCodeManager.isValidChallengeCode(user.getKey(), confirmationObject, null)) {
      challengeCodeManager.remove(user.getKey());
      confirmationSucceeded = true;
    }
    return confirmationSucceeded;
  }

  @Override
  public boolean confirmAndNotifyUser(GbifUser user, UUID confirmationObject) {
    boolean confirmationSucceeded = false;

    if (user.getKey() != null
        && challengeCodeManager.isValidChallengeCode(user.getKey(), confirmationObject, null)) {
      challengeCodeManager.remove(user.getKey());
      confirmationSucceeded = true;
    }

    if (confirmationSucceeded) {
      try {
        BaseEmailModel emailModel = identityEmailManager.generateWelcomeEmailModel(user);
        emailSender.send(emailModel);
      } catch (IOException e) {
        LOG.error(
            RegistryMailUtils.NOTIFY_ADMIN,
            "Error while trying to generate welcome email for user " + user.getUserName(),
            e);
      }
    }
    return confirmationSucceeded;
  }

  @Override
  public boolean confirmUserAndEmail(GbifUser user, String email, UUID confirmationObject) {
    boolean confirmationSucceeded = false;

    if (user.getKey() != null
        && challengeCodeManager.isValidChallengeCode(user.getKey(), confirmationObject, email)) {
      challengeCodeManager.remove(user.getKey());
      confirmationSucceeded = true;
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
      LOG.error(
          RegistryMailUtils.NOTIFY_ADMIN,
          "Error while trying to generate email to reset password of user " + user.getUserName(),
          e);
      return;
    }
    emailSender.send(emailModel);
  }

  @Override
  public void onPasswordChanged(GbifUser user) {
    BaseEmailModel emailModel;
    try {
      emailModel = identityEmailManager.generatePasswordChangedEmailModel(user);
    } catch (IOException e) {
      LOG.error(
          RegistryMailUtils.NOTIFY_ADMIN,
          "Error while trying to generate email password was changed of user " + user.getUserName(),
          e);
      return;
    }
    emailSender.send(emailModel);
  }

  @Override
  public void onChangeEmail(GbifUser user, String newEmail) {
    ChallengeCode challengeCode = challengeCodeManager.create(user.getKey(), newEmail);
    BaseEmailModel emailModel;
    try {
      emailModel =
          identityEmailManager.generateAccountEmailChangeEmailModel(user, newEmail, challengeCode);
    } catch (IOException e) {
      LOG.error(
          RegistryMailUtils.NOTIFY_ADMIN,
          "Error while trying to generate email to change email of user " + user.getUserName(),
          e);
      return;
    }
    emailSender.send(emailModel);
  }

  @Override
  public void onEmailChanged(GbifUser user, String oldEmail) {
    BaseEmailModel emailModel;
    try {
      emailModel = identityEmailManager.generateAccountEmailChangedEmailModel(user, oldEmail);
    } catch (IOException e) {
      LOG.error(
          RegistryMailUtils.NOTIFY_ADMIN,
          "Error while trying to generate email 'email changed' of user " + user.getUserName(),
          e);
      return;
    }
    emailSender.send(emailModel);
  }
}
