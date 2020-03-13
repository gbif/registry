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
package org.gbif.registry.mail.identity;

import org.gbif.api.model.ChallengeCode;
import org.gbif.api.model.common.GbifUser;
import org.gbif.registry.domain.mail.BaseEmailModel;
import org.gbif.registry.domain.mail.BaseTemplateDataModel;
import org.gbif.registry.mail.EmailTemplateProcessor;
import org.gbif.registry.mail.EmailType;
import org.gbif.registry.mail.config.IdentitySuretyMailConfigurationProperties;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import freemarker.template.TemplateException;

/** Manager responsible to generate {@link BaseEmailModel}. */
@Service
public class IdentityEmailManager {

  private final EmailTemplateProcessor emailTemplateProcessor;
  private final IdentitySuretyMailConfigurationProperties identityMailConfigProperties;

  public IdentityEmailManager(
      @Qualifier("identityEmailTemplateProcessor") EmailTemplateProcessor emailTemplateProcessor,
      IdentitySuretyMailConfigurationProperties identityMailConfigProperties) {
    this.emailTemplateProcessor = emailTemplateProcessor;
    this.identityMailConfigProperties = identityMailConfigProperties;
  }

  public BaseEmailModel generateNewUserEmailModel(GbifUser user, ChallengeCode challengeCode)
      throws IOException {
    try {
      return generateConfirmationEmailModel(
          user,
          generateConfirmUserUrl(user.getUserName(), challengeCode.getCode()),
          IdentityEmailType.NEW_USER);
    } catch (TemplateException e) {
      throw new IOException(e);
    }
  }

  public BaseEmailModel generateResetPasswordEmailModel(GbifUser user, ChallengeCode challengeCode)
      throws IOException {
    try {
      return generateConfirmationEmailModel(
          user,
          generateResetPasswordUrl(user.getUserName(), challengeCode.getCode()),
          IdentityEmailType.RESET_PASSWORD);
    } catch (TemplateException e) {
      throw new IOException(e);
    }
  }

  public BaseEmailModel generateWelcomeEmailModel(GbifUser user) throws IOException {
    try {
      return emailTemplateProcessor.buildEmail(
          IdentityEmailType.WELCOME, user.getEmail(), new Object(), Locale.ENGLISH);
    } catch (TemplateException e) {
      throw new IOException(e);
    }
  }

  /**
   * Email model that only includes a username and a formatted URL for a specific username and
   * challenge code.
   *
   * @return new {@link BaseEmailModel} or null if an error occurred
   */
  private BaseEmailModel generateConfirmationEmailModel(GbifUser user, URL url, EmailType emailType)
      throws IOException, TemplateException {
    BaseTemplateDataModel dataModel = new BaseTemplateDataModel(user.getUserName(), url);
    return emailTemplateProcessor.buildEmail(emailType, user.getEmail(), dataModel, Locale.ENGLISH);
  }

  private URL generateConfirmUserUrl(String userName, UUID confirmationKey)
      throws MalformedURLException {
    return new URL(
        MessageFormat.format(
            identityMailConfigProperties.getUrlTemplate().getConfirmUser(),
            userName,
            confirmationKey.toString()));
  }

  private URL generateResetPasswordUrl(String userName, UUID confirmationKey)
      throws MalformedURLException {
    return new URL(
        MessageFormat.format(
            identityMailConfigProperties.getUrlTemplate().getResetPassword(),
            userName,
            confirmationKey.toString()));
  }
}
