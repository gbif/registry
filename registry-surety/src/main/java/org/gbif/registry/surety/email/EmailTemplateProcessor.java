package org.gbif.registry.surety.email;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nullable;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

import static freemarker.template.Configuration.VERSION_2_3_25;

/**
 * Email template processor allows to generate a {@link BaseEmailModel} from a Freemarker template.
 */
public class EmailTemplateProcessor {

  private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

  //shared config among all instances
  private static final Configuration FREEMARKER_CONFIG = new Configuration(VERSION_2_3_25);

  static {
    FREEMARKER_CONFIG.setDefaultEncoding("UTF-8");
    FREEMARKER_CONFIG.setLocale(Locale.US);
    FREEMARKER_CONFIG.setNumberFormat("0.####");
    FREEMARKER_CONFIG.setDateFormat("yyyy-mm-dd");
    FREEMARKER_CONFIG.setClassForTemplateLoading(EmailTemplateProcessor.class,
                                                 EmailManagerConfiguration.FREEMARKER_TEMPLATES_LOCATION);
  }

  private final Function<Locale, String> subjectProvider;
  private final Function<Locale, String> templateFileProvider;

  /**
   *
   * @param subjectProvider function that returns a subject as a String given a Locale
   * @param templateFileProvider function that returns the name of a Freemarker template given a Locale
   */
  public EmailTemplateProcessor(Function<Locale,String> subjectProvider, Function<Locale,String> templateFileProvider) {
    this.subjectProvider = subjectProvider;
    this.templateFileProvider = templateFileProvider;
  }

  /**
   * Build a {@link BaseEmailModel} from
   * @param emailAddress
   * @param templateDataModel
   * @param locale if null is provided {@link #DEFAULT_LOCALE} will be used
   * @return
   * @throws IOException
   * @throws TemplateException
   */
  public BaseEmailModel buildEmail(String emailAddress, Object templateDataModel, @Nullable Locale locale)
          throws IOException, TemplateException {

    Objects.requireNonNull(emailAddress, "emailAddress shall be provided");
    Objects.requireNonNull(templateDataModel, "templateDataModel shall be provided");

    //at some point this class should be able to check supported locale
    Locale emailLocale = Optional.ofNullable(locale).orElse(DEFAULT_LOCALE);

    // Prepare the E-Mail body text
    StringWriter contentBuffer = new StringWriter();
    FREEMARKER_CONFIG.getTemplate(templateFileProvider.apply(emailLocale)).process(templateDataModel, contentBuffer);
    return new BaseEmailModel(emailAddress, subjectProvider.apply(emailLocale), contentBuffer.toString());
  }

}
