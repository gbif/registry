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
import org.gbif.common.parsers.LanguageParser;

/**
 * {@link org.apache.commons.beanutils.Converter} implementation that handles conversion to and from
 * <b>Language</b> ENUM objects.
 */
public class LanguageTypeConverter extends AbstractGbifParserConvert<Language> {

  /** Construct a <b>LanguageTypeConverter</b> <i>Converter</i>. */
  public LanguageTypeConverter() {
    super(Language.class, LanguageParser.getInstance());
  }
}
