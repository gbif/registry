package org.gbif.identity.surety;

import org.gbif.api.model.common.GbifUser;
import org.gbif.registry.surety.email.BaseEmailModel;
import org.gbif.registry.surety.email.BaseTemplateDataModel;
import org.gbif.registry.surety.email.EmailTemplateProcessor;
import org.gbif.registry.surety.model.ChallengeCode;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Map;

import freemarker.template.TemplateException;

/**
 * Manager responsible to generate {@link BaseEmailModel} for each {@link IdentityEmailConfiguration.EmailType}.
 */
public class IdentityEmailManager {

  private final IdentityEmailConfiguration identityEmailConfiguration;
  private final Map<IdentityEmailConfiguration.EmailType, EmailTemplateProcessor> templateProcessors;

  IdentityEmailManager(IdentityEmailConfiguration identityEmailConfiguration,
                              Map<IdentityEmailConfiguration.EmailType, EmailTemplateProcessor> templateProcessors) {
    this.identityEmailConfiguration = identityEmailConfiguration;
    this.templateProcessors = templateProcessors;
  }

  /**
   * Email model that only includes a username and a formatted URL for a specific username and challenge code.
   * @return new {@link BaseEmailModel} or null if an error occurred
   */
  public BaseEmailModel generateNewUserEmailModel(GbifUser user, ChallengeCode challengeCode) throws IOException {
    try {
      return generateConfirmationEmailModel(user, identityEmailConfiguration.generateConfirmUserUrl(user.getUserName(), challengeCode.getCode()),
              IdentityEmailConfiguration.EmailType.NEW_USER);
    } catch (TemplateException e) {
      throw new IOException(e);
    }
  }

  public BaseEmailModel generateResetPasswordEmailModel(GbifUser user, ChallengeCode challengeCode) throws IOException {
    try {
      return generateConfirmationEmailModel(user, identityEmailConfiguration.generateResetPasswordUrl(user.getUserName(), challengeCode.getCode()),
              IdentityEmailConfiguration.EmailType.RESET_PASSWORD);
    } catch (TemplateException e) {
      throw new IOException(e);
    }
  }

  private BaseEmailModel generateConfirmationEmailModel(GbifUser user, URL url, IdentityEmailConfiguration.EmailType emailType)
          throws IOException, TemplateException {
    BaseTemplateDataModel dataModel = new BaseTemplateDataModel(user.getUserName(), url);
    return templateProcessors.get(emailType).buildEmail(user.getEmail(), dataModel, Locale.ENGLISH);
  }
}
