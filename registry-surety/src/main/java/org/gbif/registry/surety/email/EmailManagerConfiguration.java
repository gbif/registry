package org.gbif.registry.surety.email;

import java.util.Locale;
import java.util.ResourceBundle;
import javax.mail.Session;

/**
 * Container object to simplify handling of the EMail manager configurations.
 */
public class EmailManagerConfiguration {

  static final String FREEMARKER_TEMPLATES_LOCATION = "/email";
  //last part of the path here is not a folder but the prefix of the ResourceBundle (email_subjects_en, email_subjects_fr)
  static final String EMAIL_SUBJECTS = "email/subjects/email_subjects";

  private Session session;
  private String bccAddresses;

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

  ResourceBundle getDefaultEmailSubjects() {
    return ResourceBundle.getBundle(EMAIL_SUBJECTS, Locale.ENGLISH);
  }
}
