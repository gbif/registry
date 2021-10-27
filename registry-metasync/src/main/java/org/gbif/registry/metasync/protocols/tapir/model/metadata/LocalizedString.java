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
package org.gbif.registry.metasync.protocols.tapir.model.metadata;

import org.gbif.api.vocabulary.Language;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

public class LocalizedString {

  private final Map<Language, String> values = Maps.newHashMap();

  public void addValue(Language language, String value) {
    values.put(language, value);
  }

  public Map<Language, String> getValues() {
    return values;
  }

  @Override
  public String toString() {
    if (values.isEmpty()) {
      return "";
    }

    if (values.containsKey(Language.ENGLISH)) {
      return values.get(Language.ENGLISH);
    }

    List<Language> languages = new ArrayList<Language>(values.keySet());
    return values.get(languages.get(0));
  }
}
