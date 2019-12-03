package org.gbif.registry.ws.surety;

import org.gbif.registry.mail.EmailDataProvider;
import org.gbif.registry.mail.EmailType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.ResourceBundle;

@Service
@Qualifier("organizationEmailDataProvider")
public class OrganizationEmailDataProvider implements EmailDataProvider {

  private static final ResourceBundle EMAIL_SUBJECT_RESOURCE = ResourceBundle.getBundle(
      "email/subjects/email_subjects", Locale.ENGLISH);

  @Override
  public String getSubject(Locale locale, EmailType emailType) {
    return EMAIL_SUBJECT_RESOURCE.getString(emailType.getSubjectKey());
  }

  @Override
  public String getTemplate(Locale locale, EmailType emailType) {
    return emailType.getFtlTemplate();
  }
}
