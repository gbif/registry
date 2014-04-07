package org.gbif.registry.metadata.parse.converter;

import org.gbif.common.parsers.core.ParseResult;
import org.gbif.common.parsers.date.DateParseUtils;

import java.util.Date;

import org.apache.commons.beanutils.converters.AbstractConverter;

public class DateConverter extends AbstractConverter {

  public DateConverter() {

  }

  @Override
  protected Object convertToType(Class type, Object value) {
    ParseResult<Date> result = DateParseUtils.parse(value.toString());
    return result.getStatus() == ParseResult.STATUS.SUCCESS ? result.getPayload() : null;
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
