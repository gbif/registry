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

import java.util.Locale;

/** Email helper class which provide template name and email subject. */
public interface EmailDataProvider {

  String EMAIL_TEMPLATES_PATH = "email/templates/email_templates";

  /**
   * Returns email subject for selected locale and email type.
   *
   * @param locale language locale
   * @param emailType email type
   * @param subjectParams computable params for subject message formatting
   * @return email subject
   */
  String getSubject(Locale locale, EmailType emailType, String... subjectParams);

  /**
   * Returns template name for selected locale and email type.
   *
   * @param locale language locale
   * @param emailType email type
   * @return template name
   */
  String getTemplate(Locale locale, EmailType emailType);
}
