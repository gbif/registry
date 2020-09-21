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

import freemarker.template.Template;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

/**
 * Email template processor allows to generate a {@link BaseEmailModel} from a Freemarker template.
 */
public abstract class FreemarkerEmailTemplateProcessor implements EmailTemplateProcessor {

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
        emailType, emailAddress, templateDataModel, locale, Collections.emptySet(), subjectParams);
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
      Set<String> ccAddresses,
      String... subjectParams)
      throws IOException, TemplateException {
    Objects.requireNonNull(emailAddress, "emailAddress shall be provided");
    Objects.requireNonNull(templateDataModel, "templateDataModel shall be provided");
    Objects.requireNonNull(locale, "locale shall be provided");

    Configuration freemarkerConfig = createFreemarkerConfiguration(locale);
    Template freemarkerTemplate = freemarkerConfig.getTemplate(emailType.getTemplate());
    String htmlBody = FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerTemplate, templateDataModel);

    return new BaseEmailModel(
        Collections.singleton(emailAddress),
        emailType.getSubject(locale, emailType, subjectParams),
        htmlBody,
        ccAddresses);
  }

  private Configuration createFreemarkerConfiguration(Locale locale) {
    Configuration freemarkerConfig =
        new Configuration(Configuration.VERSION_2_3_25);
    freemarkerConfig.setLocale(locale);
    freemarkerConfig.setDefaultEncoding(StandardCharsets.UTF_8.name());
    freemarkerConfig.setNumberFormat("0.####");
    freemarkerConfig.setDateFormat("yyyy-mm-dd");
    freemarkerConfig.setClassForTemplateLoading(
        this.getClass(), "/email/templates");
    return freemarkerConfig;
  }
}
