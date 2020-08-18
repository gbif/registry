/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
import org.gbif.registry.domain.mail.BaseEmailModel;
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
  private EmailSender emailSender;
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
  public boolean isValidChallengeCode(Integer userKey, UUID challengeCode) {
    return challengeCodeManager.isValidChallengeCode(userKey, challengeCode);
  }

  @Override
  public void onDeleteUser(String username, String email, List<Download> downloads) {
    BaseEmailModel emailModel;
    try {
      emailModel = identityEmailManager.generateDeleteUserEmailModel(username, email, downloads);
    } catch (IOException e) {
      LOG.error(
          RegistryMailUtils.NOTIFY_ADMIN,
          "Error while trying to generate email to delete user " + username,
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
        && challengeCodeManager.isValidChallengeCode(user.getKey(), confirmationObject)) {
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
}
