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

/** Type of emails related to organization endorsement */
public enum OrganizationEmailType implements EmailType {

  /** Email 'New organization, requires endorsement'. */
  NEW_ORGANIZATION("newOrganization", "confirm_organization_en.ftl"),

  /** Email 'Organization was endorsed'. */
  ENDORSEMENT_CONFIRMATION("endorsementConfirmation", "organization_confirmed_en.ftl"),

  /** Email 'Password reminder'. */
  PASSWORD_REMINDER("passwordReminder", "organization_password_reminder_en.ftl");

  private final String subjectKey;
  private final String ftlTemplate;

  OrganizationEmailType(String subjectKey, String ftlTemplate) {
    this.subjectKey = subjectKey;
    this.ftlTemplate = ftlTemplate;
  }

  @Override
  public String getSubjectKey() {
    return subjectKey;
  }

  @Override
  public String getFtlTemplate() {
    return ftlTemplate;
  }
}
