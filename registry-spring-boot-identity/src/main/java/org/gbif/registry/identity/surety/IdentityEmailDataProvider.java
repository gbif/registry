package org.gbif.registry.identity.surety;

import org.gbif.registry.surety.email.EmailDataProvider;
import org.gbif.registry.surety.email.EmailType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.ResourceBundle;

@Service
@Qualifier("identityEmailDataProvider")
public class IdentityEmailDataProvider implements EmailDataProvider {

  //last part of the path here is not a folder but the prefix of the ResourceBundle (email_subjects_en, email_subjects_fr)
  private static final ResourceBundle EMAIL_SUBJECT_RESOURCE = ResourceBundle.getBundle(
      "email/subjects/identity_email_subjects", Locale.ENGLISH);

  @Override
  public String getSubject(Locale locale, EmailType emailType) {
    return EMAIL_SUBJECT_RESOURCE.getString(emailType.getSubjectKey());
  }

  @Override
  public String getTemplate(Locale locale, EmailType emailType) {
    return emailType.getFtlTemplate();
  }
}
