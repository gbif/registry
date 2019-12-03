package org.gbif.registry.mail;

import freemarker.template.TemplateException;
import org.gbif.registry.domain.mail.BaseEmailModel;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Email helper class which process template and prepare data to send.
 */
public interface EmailTemplateProcessor {

  /**
   * Construct an email to send.
   *
   * @param emailType         email type
   * @param emailAddress      email address
   * @param templateDataModel data which fill template
   * @param locale            language locale
   * @param subjectParams     computable params for subject message formatting
   * @return email data which is ready to be sent
   */
  BaseEmailModel buildEmail(EmailType emailType,
                            String emailAddress,
                            Object templateDataModel,
                            @Nullable Locale locale,
                            String... subjectParams)
    throws IOException, TemplateException;

  /**
   * Construct an email to send.
   *
   * @param emailType         email type
   * @param emailAddress      email address
   * @param templateDataModel data which fill template
   * @param locale            language locale
   * @param ccAddresses       carbon copy addresses
   * @param subjectParams     computable params for subject message formatting
   * @return email data which is ready to be sent
   */
  BaseEmailModel buildEmail(EmailType emailType,
                            String emailAddress,
                            Object templateDataModel,
                            @Nullable Locale locale,
                            List<String> ccAddresses,
                            String... subjectParams)
    throws IOException, TemplateException;
}
