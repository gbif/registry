package org.gbif.registry.ws.surety;

import org.gbif.registry.surety.email.EmailType;

/**
 * Type of emails related to organization endorsement
 */
public enum OrganizationEmailType implements EmailType {

  NEW_ORGANIZATION("newOrganization", "confirm_organization_en.ftl"),
  ENDORSEMENT_CONFIRMATION("endorsementConfirmation", "organization_confirmed_en.ftl");

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
