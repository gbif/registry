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

import org.gbif.registry.mail.EmailType;

/**
 * Type of emails related to 'identity' stuff like: welcome email, create new account and reset
 * password.
 */
public enum IdentityEmailType implements EmailType {

  /** Email 'Account was created, please confirm it'. */
  NEW_USER("createAccount", "create_confirmation_en.ftl"),

  /** Email 'Reset password'. */
  RESET_PASSWORD("resetPassword", "reset_password_en.ftl"),

  /** Email 'Password was changed'. */
  PASSWORD_CHANGED("passwordChanged", "password_changed_en.ftl"),

  /** Welcome email with links and information. */
  WELCOME("welcome", "welcome_en.ftl"),

  /** Email 'User was deleted' */
  DELETE_ACCOUNT("deleteAccount", "delete_account_en.ftl");

  private final String subjectKey;
  private final String ftlTemplate;

  IdentityEmailType(String subjectKey, String ftlTemplate) {
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
