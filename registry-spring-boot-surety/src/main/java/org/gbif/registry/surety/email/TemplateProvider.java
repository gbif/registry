package org.gbif.registry.surety.email;

import java.util.Locale;

public interface TemplateProvider {

  String getTemplate(Locale locale);
}
