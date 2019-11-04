package org.gbif.registry.metadata.parse.converter;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DateConverterTest {
  private DateConverter converter = new DateConverter();

  @Test
  public void parseDates() throws Throwable {
    assertLang(2000, 10, 20, 0, "2000-10-20");
    assertLang(2000, 10, 20, 0, "20.10.2000");
    assertLang(2000, 1, 1, 1, "2000");
  }

  private void assertLang(Integer year, Integer month, Integer day, Integer millis, String value) throws Throwable {
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
