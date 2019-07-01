package org.gbif.registry.identity.surety;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * Holder for configuration related to the Identity emails.
 */
class IdentityEmailConfiguration {

  static final String CONFIRM_USER_URL_TEMPLATE = "mail.urlTemplate.confirmUser";
  static final String RESET_PASSWORD_URL_TEMPLATE = "mail.urlTemplate.resetPassword";

  //last part of the path here is not a folder but the prefix of the ResourceBundle (email_subjects_en, email_subjects_fr)
  private static final ResourceBundle EMAIL_SUBJECT_RESOURCE = ResourceBundle.getBundle(
      "email/subjects/identity_email_subjects", Locale.ENGLISH);
  /**
   * Type of emails related to organization endorsement
   */
  enum EmailType {
    NEW_USER("createAccount", "create_confirmation_en.ftl"),
    RESET_PASSWORD("resetPassword", "reset_password_en.ftl"),
    WELCOME("welcome", "welcome_en.ftl");

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

  private final String confirmUserUrlTemplate;
  private final String resetPasswordUrlTemplate;

  public static IdentityEmailConfiguration from(Properties filteredProperties) {
    return new IdentityEmailConfiguration(filteredProperties);
  }

  private IdentityEmailConfiguration(Properties filteredProperties){
    confirmUserUrlTemplate = filteredProperties.getProperty(CONFIRM_USER_URL_TEMPLATE);
    resetPasswordUrlTemplate = filteredProperties.getProperty(RESET_PASSWORD_URL_TEMPLATE);
  }

  /**
   * Once we add support for multiple language this method should accept a Locale
   * @param identityEmailType
   * @return
   */
  String getEmailSubject(EmailType identityEmailType) {
    return EMAIL_SUBJECT_RESOURCE.getString(identityEmailType.getSubjectKey());
  }

  URL generateConfirmUserUrl(String userName, UUID confirmationKey) throws MalformedURLException {
    return new URL(MessageFormat.format(confirmUserUrlTemplate, userName, confirmationKey.toString()));
  }

  URL generateResetPasswordUrl(String userName, UUID confirmationKey) throws MalformedURLException {
    return new URL(MessageFormat.format(resetPasswordUrlTemplate, userName, confirmationKey.toString()));
  }

}
