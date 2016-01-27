package org.gbif.registry.metasync.util.converter;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DateTimeConverterTest {

  DateTimeConverter converter = new DateTimeConverter();

  /**
   * Test ISO 8691 formats.
   *
   * See https://en.wikipedia.org/wiki/ISO_8601 for an overview.
   */
  @Test
  public void testIso8691Dates() {
    assertEquals(new DateTime().withDate(2016, 1, 27).withTimeAtStartOfDay(), converter.convert(DateTime.class, "2016-01-27"));
    assertEquals(new DateTime().withDate(2016, 1, 25).withTimeAtStartOfDay(), converter.convert(DateTime.class, "2016-W04"));
    assertEquals(new DateTime().withDate(2016, 1, 27).withTimeAtStartOfDay(), converter.convert(DateTime.class, "2016-W04-3"));
    assertEquals(new DateTime().withDate(2016, 1, 27).withTimeAtStartOfDay(), converter.convert(DateTime.class, "2016-027"));
    assertEquals(new DateTime().withDate(2016, 1, 27).withTimeAtStartOfDay(), converter.convert(DateTime.class, "20160127"));

    assertEquals(new DateTime().withZone(DateTimeZone.UTC).withDate(2016, 1, 27).withTime(06, 11, 22, 0).toDate(), ((DateTime) converter.convert(DateTime.class, "2016-01-27T06:11:22+00:00")).toDate());
    assertEquals(new DateTime().withZone(DateTimeZone.UTC).withDate(2016, 1, 27).withTime(06, 11, 22, 0).toDate(), ((DateTime) converter.convert(DateTime.class, "2016-01-27T06:11:22Z")).toDate());
    assertEquals(new DateTime().withZone(DateTimeZone.UTC).withDate(2016, 1, 27).withTime(06, 11, 22, 0).toDate(), ((DateTime) converter.convert(DateTime.class, "20160127T061122Z")).toDate());

    assertEquals(new DateTime().withZone(DateTimeZone.UTC).withDate(2016, 1, 27).withTime(06, 11, 22, 0).toDate(), ((DateTime) converter.convert(DateTime.class, "2016-01-27T09:11:22+03:00")).toDate());
    assertEquals(new DateTime().withZone(DateTimeZone.UTC).withDate(2016, 1, 27).withTime(06, 11, 22, 0).toDate(), ((DateTime) converter.convert(DateTime.class, "2016-01-27T03:11:22-03:00")).toDate());
    assertEquals(new DateTime().withZone(DateTimeZone.UTC).withDate(2016, 1, 27).withTime(06, 11, 22, 0).toDate(), ((DateTime) converter.convert(DateTime.class, "2016-01-27T03:11:22−03:00")).toDate());

    assertEquals(new DateTime().withZone(DateTimeZone.UTC).withDate(2016, 1, 27).withTime(06, 11, 22, 0).toDate(), ((DateTime) converter.convert(DateTime.class, "20160127T061122Z")).toDate());
  }

  /**
   * Test some invalid formats, but which we are still provided with.
   */
  @Test
  public void testInvalidDates() {
    assertEquals(new DateTime().withDate(2016, 1, 27).withTimeAtStartOfDay(), converter.convert(DateTime.class, "2016/01/27"));
    assertEquals(new DateTime().withZone(DateTimeZone.UTC).withDate(2016, 1, 27).withTime(06, 11, 22, 0).toDate(), ((DateTime) converter.convert(DateTime.class, "2016-01-27 06:11:22")).toDate());
    assertEquals(new DateTime().withZone(DateTimeZone.UTC).withDate(2016, 1, 27).withTime(06, 11, 22, 0).toDate(), ((DateTime) converter.convert(DateTime.class, "2016-01-27 09:11:22+0300")).toDate());
    assertEquals(new DateTime().withDate(2016, 1, 27).withTimeAtStartOfDay(), converter.convert(DateTime.class, "2016-01-27TCentral Sta:ndard Time"));
  }
}
