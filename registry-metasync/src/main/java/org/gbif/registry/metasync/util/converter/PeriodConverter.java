package org.gbif.registry.metasync.util.converter;

import org.apache.commons.beanutils.Converter;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;
import org.joda.time.format.PeriodFormatter;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Used by commons-digester (via commons-beanutils) to convert Strings into {@link Period}s from Joda Time.
 */
public class PeriodConverter implements Converter {

  private final PeriodFormatter formatter = ISOPeriodFormat.standard();

  @Override
  public Object convert(Class type, Object value) {
    checkNotNull(type, "type cannot be null");
    checkNotNull(value, "Value cannot be null");
    checkArgument(type.equals(Period.class),
                  "Conversion target should be org.joda.time.Duration, but is %s",
                  type.getClass());
    checkArgument(String.class.isAssignableFrom(value.getClass()),
                  "Value should be a string, but is a %s",
                  value.getClass());

    return formatter.parsePeriod((String) value);
  }

}
