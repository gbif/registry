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

import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DateConverterTest {

  private final DateConverter converter = new DateConverter();

  @Test
  public void parseDates() throws Throwable {
    assertLang(2000, 10, 20, 0, "2000-10-20");
    assertLang(2000, 10, 20, 0, "20.10.2000");
    assertLang(2000, 1, 1, 1, "2000");
  }

  private void assertLang(Integer year, Integer month, Integer day, Integer millis, String value) {
    Date d = (Date) converter.convertToType(Date.class, value);

    Calendar cal = Calendar.getInstance();
    cal.setTime(d);

    if (year != null) {
      assertEquals(year, (Integer) cal.get(Calendar.YEAR));
    }
    if (month != null) {
      assertEquals(month, (Integer) (1 + cal.get(Calendar.MONTH)));
    }
    if (day != null) {
      assertEquals(day, (Integer) cal.get(Calendar.DAY_OF_MONTH));
    }
    if (millis != null) {
      assertEquals(millis, (Integer) cal.get(Calendar.MILLISECOND));
    }
    if (year == null && month == null && day == null) {
      assertNull(d);
    }
  }
}
