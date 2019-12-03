package org.gbif.registry.mail.identity;

import org.gbif.registry.mail.EmailType;

/**
 * Type of emails related to 'identity' stuff like: welcome email, create new account and reset password.
 */
public enum IdentityEmailType implements EmailType {

  /**
   * Email 'Account was created, please confirm it'.
   */
  NEW_USER("createAccount", "create_confirmation_en.ftl"),

  /**
   * Email 'Reset password'.
   */
  RESET_PASSWORD("resetPassword", "reset_password_en.ftl"),

  /**
   * Welcome email with links and information.
   */
  WELCOME("welcome", "welcome_en.ftl");

  private final String subjectKey;
  private final String ftlTemplate;

  IdentityEmailType(String subjectKey, String ftlTemplate) {
    this.subjectKey = subjectKey;
    this.ftlTemplate = ftlTemplate;
  }

  public String getSubjectKey() {
    return subjectKey;
  }

  public String getFtlTemplate() {
    return ftlTemplate;
  }
}
