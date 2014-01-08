package org.gbif.registry.metadata.parse;

import org.gbif.api.vocabulary.Language;
import org.gbif.registry.metadata.parse.converter.GreedyUriConverter;
import org.gbif.registry.metadata.parse.converter.LanguageTypeConverter;

import java.net.URI;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.digester3.Digester;
import org.apache.commons.digester3.RuleSetBase;

/**
 * Digester rules to parse Dublin Core metadata documents together with a DatasetDelegator digester model.
 */
public class DublinCoreRuleSet extends RuleSetBase {

    public DublinCoreRuleSet( ) {
        super("http://purl.org/dc/terms/");
    }

  private void setupTypeConverters() {

    GreedyUriConverter uriConverter = new GreedyUriConverter();
    ConvertUtils.register(uriConverter, URI.class);

    LanguageTypeConverter langConverter = new LanguageTypeConverter();
    ConvertUtils.register(langConverter, Language.class);
  }

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
      digester.addCallMethod("*/isFormatOf", "addDataUrl", 0, new Class[]{URI.class});
      digester.addCallMethod("*/creator", "addCreator", 0);
      digester.addCallMethod("*/created", "setPubDateAsString", 0);
      digester.addBeanPropertySetter("*/rights", "rights");
      digester.addCallMethod("*/license", "addLicense", 0);
      digester.addCallMethod("*/bibliographicCitation", "addBibCitation", 0);
      digester.addCallMethod("*/identifier", "addIdentifier", 0);
    }
}