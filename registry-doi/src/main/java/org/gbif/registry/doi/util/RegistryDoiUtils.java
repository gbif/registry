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
package org.gbif.registry.doi.util;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.vocabulary.IdentifierType;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

import org.apache.commons.lang3.time.DateFormatUtils;

public final class RegistryDoiUtils {

  private RegistryDoiUtils() {}

  public static String fdate(Date date) {
    return DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT.format(date);
  }

  public static String getYear(Date date) {
    if (date == null) {
      return null;
    }
    Calendar cal = new GregorianCalendar();
    cal.setTime(date);
    return String.valueOf(cal.get(Calendar.YEAR));
  }

  /**
   * Checks if a DOI can be found in the list of dataset identifiers.
   */
  public static boolean isIdentifierDOIFound(DOI doi, Identifiable identifiable) {
    return identifiable.getIdentifiers().stream()
        .anyMatch(
            identifier ->
                IdentifierType.DOI == identifier.getType()
                    && identifier.getIdentifier().equals(doi.toString()));
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public static boolean isUuid(String str) {
    try {
      UUID.fromString(str);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
