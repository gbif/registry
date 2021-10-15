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
package org.gbif.registry.metadata.parse.converter;

import org.gbif.api.vocabulary.Language;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LanguageTypeConverterTest {

  private final LanguageTypeConverter converter = new LanguageTypeConverter();

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
