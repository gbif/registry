/*
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

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Type of emails related to 'identity' functionality like: welcome email, create new account and reset
 * password.
 */
public enum IdentityEmailType implements EmailType {

  /**
   * Email 'Account was created, please confirm it'.
   */
  NEW_USER("createAccount", "create_confirmation.ftl"),

  /**
   * Email 'Reset password'.
   */
  RESET_PASSWORD("resetPassword", "reset_password.ftl"),

  /**
   * Email 'Password was changed'.
   */
  PASSWORD_CHANGED("passwordChanged", "password_changed.ftl"),

  /**
   * Welcome email with links and information.
   */
  WELCOME("welcome", "welcome.ftl"),

  /**
   * Email 'User was deleted'
   */
  DELETE_ACCOUNT("deleteAccount", "delete_account.ftl"),

  /**
   * Email 'Change email'.
   */
  CHANGE_EMAIL("changeEmail", "change_email.ftl"),

  /**
   * Email 'Change email'.
   */
  EMAIL_CHANGED("emailChanged", "email_changed.ftl");

  private static final ResourceBundleMessageSource MESSAGE_SOURCE;

  private final String key;
  private final String template;

  static {
    MESSAGE_SOURCE = new ResourceBundleMessageSource();
    MESSAGE_SOURCE.setBasename("email/subjects/identity_email_subjects");
    MESSAGE_SOURCE.setDefaultEncoding(StandardCharsets.UTF_8.displayName());
  }

  IdentityEmailType(String key, String template) {
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
  public String getSubject(Locale locale, String... subjectParams) {
    return MESSAGE_SOURCE.getMessage(this.getKey(), subjectParams, locale);
  }
}
