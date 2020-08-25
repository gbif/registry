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
  NEW_ORGANIZATION("newOrganization"),

  /** Email 'Organization was endorsed'. */
  ENDORSEMENT_CONFIRMATION("endorsementConfirmation"),

  /** Email 'Password reminder'. */
  PASSWORD_REMINDER("passwordReminder");

  private final String key;

  OrganizationEmailType(String key) {
    this.key = key;
  }

  @Override
  public String getKey() {
    return key;
  }
}
