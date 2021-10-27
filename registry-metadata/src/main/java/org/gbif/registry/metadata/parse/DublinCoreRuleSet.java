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
package org.gbif.registry.metadata.parse;

import org.gbif.api.vocabulary.Language;
import org.gbif.registry.metadata.parse.converter.GreedyUriConverter;
import org.gbif.registry.metadata.parse.converter.LanguageTypeConverter;

import java.net.URI;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.digester3.Digester;
import org.apache.commons.digester3.RuleSetBase;

/**
 * Digester rules to parse Dublin Core metadata documents together with a DatasetDelegator digester
 * model.
 */
public class DublinCoreRuleSet extends RuleSetBase {

  public DublinCoreRuleSet() {
    super("http://purl.org/dc/terms/");
  }

  private void setupTypeConverters() {

    GreedyUriConverter uriConverter = new GreedyUriConverter();
    ConvertUtils.register(uriConverter, URI.class);

    LanguageTypeConverter langConverter = new LanguageTypeConverter();
    ConvertUtils.register(langConverter, Language.class);
  }

  @Override
  public void addRuleInstances(Digester digester) {
    setupTypeConverters();

    // add the rules
    digester.addCallMethod("*/protocol", "throwIllegalArgumentException");
    digester.addBeanPropertySetter("*/title", "title");
    digester.addCallMethod("*/abstract", "addAbstract", 0);
    digester.addBeanPropertySetter("*/description", "description");
    digester.addCallMethod("*/subject", "addSubjects", 0);
    digester.addBeanPropertySetter("*/language", "dataLanguage");
    digester.addBeanPropertySetter("*/source", "homepage");
    digester.addCallMethod("*/isFormatOf", "addDataUrl", 0, new Class[] {URI.class});
    digester.addCallMethod("*/creator", "addCreator", 0);
    digester.addCallMethod("*/created", "setPubDateAsString", 0);

    // License parsed from rights?
    digester.addCallMethod("*/rights", "setLicense", 2);
    digester.addCallParam("*/rights", 1);

    // License parsed from license?
    digester.addCallMethod("*/license", "setLicense", 2);
    digester.addCallParam("*/license", 0);

    digester.addCallMethod("*/bibliographicCitation", "addBibCitation", 0);
    digester.addCallMethod("*/identifier", "addIdentifier", 0);
  }
}
