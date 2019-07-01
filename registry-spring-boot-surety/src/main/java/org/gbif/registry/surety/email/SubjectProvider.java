package org.gbif.registry.surety.email;

import java.util.Locale;

public interface SubjectProvider {

  String getSubject(Locale locale);
}
