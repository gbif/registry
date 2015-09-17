package org.gbif.registry.metadata;

import java.nio.charset.StandardCharsets;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;

/**
 * Configuration provider for XML based dataset writer. Currently providing Freemarker configuration.
 *
 * @author cgendreau
 */
public class DatasetXMLWriterConfigurationProvider {

  private static final String TEMPLATE_PATH = "/";

  /**
   * Provides a Freemarker template loader. It is configured to access the UTF-8 templates folder on the classpath.
   */
  public static Configuration provideFreemarker() {
    return provideFreemarker(TEMPLATE_PATH);
  }

  /**
   * Provides a Freemarker template loader. It is configured to access the UTF-8 templates folder on the classpath.
   * @param templatePath Freemarker base package path
   */
  public static Configuration provideFreemarker(String templatePath) {
    // load templates from classpath by prefixing /templates
    TemplateLoader tl = new ClassTemplateLoader(DatasetXMLWriterConfigurationProvider.class, templatePath);

    Configuration fm = new Configuration();

    fm.setDefaultEncoding(StandardCharsets.UTF_8.toString());
    fm.setTemplateLoader(tl);

    return fm;
  }
}
