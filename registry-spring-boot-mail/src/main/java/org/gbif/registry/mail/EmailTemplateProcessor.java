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
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import freemarker.template.TemplateException;

/** Email helper class which process template and prepare data to send. */
public interface EmailTemplateProcessor {

  /**
   * Construct an email to send.
   *
   * @param emailType email type
   * @param emailAddress email address
   * @param templateDataModel data which fill template
   * @param locale language locale
   * @param subjectParams computable params for subject message formatting
   * @return email data which is ready to be sent
   */
  BaseEmailModel buildEmail(
      EmailType emailType,
      String emailAddress,
      Object templateDataModel,
      @Nullable Locale locale,
      String... subjectParams)
      throws IOException, TemplateException;

  /**
   * Construct an email to send.
   *
   * @param emailType email type
   * @param emailAddress email address
   * @param templateDataModel data which fill template
   * @param locale language locale
   * @param ccAddresses carbon copy addresses
   * @param subjectParams computable params for subject message formatting
   * @return email data which is ready to be sent
   */
  BaseEmailModel buildEmail(
      EmailType emailType,
      String emailAddress,
      Object templateDataModel,
      @Nullable Locale locale,
      List<String> ccAddresses,
      String... subjectParams)
      throws IOException, TemplateException;
}
