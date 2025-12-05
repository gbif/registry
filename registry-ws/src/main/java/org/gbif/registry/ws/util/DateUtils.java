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
package org.gbif.registry.ws.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.function.Function;

import com.google.common.base.Strings;

public class DateUtils {

  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern(
          "[yyyy-MM-dd'T'HH:mm:ssXXX][yyyy-MM-dd'T'HH:mmXXX][yyyy-MM-dd'T'HH:mm:ss.SSS XXX][yyyy-MM-dd'T'HH:mm:ss.SSSXXX]"
              + "[yyyy-MM-dd'T'HH:mm:ss.SSSSSS][yyyy-MM-dd'T'HH:mm:ss.SSSSS][yyyy-MM-dd'T'HH:mm:ss.SSSS][yyyy-MM-dd'T'HH:mm:ss.SSS]"
              + "[yyyy-MM-dd'T'HH:mm:ss][yyyy-MM-dd'T'HH:mm:ss XXX][yyyy-MM-dd'T'HH:mm:ssXXX][yyyy-MM-dd'T'HH:mm:ss]"
              + "[yyyy-MM-dd'T'HH:mm][yyyy-MM-dd][yyyy-MM][yyyy]");

  public static final Function<String, Date> STRING_TO_DATE =
      dateAsString -> {
        if (Strings.isNullOrEmpty(dateAsString)) {
          return null;
        }

        boolean firstYear = false;
        if (dateAsString.startsWith("0000")) {
          firstYear = true;
          dateAsString = dateAsString.replaceFirst("0000", "1970");
        }

        // parse string
        TemporalAccessor temporalAccessor =
            FORMATTER.parseBest(
                dateAsString,
                ZonedDateTime::from,
                LocalDateTime::from,
                LocalDate::from,
                YearMonth::from,
                Year::from);
        Date dateParsed = null;
        if (temporalAccessor instanceof ZonedDateTime) {
          dateParsed = Date.from(((ZonedDateTime) temporalAccessor).toInstant());
        } else if (temporalAccessor instanceof LocalDateTime) {
          dateParsed = Date.from(((LocalDateTime) temporalAccessor).toInstant(ZoneOffset.UTC));
        } else if (temporalAccessor instanceof LocalDate) {
          dateParsed =
              Date.from((((LocalDate) temporalAccessor).atStartOfDay()).toInstant(ZoneOffset.UTC));
        } else if (temporalAccessor instanceof YearMonth) {
          dateParsed =
              Date.from(
                  (((YearMonth) temporalAccessor).atDay(1))
                      .atStartOfDay()
                      .toInstant(ZoneOffset.UTC));
        } else if (temporalAccessor instanceof Year) {
          dateParsed =
              Date.from(
                  (((Year) temporalAccessor).atDay(1)).atStartOfDay().toInstant(ZoneOffset.UTC));
        }

        if (dateParsed != null && firstYear) {
          Calendar cal = Calendar.getInstance();
          cal.setTime(dateParsed);
          cal.set(Calendar.YEAR, 1);
          return cal.getTime();
        }

        return dateParsed;
      };

  public static final Function<String, LocalDateTime> LOWER_BOUND_RANGE_PARSER =
      lowerBound -> {
        if (Strings.isNullOrEmpty(lowerBound)) {
          return null;
        }

        // check if it has time and if so we use it
        try {
          return LocalDateTime.parse(lowerBound);
        } catch (DateTimeParseException ex) {
          // continue with the others parsers
        }

        TemporalAccessor temporalAccessor =
            FORMATTER.parseBest(lowerBound, LocalDate::from, YearMonth::from, Year::from);

        if (temporalAccessor instanceof LocalDate) {
          return ((LocalDate) temporalAccessor).atTime(LocalTime.MIN);
        }

        if (temporalAccessor instanceof Year) {
          return Year.from(temporalAccessor).atMonth(Month.JANUARY).atDay(1).atTime(LocalTime.MIN);
        }

        if (temporalAccessor instanceof YearMonth) {
          return YearMonth.from(temporalAccessor).atDay(1).atTime(LocalTime.MIN);
        }

        return null;
      };

  public static final Function<String, LocalDateTime> UPPER_BOUND_RANGE_PARSER =
      upperBound -> {
        if (Strings.isNullOrEmpty(upperBound)) {
          return null;
        }

        // check if it has time and if so we use it
        try {
          return LocalDateTime.parse(upperBound);
        } catch (DateTimeParseException ex) {
          // continue with the others parsers
        }

        TemporalAccessor temporalAccessor =
            FORMATTER.parseBest(upperBound, LocalDate::from, YearMonth::from, Year::from);

        if (temporalAccessor instanceof LocalDate) {
          return ((LocalDate) temporalAccessor).atTime(LocalTime.MAX);
        }

        if (temporalAccessor instanceof Year) {
          return Year.from(temporalAccessor)
              .atMonth(Month.DECEMBER)
              .atEndOfMonth()
              .atTime(LocalTime.MAX);
        }

        if (temporalAccessor instanceof YearMonth) {
          return YearMonth.from(temporalAccessor).atEndOfMonth().atTime(LocalTime.MAX);
        }

        return null;
      };

  public static final Function<String, OffsetDateTime> LOWER_BOUND_RANGE_PARSER_OFFSET =
      lowerBound -> {
        if (Strings.isNullOrEmpty(lowerBound)) {
          return null;
        }

        // Try to parse as OffsetDateTime first (preserves timezone if present)
        try {
          TemporalAccessor temporalAccessor =
              FORMATTER.parseBest(
                  lowerBound,
                  OffsetDateTime::from,
                  ZonedDateTime::from,
                  LocalDateTime::from,
                  LocalDate::from,
                  YearMonth::from,
                  Year::from);

          if (temporalAccessor instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
          } else if (temporalAccessor instanceof ZonedDateTime zonedDateTime) {
            return  zonedDateTime.toOffsetDateTime();
          } else if (temporalAccessor instanceof LocalDateTime localDateTime) {
            return localDateTime.atOffset(ZoneOffset.UTC);
          } else if (temporalAccessor instanceof LocalDate localDate) {
            return localDate.atTime(LocalTime.MIN).atOffset(ZoneOffset.UTC);
          } else if (temporalAccessor instanceof Year year) {
            return Year.from(year)
                .atMonth(Month.JANUARY)
                .atDay(1)
                .atTime(LocalTime.MIN)
                .atOffset(ZoneOffset.UTC);
          } else if (temporalAccessor instanceof YearMonth yearMonth) {
            return YearMonth.from(yearMonth)
                .atDay(1)
                .atTime(LocalTime.MIN)
                .atOffset(ZoneOffset.UTC);
          }
        } catch (DateTimeParseException ex) {
          // Fall back to LocalDateTime parser if FORMATTER fails
        }

        // Fallback to existing LocalDateTime parser and convert to UTC
        LocalDateTime localDateTime = LOWER_BOUND_RANGE_PARSER.apply(lowerBound);
        return localDateTime != null ? localDateTime.atOffset(ZoneOffset.UTC) : null;
      };

  public static final Function<String, OffsetDateTime> UPPER_BOUND_RANGE_PARSER_OFFSET =
      upperBound -> {
        if (Strings.isNullOrEmpty(upperBound)) {
          return null;
        }

        // Try to parse as OffsetDateTime first (preserves timezone if present)
        try {
          TemporalAccessor temporalAccessor =
              FORMATTER.parseBest(
                  upperBound,
                  OffsetDateTime::from,
                  ZonedDateTime::from,
                  LocalDateTime::from,
                  LocalDate::from,
                  YearMonth::from,
                  Year::from);

          if (temporalAccessor instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
          } else if (temporalAccessor instanceof ZonedDateTime zonedDateTime) {
            return zonedDateTime.toOffsetDateTime();
          } else if (temporalAccessor instanceof LocalDateTime localDateTime) {
            return localDateTime.atOffset(ZoneOffset.UTC);
          } else if (temporalAccessor instanceof LocalDate localDate) {
            return localDate.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);
          } else if (temporalAccessor instanceof Year year) {
            return Year.from(year)
                .atMonth(Month.DECEMBER)
                .atEndOfMonth()
                .atTime(LocalTime.MAX)
                .atOffset(ZoneOffset.UTC);
          } else if (temporalAccessor instanceof YearMonth yearMonth) {
            return YearMonth.from(yearMonth)
                .atEndOfMonth()
                .atTime(LocalTime.MAX)
                .atOffset(ZoneOffset.UTC);
          }
        } catch (DateTimeParseException ex) {
          // Fall back to LocalDateTime parser if FORMATTER fails
        }

        // Fallback to existing LocalDateTime parser and convert to UTC
        LocalDateTime localDateTime = UPPER_BOUND_RANGE_PARSER.apply(upperBound);
        return localDateTime != null ? localDateTime.atOffset(ZoneOffset.UTC) : null;
      };
}
