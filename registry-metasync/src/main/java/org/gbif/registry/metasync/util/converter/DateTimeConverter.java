/*
 * Copyright 2013 Global Biodiversity Information Facility (GBIF)
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
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Used by commons-digester (via commons-beanutils) to convert Strings into {@link DateTime}s from Joda Time assuming
 * that all those Strings are in ISO 8601 form but where the time is optional.
 */
public class DateTimeConverter implements Converter {

  private static final Logger LOG = LoggerFactory.getLogger(DateTimeConverter.class);
  private final DateTimeFormatter formatter = ISODateTimeFormat.dateOptionalTimeParser();

  @Override
  public Object convert(Class type, Object value) {
    checkNotNull(type, "type cannot be null");
    checkNotNull(value, "Value cannot be null");
    checkArgument(type.equals(DateTime.class),
                  "Conversion target should be org.joda.time.DateTime, but is %s",
                  type.getClass());
    checkArgument(String.class.isAssignableFrom(value.getClass()),
                  "Value should be a string, but is a %s",
                  value.getClass());
    DateTime dateTime = null;
    try {
      dateTime = formatter.parseDateTime((String) value);
    } catch (IllegalArgumentException e) {
      LOG.debug("Could not parse date: [{}]", value, e);
    }
    return dateTime;
  }

}
