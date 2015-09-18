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
class DatasetXMLWriterConfigurationProvider {

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

    // Using deprecated constructor
    // From Freemarker documentation:
    // Configuration cfg = new Configuration(VERSION_X_Y_Z));
    // Where X, Y, Z enables the not-100%-backward-compatible fixes introduced in
    // FreeMarker version X.Y.Z  and earlier (see Configuration(Version)).
    // To be safe, we do not use any of the new features but it would be useful to have a minimum version
    // of Freemarker to use.
    Configuration fm = new Configuration();

    fm.setDefaultEncoding(StandardCharsets.UTF_8.toString());
    fm.setTemplateLoader(tl);

    return fm;
  }
}
