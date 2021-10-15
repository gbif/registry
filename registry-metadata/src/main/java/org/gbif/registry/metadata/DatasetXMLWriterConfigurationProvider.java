/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.metadata;

import java.nio.charset.StandardCharsets;
import java.text.ChoiceFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateModelException;

/**
 * Configuration provider for XML based dataset writer. Currently providing Freemarker
 * configuration.
 */
public class DatasetXMLWriterConfigurationProvider {

  public static final String FM_UTILS_NAME = "fmUtil";
  private static final String TEMPLATE_PATH = "/";
  private static final Logger LOG =
      LoggerFactory.getLogger(DatasetXMLWriterConfigurationProvider.class);

  /**
   * Provides a Freemarker template loader. It is configured to access the UTF-8 templates folder on
   * the classpath.
   */
  public static Configuration provideFreemarker() {
    return provideFreemarker(TEMPLATE_PATH);
  }

  /**
   * Provides a Freemarker template loader. It is configured to access the UTF-8 templates folder on
   * the classpath.
   *
   * @param templatePath Freemarker base package path
   */
  public static Configuration provideFreemarker(String templatePath) {
    // load templates from classpath by prefixing /templates
    TemplateLoader tl =
        new ClassTemplateLoader(DatasetXMLWriterConfigurationProvider.class, templatePath);

    // Using deprecated constructor
    // From Freemarker documentation:
    // Configuration cfg = new Configuration(VERSION_X_Y_Z));
    // Where X, Y, Z enables the not-100%-backward-compatible fixes introduced in
    // FreeMarker version X.Y.Z  and earlier (see Configuration(Version)).
    // To be safe, we do not use any of the new features but it would be useful to have a minimum
    // version
    // of Freemarker to use.
    Configuration fm = new Configuration();

    fm.setDefaultEncoding(StandardCharsets.UTF_8.toString());
    fm.setTemplateLoader(tl);
    try {
      fm.setSharedVariable(FM_UTILS_NAME, new FreemarkerUtils());
    } catch (TemplateModelException e) {
      LOG.error("Can't set sharedVariable in Freemarker Configuration", e);
    }
    return fm;
  }

  /**
   * Nested class to expose some Java utilities to Freemarker template. This class requires to be
   * public to be visible by the Freemarker template.
   */
  public static final class FreemarkerUtils {

    /**
     * Allows to return a different string based on the number provided. Useful for pluralization or
     * words that depends on a variable in templates
     *
     * @param pattern "-1#is negative| 0#is zero or fraction | 1#is one |1.0<is 1+ |2#is two |2<is
     *     more than 2."
     * @param number
     * @see ChoiceFormat for patterns
     */
    public String choiceFormat(String pattern, double number) {
      ChoiceFormat fmt = new ChoiceFormat(pattern);
      return fmt.format(number);
    }
  }
}
