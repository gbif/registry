package org.gbif.registry.metadata.parse.converter;

import org.gbif.common.parsers.core.EnumParser;
import org.gbif.common.parsers.core.ParseResult;

import org.apache.commons.beanutils.converters.AbstractConverter;

public class AbstractGbifParserConvert<T extends Enum<T>> extends AbstractConverter {
  private final EnumParser<T> parser;
  private final Class<T> clazz;

  public AbstractGbifParserConvert(Class<T> clazz, EnumParser<T> parser) {
    this.parser = parser;
    this.clazz = clazz;
  }

  public AbstractGbifParserConvert(Class<T> clazz, EnumParser<T> parser, T defaultValue) {
    super(defaultValue);
    this.parser = parser;
    this.clazz = clazz;
  }

  @Override
  protected Object convertToType(Class type, Object value) throws Throwable {
    ParseResult<T> result = parser.parse(value.toString());
    return result.getStatus() == ParseResult.STATUS.SUCCESS ? result.getPayload() : null;
  }

  @Override
  protected Class getDefaultType() {
    return clazz;
  }
}
