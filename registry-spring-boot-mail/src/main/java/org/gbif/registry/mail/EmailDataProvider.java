package org.gbif.registry.mail;

import java.util.Locale;

/**
 * Email helper class which provide template name and email subject.
 */
public interface EmailDataProvider {

  /**
   * Returns email subject for selected locale and email type.
   *
   * @param locale language locale
   * @param emailType email type
   * @param subjectParams computable params for subject message formatting
   * @return email subject
   */
  String getSubject(Locale locale, EmailType emailType, String... subjectParams);

  /**
   * Returns template name for selected locale and email type.
   *
   * @param locale language locale
   * @param emailType email type
   * @return template name
   */
  String getTemplate(Locale locale, EmailType emailType);
}
