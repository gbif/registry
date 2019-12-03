package org.gbif.registry.mail;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.gbif.registry.domain.mail.BaseEmailModel;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Email template processor allows to generate a {@link BaseEmailModel} from a Freemarker template.
 */
public abstract class FreemarkerEmailTemplateProcessor implements EmailTemplateProcessor {

  private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

  // shared config among all instances
  private static final Configuration FREEMARKER_CONFIG = new Configuration(Configuration.VERSION_2_3_25);

  static {
    FREEMARKER_CONFIG.setDefaultEncoding(StandardCharsets.UTF_8.name());
    FREEMARKER_CONFIG.setLocale(Locale.US);
    FREEMARKER_CONFIG.setNumberFormat("0.####");
    FREEMARKER_CONFIG.setDateFormat("yyyy-mm-dd");
    FREEMARKER_CONFIG.setClassForTemplateLoading(FreemarkerEmailTemplateProcessor.class,
      "/email");
  }

  /**
   * Build a {@link BaseEmailModel} from
   *
   * @param emailType         template type (new user, reset password or welcome)
   * @param emailAddress      email address
   * @param templateDataModel source data
   * @param locale            if null is provided {@link #DEFAULT_LOCALE} will be used
   * @param subjectParams     computable params for subject message formatting
   * @return email model to send
   */
  public BaseEmailModel buildEmail(EmailType emailType,
                                   String emailAddress,
                                   Object templateDataModel,
                                   @Nullable Locale locale,
                                   String... subjectParams)
    throws IOException, TemplateException {
    return buildEmail(emailType, emailAddress, templateDataModel, locale, null, subjectParams);
  }

  /**
   * Build a {@link BaseEmailModel} from
   *
   * @param emailType         template type (new user, reset password or welcome)
   * @param emailAddress      email address
   * @param templateDataModel source data
   * @param locale            if null is provided {@link #DEFAULT_LOCALE} will be used
   * @param ccAddresses       carbon copy addresses
   * @param subjectParams     computable params for subject message formatting
   * @return email model to send
   */
  public BaseEmailModel buildEmail(EmailType emailType,
                                   String emailAddress,
                                   Object templateDataModel,
                                   @Nullable Locale locale,
                                   List<String> ccAddresses,
                                   String... subjectParams)
    throws IOException, TemplateException {
    Objects.requireNonNull(emailAddress, "emailAddress shall be provided");
    Objects.requireNonNull(templateDataModel, "templateDataModel shall be provided");

    // at some point this class should be able to check supported locale
    Locale emailLocale = Optional.ofNullable(locale).orElse(DEFAULT_LOCALE);

    // Prepare the E-Mail body text
    StringWriter contentBuffer = new StringWriter();
    FREEMARKER_CONFIG.getTemplate(getEmailDataProvider().getTemplate(emailLocale, emailType)).process(templateDataModel, contentBuffer);
    return new BaseEmailModel(emailAddress, getEmailDataProvider().getSubject(emailLocale, emailType, subjectParams), contentBuffer.toString(), ccAddresses);
  }

  public abstract EmailDataProvider getEmailDataProvider();
}
