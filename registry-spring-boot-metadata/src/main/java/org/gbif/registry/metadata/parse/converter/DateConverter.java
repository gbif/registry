package org.gbif.registry.metadata.parse.converter;

import org.apache.commons.beanutils.converters.AbstractConverter;
import org.gbif.common.parsers.core.ParseResult;
import org.gbif.common.parsers.date.DateParsers;

import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

public class DateConverter extends AbstractConverter {

  /**
   * Parse a date string.
   *
   * Add one millisecond to dates that are only years, to retain API compatibility with a Date object, but allow
   * us to remember this.
   */
  @Override
  protected Object convertToType(Class type, Object value) {
    ParseResult<TemporalAccessor> result = DateParsers.defaultNumericalDateParser().parse(value.toString());
    if (result.getStatus() == ParseResult.STATUS.SUCCESS) {
      TemporalAccessor ta = result.getPayload();
      if (ta.isSupported(ChronoField.DAY_OF_MONTH)) {
        return Date.from(LocalDate.from(ta).atStartOfDay(ZoneOffset.UTC).toInstant());
      } else if (ta.isSupported(ChronoField.YEAR)) {
        return Date.from(Year.from(ta).atDay(1).atStartOfDay(ZoneOffset.UTC).plusNanos(1_000_000).toInstant());
      }
    }

    return null;
  }

  /**
   * Return the default type this {@code Converter} handles.
   *
   * @return The default type this {@code Converter} handles.
   */
  @Override
  protected Class getDefaultType() {
    return Date.class;
  }
}
