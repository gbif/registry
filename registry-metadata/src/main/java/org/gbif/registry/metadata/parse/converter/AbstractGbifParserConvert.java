package org.gbif.registry.metadata.parse.converter;

import org.gbif.api.model.common.InterpretedEnum;
import org.gbif.common.parsers.InterpretedEnumParser;
import org.gbif.common.parsers.ParseResult;

import org.apache.commons.beanutils.converters.AbstractConverter;

public class AbstractGbifParserConvert<T extends Enum<T>> extends AbstractConverter {
  private final InterpretedEnumParser<T> parser;
  private final Class<T> clazz;

  public AbstractGbifParserConvert(Class<T> clazz, InterpretedEnumParser<T> parser) {
    this.parser = parser;
    this.clazz = clazz;
  }

  public AbstractGbifParserConvert(Class<T> clazz, InterpretedEnumParser<T> parser, T defaultValue) {
    super(defaultValue);
    this.parser = parser;
    this.clazz = clazz;
  }

  @Override
  protected Object convertToType(Class type, Object value) throws Throwable {
    ParseResult<InterpretedEnum<String, T>> result = parser.parse(value.toString());
    return result.getStatus() == ParseResult.STATUS.SUCCESS ? result.getPayload().getInterpreted() : null;
  }

  @Override
  protected Class getDefaultType() {
    return clazz;
  }
}
