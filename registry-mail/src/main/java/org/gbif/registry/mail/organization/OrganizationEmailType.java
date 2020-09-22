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
package org.gbif.registry.mail.organization;

import org.gbif.registry.mail.EmailType;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/** Type of emails related to organization endorsement */
public enum OrganizationEmailType implements EmailType {

  /** Email 'New organization, requires endorsement'. */
  NEW_ORGANIZATION("newOrganization", "confirm_organization.ftl"),

  /** Email 'Organization was endorsed'. */
  ENDORSEMENT_CONFIRMATION("endorsementConfirmation", "organization_confirmed.ftl"),

  /** Email 'Password reminder'. */
  PASSWORD_REMINDER("passwordReminder", "organization_password_reminder.ftl");

  private final String key;
  private final String template;

  OrganizationEmailType(String key, String template) {
    this.key = key;
    this.template = template;
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public String getTemplate() {
    return template;
  }

  @Override
  public String getSubject(Locale locale, EmailType emailType, String... subjectParams) {
    ResourceBundle bundle = ResourceBundle.getBundle("email/subjects/email_subjects", locale);
    String rawSubjectString = bundle.getString(emailType.getKey());
    if (subjectParams.length == 0) {
      return rawSubjectString;
    } else {
      return MessageFormat.format(rawSubjectString, (Object[]) subjectParams);
    }
  }
}
