package org.gbif.identity.service;

import org.gbif.api.model.common.GbifUser;
import org.gbif.registry.surety.email.BaseEmailModel;
import org.gbif.registry.surety.email.BaseTemplateDataModel;
import org.gbif.registry.surety.email.EmailTemplateProcessor;
import org.gbif.registry.surety.model.ChallengeCode;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Objects;

import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One processor per type of email to send.
 */
public class IdentityEmailTemplateProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(IdentityEmailTemplateProcessor.class);

  private final EmailTemplateProcessor emailTemplateProcessor;
  private final String urlTemplate;

  public IdentityEmailTemplateProcessor(EmailTemplateProcessor emailTemplateProcessor, String urlTemplate) {
    Objects.requireNonNull(emailTemplateProcessor, "emailTemplateProcessor shall be provided");
    Objects.requireNonNull(urlTemplate, "urlTemplate shall be provided");

    this.emailTemplateProcessor = emailTemplateProcessor;
    this.urlTemplate = urlTemplate;
  }

  /**
   * Email model that only includes a username and a formatted URL for a specific username and challenge code.
   * @return new {@link BaseEmailModel} or null if an error occurred
   */
  public BaseEmailModel generateUserChallengeCodeEmailModel(GbifUser user, ChallengeCode challengeCode) {
    BaseEmailModel baseEmailModel = null;
    try {
      URL url = new URL(MessageFormat.format(urlTemplate, user.getUserName(), challengeCode.getCode()));
      BaseTemplateDataModel dataModel = new BaseTemplateDataModel(user.getUserName(), url);
      baseEmailModel = emailTemplateProcessor.buildEmail(user.getEmail(), dataModel, Locale.ENGLISH);
    } catch (TemplateException | IOException ex) {
      LOG.error("Error while generating e-mail.", ex);
    }
    return baseEmailModel;
  }

}


