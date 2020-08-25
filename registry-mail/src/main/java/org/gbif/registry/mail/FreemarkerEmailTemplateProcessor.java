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
package org.gbif.registry.mail;

import org.gbif.registry.domain.mail.BaseEmailModel;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

/**
 * Email template processor allows to generate a {@link BaseEmailModel} from a Freemarker template.
 */
public abstract class FreemarkerEmailTemplateProcessor implements EmailTemplateProcessor {

  // shared config among all instances
  private static final Configuration FREEMARKER_CONFIG =
      new Configuration(Configuration.VERSION_2_3_25);

  static {
    FREEMARKER_CONFIG.setDefaultEncoding(StandardCharsets.UTF_8.name());
    FREEMARKER_CONFIG.setLocale(Locale.US);
    FREEMARKER_CONFIG.setNumberFormat("0.####");
    FREEMARKER_CONFIG.setDateFormat("yyyy-mm-dd");
    FREEMARKER_CONFIG.setClassForTemplateLoading(FreemarkerEmailTemplateProcessor.class, "/email");
  }

  /**
   * Build a {@link BaseEmailModel} from
   *
   * @param emailType template type (new user, reset password or welcome)
   * @param emailAddress email address
   * @param templateDataModel source data
   * @param locale locale
   * @param subjectParams computable params for subject message formatting
   * @return email model to send
   */
  @Override
  public BaseEmailModel buildEmail(
      EmailType emailType,
      String emailAddress,
      Object templateDataModel,
      Locale locale,
      String... subjectParams)
      throws IOException, TemplateException {
    return buildEmail(
        emailType, emailAddress, templateDataModel, locale, Collections.emptyList(), subjectParams);
  }

  /**
   * Build a {@link BaseEmailModel} from
   *
   * @param emailType template type (new user, reset password or welcome)
   * @param emailAddress email address
   * @param templateDataModel source data
   * @param locale locale
   * @param ccAddresses carbon copy addresses
   * @param subjectParams computable params for subject message formatting
   * @return email model to send
   */
  @Override
  public BaseEmailModel buildEmail(
      EmailType emailType,
      String emailAddress,
      Object templateDataModel,
      Locale locale,
      List<String> ccAddresses,
      String... subjectParams)
      throws IOException, TemplateException {
    Objects.requireNonNull(emailAddress, "emailAddress shall be provided");
    Objects.requireNonNull(templateDataModel, "templateDataModel shall be provided");
    Objects.requireNonNull(locale, "locale shall be provided");

    // Prepare the E-Mail body text
    StringWriter contentBuffer = new StringWriter();
    FREEMARKER_CONFIG
        .getTemplate(getEmailDataProvider().getTemplate(locale, emailType))
        .process(templateDataModel, contentBuffer);
    return new BaseEmailModel(
        emailAddress,
        getEmailDataProvider().getSubject(locale, emailType, subjectParams),
        contentBuffer.toString(),
        ccAddresses);
  }

  public abstract EmailDataProvider getEmailDataProvider();
}
