package org.gbif.registry.mail;

import java.util.Locale;

public interface EmailDataProvider {

  String getSubject(Locale locale, EmailType emailType);

  String getTemplate(Locale locale, EmailType emailType);
}
