package org.gbif.registry.metadata.parse.converter;

import org.gbif.api.vocabulary.Language;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LanguageTypeConverterTest {
  private LanguageTypeConverter converter = new LanguageTypeConverter();

  @Test
  public void parseLanguages() throws Throwable {
    assertLang(Language.AFRIKAANS, "af");
    assertLang(Language.AFRIKAANS, "AF");
    assertLang(Language.ENGLISH, "EN");
    assertLang(Language.ENGLISH, "en_US");
    assertLang(Language.ENGLISH, "en_UK");
    assertLang(Language.FRENCH, "french");
    assertLang(Language.FRENCH, "fra");
    assertLang(Language.FRENCH, "fre");
  }

  private void assertLang(Language expect, String value) throws Throwable {
    Language l = (Language) converter.convertToType(Language.class, value);
    if (expect == null) {
      assertNull(l);
    } else {
      assertEquals(expect, l);
    }
  }
}
