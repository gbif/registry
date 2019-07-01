package org.gbif.registry.identity.surety;

import freemarker.template.TemplateException;
import org.gbif.api.model.ChallengeCode;
import org.gbif.api.model.common.GbifUser;
import org.gbif.registry.surety.email.BaseEmailModel;
import org.gbif.registry.surety.email.BaseTemplateDataModel;
import org.gbif.registry.surety.email.EmailTemplateProcessor;
import org.gbif.registry.surety.email.EmailType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;

/**
 * Manager responsible to generate {@link BaseEmailModel} for each {@link IdentityEmailConfiguration.EmailType}.
 */
@Service
public class IdentityEmailManager {

  // TODO: 2019-07-01 replace these fields with beans
  private final IdentityEmailConfiguration identityEmailConfiguration;
  private final EmailTemplateProcessor emailTemplateProcessor;

  public IdentityEmailManager(IdentityEmailConfiguration identityEmailConfiguration, EmailTemplateProcessor emailTemplateProcessor) {
    this.identityEmailConfiguration = identityEmailConfiguration;
    this.emailTemplateProcessor = emailTemplateProcessor;
  }

  public BaseEmailModel generateNewUserEmailModel(GbifUser user, ChallengeCode challengeCode) throws IOException {
    try {
      return generateConfirmationEmailModel(user, identityEmailConfiguration.generateConfirmUserUrl(user.getUserName(), challengeCode.getCode()),
          IdentityEmailType.NEW_USER);
    } catch (TemplateException e) {
      throw new IOException(e);
    }
  }

  public BaseEmailModel generateResetPasswordEmailModel(GbifUser user, ChallengeCode challengeCode) throws IOException {
    try {
      return generateConfirmationEmailModel(user, identityEmailConfiguration.generateResetPasswordUrl(user.getUserName(), challengeCode.getCode()),
          IdentityEmailType.RESET_PASSWORD);
    } catch (TemplateException e) {
      throw new IOException(e);
    }
  }

  public BaseEmailModel generateWelcomeEmailModel(GbifUser user) throws IOException {
    try {
      return emailTemplateProcessor.buildEmail(IdentityEmailType.WELCOME, user.getEmail(), new Object(), Locale.ENGLISH);
    } catch (TemplateException e) {
      throw new IOException(e);
    }
  }

  /**
   * Email model that only includes a username and a formatted URL for a specific username and challenge code.
   *
   * @return new {@link BaseEmailModel} or null if an error occurred
   */
  private BaseEmailModel generateConfirmationEmailModel(GbifUser user, URL url, EmailType emailType)
      throws IOException, TemplateException {
    BaseTemplateDataModel dataModel = new BaseTemplateDataModel(user.getUserName(), url);
    return emailTemplateProcessor.buildEmail(emailType, user.getEmail(), dataModel, Locale.ENGLISH);
  }
}
