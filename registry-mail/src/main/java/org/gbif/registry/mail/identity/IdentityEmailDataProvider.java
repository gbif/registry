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

import org.gbif.registry.mail.EmailDataProvider;
import org.gbif.registry.mail.EmailType;

import java.util.Locale;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("identityEmailDataProvider")
public class IdentityEmailDataProvider implements EmailDataProvider {

  public static final String IDENTITY_EMAIL_SUBJECTS_PATH =
      "email/subjects/identity_email_subjects";

  @Override
  public String getSubject(Locale locale, EmailType emailType, String... subjectParams) {
    ResourceBundle bundle = ResourceBundle.getBundle(IDENTITY_EMAIL_SUBJECTS_PATH, locale);
    return bundle.getString(emailType.getKey());
  }

  @Override
  public String getTemplate(Locale locale, EmailType emailType) {
    ResourceBundle bundle = ResourceBundle.getBundle(EMAIL_TEMPLATES_PATH, locale);
    return bundle.getString(emailType.getKey());
  }
}
