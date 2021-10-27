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
package org.gbif.registry.metasync.util.converter;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DateTimeConverterTest {

  DateTimeConverter converter = new DateTimeConverter();

  /**
   * Test ISO 8691 formats.
   *
   * <p>See https://en.wikipedia.org/wiki/ISO_8601 for an overview.
   */
  @Test
  public void testIso8691Dates() {
    assertEquals(
        new DateTime().withDate(2016, 1, 27).withTimeAtStartOfDay(),
        converter.convert(DateTime.class, "2016-01-27"));
    assertEquals(
        new DateTime().withDate(2016, 1, 25).withTimeAtStartOfDay(),
        converter.convert(DateTime.class, "2016-W04"));
    assertEquals(
        new DateTime().withDate(2016, 1, 27).withTimeAtStartOfDay(),
        converter.convert(DateTime.class, "2016-W04-3"));
    assertEquals(
        new DateTime().withDate(2016, 1, 27).withTimeAtStartOfDay(),
        converter.convert(DateTime.class, "2016-027"));
    assertEquals(
        new DateTime().withDate(2016, 1, 27).withTimeAtStartOfDay(),
        converter.convert(DateTime.class, "20160127"));

    assertEquals(
        new DateTime()
            .withZone(DateTimeZone.UTC)
            .withDate(2016, 1, 27)
            .withTime(6, 11, 22, 0)
            .toDate(),
        ((DateTime) converter.convert(DateTime.class, "2016-01-27T06:11:22+00:00")).toDate());
    assertEquals(
        new DateTime()
            .withZone(DateTimeZone.UTC)
            .withDate(2016, 1, 27)
            .withTime(6, 11, 22, 0)
            .toDate(),
        ((DateTime) converter.convert(DateTime.class, "2016-01-27T06:11:22Z")).toDate());
    assertEquals(
        new DateTime()
            .withZone(DateTimeZone.UTC)
            .withDate(2016, 1, 27)
            .withTime(6, 11, 22, 0)
            .toDate(),
        ((DateTime) converter.convert(DateTime.class, "20160127T061122Z")).toDate());

    assertEquals(
        new DateTime()
            .withZone(DateTimeZone.UTC)
            .withDate(2016, 1, 27)
            .withTime(6, 11, 22, 0)
            .toDate(),
        ((DateTime) converter.convert(DateTime.class, "2016-01-27T09:11:22+03:00")).toDate());
    assertEquals(
        new DateTime()
            .withZone(DateTimeZone.UTC)
            .withDate(2016, 1, 27)
            .withTime(6, 11, 22, 0)
            .toDate(),
        ((DateTime) converter.convert(DateTime.class, "2016-01-27T03:11:22-03:00")).toDate());
    assertEquals(
        new DateTime()
            .withZone(DateTimeZone.UTC)
            .withDate(2016, 1, 27)
            .withTime(6, 11, 22, 0)
            .toDate(),
        ((DateTime) converter.convert(DateTime.class, "2016-01-27T03:11:22−03:00")).toDate());

    assertEquals(
        new DateTime()
            .withZone(DateTimeZone.UTC)
            .withDate(2016, 1, 27)
            .withTime(6, 11, 22, 0)
            .toDate(),
        ((DateTime) converter.convert(DateTime.class, "20160127T061122Z")).toDate());
  }

  /** Test some invalid formats, but which we are still provided with. */
  @Test
  public void testInvalidDates() {
    assertEquals(
        new DateTime().withDate(2016, 1, 27).withTimeAtStartOfDay(),
        converter.convert(DateTime.class, "2016/01/27"));
    assertEquals(
        new DateTime()
            .withZone(DateTimeZone.UTC)
            .withDate(2016, 1, 27)
            .withTime(6, 11, 22, 0)
            .toDate(),
        ((DateTime) converter.convert(DateTime.class, "2016-01-27 06:11:22")).toDate());
    assertEquals(
        new DateTime()
            .withZone(DateTimeZone.UTC)
            .withDate(2016, 1, 27)
            .withTime(6, 11, 22, 0)
            .toDate(),
        ((DateTime) converter.convert(DateTime.class, "2016-01-27 09:11:22+0300")).toDate());
    assertEquals(
        new DateTime().withDate(2016, 1, 27).withTimeAtStartOfDay(),
        converter.convert(DateTime.class, "2016-01-27TCentral Sta:ndard Time"));
  }

  /** Test some partial formats. */
  @Test
  public void testPartialDates() {
    assertEquals(
        new DateTime().withDate(2016, 1, 1).withTimeAtStartOfDay(),
        converter.convert(DateTime.class, "2016-01"));
    assertEquals(
        new DateTime().withDate(2016, 1, 1).withTimeAtStartOfDay(),
        converter.convert(DateTime.class, "2016"));

    assertEquals(
        new DateTime().withDate(2016, 1, 1).withTimeAtStartOfDay(),
        converter.convert(DateTime.class, "2016/01"));

    assertNull(converter.convert(DateTime.class, ":"));
    assertNull(converter.convert(DateTime.class, ""));
    assertNull(converter.convert(DateTime.class, "ABCDEFGHIJKM"));
    assertNull(converter.convert(DateTime.class, "ABCD"));
  }
}
