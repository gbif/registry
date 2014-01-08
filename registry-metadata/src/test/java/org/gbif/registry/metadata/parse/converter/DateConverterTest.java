package org.gbif.registry.metadata.parse.converter;

import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DateConverterTest {
  private DateConverter converter = new DateConverter();

  @Test
  public void parseLanguages() throws Throwable {
    assertLang(2000, 10, 20, "2000-20-10");
    assertLang(2000, 10, 20, "20.10.2000");
  }

  private void assertLang(Integer year, Integer month, Integer day, String value) throws Throwable {
    Date d = (Date) converter.convertToType(Date.class, value);
    System.out.println(d);

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
    if (year == null && month == null && day==null) {
      assertNull(d);
    }
  }
}
