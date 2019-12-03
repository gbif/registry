package org.gbif.registry.mail.organization;

import org.gbif.registry.mail.EmailType;

/**
 * Type of emails related to organization endorsement
 */
public enum OrganizationEmailType implements EmailType {

  /**
   * Email 'New organization, requires endorsement'.
   */
  NEW_ORGANIZATION("newOrganization", "confirm_organization_en.ftl"),

  /**
   * Email 'Organization was endorsed'.
   */
  ENDORSEMENT_CONFIRMATION("endorsementConfirmation", "organization_confirmed_en.ftl"),

  /**
   * Email 'Password reminder'.
   */
  PASSWORD_REMINDER("passwordReminder", "organization_password_reminder_en.ftl");

  private final String subjectKey;
  private final String ftlTemplate;

  OrganizationEmailType(String subjectKey, String ftlTemplate) {
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
