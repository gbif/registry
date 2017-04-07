package org.gbif.identity.email;

import java.util.Locale;
import java.util.ResourceBundle;
import javax.mail.Session;

/**
 * Container object to simplify handling of the EMail manager configurations.
 */
public class IdentityEmailManagerConfiguration {

  static final String FREEMARKER_TEMPLATES_LOCATION = "/email";
  static final String USER_CREATE_TEMPLATE = "create_confirmation_en.ftl";
  static final String RESET_PASSWORD_TEMPLATE = "reset_password_en.ftl";

  static final String EMAIL_SUBJECTS = "email/email_subjects";

  static final String USER_CREATE_SUBJECT_KEY = "createAccount";
  static final String RESET_PASSWORD_SUBJECT_KEY = "resetPassword";

  private Session session;
  private String bccAddresses;

  private String confirmUrlTemplate;
  private String resetPasswordUrlTemplate;


  public String getResetPasswordUrlTemplate() {
    return resetPasswordUrlTemplate;
  }

  public void setResetPasswordUrlTemplate(String resetPasswordUrlTemplate) {
    this.resetPasswordUrlTemplate = resetPasswordUrlTemplate;
  }

  public Session getSession() {
    return session;
  }

  public void setSession(Session session) {
    this.session = session;
  }

  public String getBccAddresses() {
    return bccAddresses;
  }

  public void setBccAddresses(String bccAddresses) {
    this.bccAddresses = bccAddresses;
  }

  public String getConfirmUrlTemplate() {
    return confirmUrlTemplate;
  }

  public void setConfirmUrlTemplate(String confirmUrlTemplate) {
    this.confirmUrlTemplate = confirmUrlTemplate;
  }

  ResourceBundle getDefaultEmailSubjects() {
    return ResourceBundle.getBundle(EMAIL_SUBJECTS, Locale.ENGLISH);
  }

//  ResourceBundle getDefaultEmailSubjects(Locale targetLocale) {
//    return ResourceBundle.getBundle(EMAIL_SUBJECTS, targetLocale);
//  }



}
