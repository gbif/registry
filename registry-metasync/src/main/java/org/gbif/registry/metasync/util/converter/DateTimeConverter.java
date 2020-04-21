/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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

import org.apache.commons.beanutils.Converter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Used by commons-digester (via commons-beanutils) to convert Strings into {@link DateTime}s from
 * Joda Time assuming that all those Strings are in ISO 8601 form but where the time is optional.
 *
 * <p>Allows either ISO 8601 extended format (e.g. 2016-01-27T13:38:30+01:00) or basic format (e.g.
 * 20160127T133830+0100) plus some degenerate formats observed in use (2016/01/27, a space instead
 * of T, etc).
 */
public class DateTimeConverter implements Converter {

  private static final Logger LOG = LoggerFactory.getLogger(DateTimeConverter.class);
  private final DateTimeFormatter formatter = ISODateTimeFormat.dateOptionalTimeParser();
  private final DateTimeFormatter basicFormatter = basicDateOptionalTime();

  static {
    DateTimeZone.setDefault(DateTimeZone.UTC);
  }

  @Override
  public Object convert(Class type, Object value) {
    checkNotNull(type, "type cannot be null");
    checkNotNull(value, "Value cannot be null");
    checkArgument(
        type.equals(DateTime.class),
        "Conversion target should be org.joda.time.DateTime, but is %s",
        type);
    checkArgument(
        String.class.isAssignableFrom(value.getClass()),
        "Value should be a string, but is a %s",
        value.getClass());

    DateTime dateTime = null;
    String valueString =
        ((String) value)
            .replace('/', '-')
            .replace(
                '−',
                '-') // ISO 8601 actually specifies a Unicode minus, U+2212, with a hyphen as an
            // alternative.
            .replace(' ', 'T')
            .trim();

    try {
      dateTime = basicFormatter.parseDateTime(valueString);
    } catch (IllegalArgumentException e1) {
      try {
        dateTime = formatter.parseDateTime(valueString);
      } catch (IllegalArgumentException e2) {
        try {
          dateTime =
              formatter.parseDateTime(
                  (valueString.length() > 10) ? valueString.substring(0, 10) : valueString);
        } catch (IllegalArgumentException e3) {
          LOG.debug("Could not parse date: [{}]", value, e3);
        }
      }
    }
    return dateTime;
  }

  private static DateTimeFormatter basicTime() {
    return new DateTimeFormatterBuilder()
        .appendLiteral('T')
        .appendFixedDecimal(DateTimeFieldType.hourOfDay(), 2)
        .appendFixedDecimal(DateTimeFieldType.minuteOfHour(), 2)
        .appendFixedDecimal(DateTimeFieldType.secondOfMinute(), 2)
        .toFormatter();
  }

  private static DateTimeFormatter basicOffset() {
    return new DateTimeFormatterBuilder().appendTimeZoneOffset("Z", false, 2, 2).toFormatter();
  }

  private static DateTimeFormatter basicDateOptionalTime() {
    DateTimeFormatter timeOrOffset =
        new DateTimeFormatterBuilder()
            .append(ISODateTimeFormat.basicDate().getParser())
            .appendOptional(basicTime().getParser())
            .appendOptional(basicOffset().getParser())
            .toFormatter();

    return timeOrOffset;
  }
}
