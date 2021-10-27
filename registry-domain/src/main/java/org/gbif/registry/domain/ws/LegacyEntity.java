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
package org.gbif.registry.domain.ws;

import javax.annotation.Nullable;

import com.google.common.base.Strings;

public interface LegacyEntity {

  /**
   * Checks if a field consists of the minimum number of characters. If it contains fewer characters
   * than required, a warning text is appended to the field text, padding it until it exceeds the
   * minimum required size.
   *
   * @param text field text
   * @param size minimum field size (in number of characters)
   * @return validated field meeting the minimum required length, or the original null or empty
   *     string
   */
  default String validateField(@Nullable String text, int size) {
    String validated = Strings.emptyToNull(text);
    if (validated != null) {
      StringBuilder sb = new StringBuilder(validated);
      while (sb.length() < size) {
        sb.append(" [Field must be at least ");
        sb.append(size);
        sb.append(" characters long]");
      }
      return sb.toString();
    }
    return text;
  }
}
