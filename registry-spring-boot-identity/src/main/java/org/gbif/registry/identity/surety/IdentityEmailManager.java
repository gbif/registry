package org.gbif.registry.identity.surety;

import freemarker.template.TemplateException;
import org.gbif.api.model.ChallengeCode;
import org.gbif.api.model.common.GbifUser;
import org.gbif.registry.domain.mail.BaseEmailModel;
import org.gbif.registry.domain.mail.BaseTemplateDataModel;
import org.gbif.registry.mail.EmailTemplateProcessor;
import org.gbif.registry.mail.EmailType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.UUID;

/**
 * Manager responsible to generate {@link BaseEmailModel}.
 */
@Service
public class IdentityEmailManager {

  // TODO: 2019-08-22 move to ConfigurationProperties
  @Value("${identity.surety.mail.urlTemplate.confirmUser}")
  private String confirmUserUrlTemplate;

  @Value("${identity.surety.mail.urlTemplate.resetPassword}")
  private String resetPasswordUrlTemplate;

  private final EmailTemplateProcessor emailTemplateProcessor;

  public IdentityEmailManager(@Qualifier("identityEmailTemplateProcessor") EmailTemplateProcessor emailTemplateProcessor) {
    this.emailTemplateProcessor = emailTemplateProcessor;
  }

  public BaseEmailModel generateNewUserEmailModel(GbifUser user, ChallengeCode challengeCode) throws IOException {
    try {
      return generateConfirmationEmailModel(user, generateConfirmUserUrl(user.getUserName(), challengeCode.getCode()),
        IdentityEmailType.NEW_USER);
    } catch (TemplateException e) {
      throw new IOException(e);
    }
  }

  public BaseEmailModel generateResetPasswordEmailModel(GbifUser user, ChallengeCode challengeCode) throws IOException {
    try {
      return generateConfirmationEmailModel(user, generateResetPasswordUrl(user.getUserName(), challengeCode.getCode()),
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

  private URL generateConfirmUserUrl(String userName, UUID confirmationKey) throws MalformedURLException {
    return new URL(MessageFormat.format(confirmUserUrlTemplate, userName, confirmationKey.toString()));
  }

  private URL generateResetPasswordUrl(String userName, UUID confirmationKey) throws MalformedURLException {
    return new URL(MessageFormat.format(resetPasswordUrlTemplate, userName, confirmationKey.toString()));
  }
}
