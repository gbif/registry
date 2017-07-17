package org.gbif.identity.service;

import org.gbif.api.model.common.GbifUser;
import org.gbif.registry.surety.email.BaseEmailModel;
import org.gbif.registry.surety.email.EmailManager;
import org.gbif.registry.surety.model.ChallengeCode;
import org.gbif.registry.surety.persistence.ChallengeCodeManager;

import java.util.UUID;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @see UserSuretyDelegate
 */
class UserSuretyDelegateImpl implements UserSuretyDelegate {

  private final ChallengeCodeManager<Integer> challengeCodeManager;
  private final EmailManager emailManager;
  private final IdentityEmailTemplateProcessor newUserEmailTemplateProcessor;
  private final IdentityEmailTemplateProcessor resetPasswordEmailTemplateProcessor;

  @Inject
  UserSuretyDelegateImpl(EmailManager emailManager,
                         ChallengeCodeManager<Integer> challengeCodeManager,
                         @Named("newUserEmailTemplateProcessor") IdentityEmailTemplateProcessor newUserEmailTemplateProcessor,
                         @Named("resetPasswordEmailTemplateProcessor") IdentityEmailTemplateProcessor resetPasswordEmailTemplateProcessor) {
    this.emailManager = emailManager;
    this.challengeCodeManager = challengeCodeManager;
    this.newUserEmailTemplateProcessor = newUserEmailTemplateProcessor;
    this.resetPasswordEmailTemplateProcessor = resetPasswordEmailTemplateProcessor;
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
    BaseEmailModel emailModel = newUserEmailTemplateProcessor.generateUserChallengeCodeEmailModel(user, challengeCode);
    emailManager.send(emailModel);
  }

  @Override
  public boolean confirmUser(Integer key, UUID confirmationObject) {
    return key != null
            && challengeCodeManager.isValidChallengeCode(key, confirmationObject)
            && challengeCodeManager.remove(key);
  }

  @Override
  public void onPasswordReset(GbifUser user) {
    ChallengeCode challengeCode = challengeCodeManager.create(user.getKey());
    BaseEmailModel emailModel = resetPasswordEmailTemplateProcessor.generateUserChallengeCodeEmailModel(user, challengeCode);
    emailManager.send(emailModel);
  }

}
