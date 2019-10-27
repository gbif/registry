package org.gbif.registry.doi.util;

import org.apache.commons.lang3.time.DateFormatUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public final class RegistryDoiUtils {

  private RegistryDoiUtils() {
  }

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
}
