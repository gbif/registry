package org.gbif.registry.surety.email;

import freemarker.template.TemplateException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public interface EmailTemplateProcessor {

  BaseEmailModel buildEmail(EmailType emailType, String emailAddress, Object templateDataModel, @Nullable Locale locale)
      throws IOException, TemplateException;

  BaseEmailModel buildEmail(EmailType emailType, String emailAddress, Object templateDataModel, @Nullable Locale locale,
                            List<String> ccAddresses) throws IOException, TemplateException;
}
