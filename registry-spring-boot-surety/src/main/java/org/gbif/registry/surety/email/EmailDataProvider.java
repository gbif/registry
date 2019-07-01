package org.gbif.registry.surety.email;

import java.util.Locale;

public interface EmailDataProvider {

  String getSubject(Locale locale, EmailType emailType);

  String getTemplate(Locale locale, EmailType emailType);
}
