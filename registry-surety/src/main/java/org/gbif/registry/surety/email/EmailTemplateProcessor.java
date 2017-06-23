package org.gbif.registry.surety.email;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import static freemarker.template.Configuration.VERSION_2_3_25;

/**
 * Email template processor allows to generate a {@link BaseEmailModel} from a Freemarker template.
 */
public class EmailTemplateProcessor {

  //shared config among all instances
  private static final Configuration FREEMARKER_CONFIG = new Configuration(VERSION_2_3_25);
  static {
    FREEMARKER_CONFIG.setDefaultEncoding("UTF-8");
    FREEMARKER_CONFIG.setLocale(Locale.US);
    FREEMARKER_CONFIG.setNumberFormat("0.####");
    FREEMARKER_CONFIG.setDateFormat("yyyy-mm-dd");
    FREEMARKER_CONFIG.setClassForTemplateLoading(EmailTemplateProcessor.class, EmailManagerConfiguration.FREEMARKER_TEMPLATES_LOCATION);
  }

  private final EmailManagerConfiguration emailManagerConfiguration;
  private final String subjectKey;
  private final String templateFile;

  public EmailTemplateProcessor(EmailManagerConfiguration emailManagerConfiguration,
                                String subjectKey, String templateFile) {
    this.emailManagerConfiguration = emailManagerConfiguration;
    this.subjectKey = subjectKey;
    this.templateFile = templateFile;
  }

  public BaseEmailModel buildEmail(String emailAddress, Object templateDataModel) throws IOException, TemplateException {
    // Prepare the E-Mail body text
    StringWriter contentBuffer = new StringWriter();
    Template template = FREEMARKER_CONFIG.getTemplate(templateFile);
    template.process(templateDataModel, contentBuffer);
    return new BaseEmailModel(emailAddress,
            //at some point we will add support for multiple languages
            emailManagerConfiguration.getDefaultEmailSubjects().getString(subjectKey),
            contentBuffer.toString());
  }

}
