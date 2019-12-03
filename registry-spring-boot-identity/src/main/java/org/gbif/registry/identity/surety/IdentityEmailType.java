package org.gbif.registry.identity.surety;

import org.gbif.registry.mail.EmailType;

public enum  IdentityEmailType implements EmailType {

  NEW_USER("createAccount", "create_confirmation_en.ftl"),
  RESET_PASSWORD("resetPassword", "reset_password_en.ftl"),
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
