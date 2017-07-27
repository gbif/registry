package org.gbif.registry.ws.surety;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.UUID;

import org.apache.commons.lang3.BooleanUtils;

/**
 * Configurations related to organization endorsement.
 */
class OrganizationEmailTemplateConfiguration {

  static final String CONFIRM_ORGANIZATION_URL_TEMPLATE = "mail.urlTemplate.confirmOrganization";
  static final String MAIL_ENABLED_PROPERTY = "mail.enable";
  static final String HELPDESK_EMAIL_PROPERTY = "helpdesk.email";

  private static final ResourceBundle EMAIL_SUBJECT_RESOURCE = ResourceBundle.getBundle(
          "email/subjects/email_subjects", Locale.ENGLISH);

  private final String endorsementUrlTemplate;
  private final boolean emailEnabled;
  private final String helpdeskEmail;

  /**
   * Type of emails related to organization endorsement
   */
  enum EmailType {
    TO_CONFIRM("confirmOrganization", "confirm_organization_en.ftl"),
    CONFIRMED("organizationConfirmed", "organization_confirmed_en.ftl");

    private final String subjectKey;
    private final String ftlTemplate;
    EmailType(String subjectKey, String ftlTemplate) {
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

  static OrganizationEmailTemplateConfiguration from(Properties filteredProperties) {
    return new OrganizationEmailTemplateConfiguration(filteredProperties);
  }

  private OrganizationEmailTemplateConfiguration(Properties filteredProperties){
    endorsementUrlTemplate = filteredProperties.getProperty(CONFIRM_ORGANIZATION_URL_TEMPLATE);
    emailEnabled = BooleanUtils.toBoolean(filteredProperties.getProperty(MAIL_ENABLED_PROPERTY));
    helpdeskEmail = filteredProperties.getProperty(HELPDESK_EMAIL_PROPERTY);
  }

  /**
   * Generate (from a url template) the URL to visit in order to endorse an organization.
   * @param organizationKey
   * @param confirmationKey
   * @return
   * @throws MalformedURLException
   */
  URL generateEndorsementUrl(UUID organizationKey, UUID confirmationKey) throws MalformedURLException {
    return new URL(MessageFormat.format(endorsementUrlTemplate, organizationKey.toString(), confirmationKey.toString()));
  }

  boolean isEmailEnabled() {
    return emailEnabled;
  }

  String getHelpdeskEmail() {
    return helpdeskEmail;
  }

  /**
   * Once we add support for multiple language this method should accept a Locale
   * @param organizationEmailType
   * @return
   */
  String getEmailSubject(EmailType organizationEmailType) {
    return EMAIL_SUBJECT_RESOURCE.getString(organizationEmailType.getSubjectKey());
  }

}
