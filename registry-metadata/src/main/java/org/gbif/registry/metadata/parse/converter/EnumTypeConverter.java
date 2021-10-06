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
package org.gbif.registry.metadata.parse.converter;

import org.gbif.api.util.VocabularyUtils;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.converters.AbstractConverter;

/**
 * {@link org.apache.commons.beanutils.Converter} implementation that handles conversion to and from
 * java enumerations.
 */
public class EnumTypeConverter<T extends Enum<?>> extends AbstractConverter {

  private final Class<T> clazz;
  private final T defaultValue;

  private final Map<String, T> lookup = new HashMap<>();

  public EnumTypeConverter(Class<T> clazz, T defaultValue) {
    this.clazz = clazz;
    this.defaultValue = defaultValue;
  }

  public void addMappings(Map<String, T> mapping) {
    for (Map.Entry<String, T> entry : mapping.entrySet()) {
      lookup.put(entry.getKey().toLowerCase(), entry.getValue());
    }
  }

  /**
   * Return the default type this <code>Converter</code> handles.
   *
   * @return The default type this <code>Converter</code> handles.
   */
  @Override
  protected Class getDefaultType() {
    return clazz;
  }

  /**
   * Convert a PreservationMethodType or object into a String. Checks map with alternative values
   * for each PreservationMethodType before returning the default value.
   *
   * @param type Data type to which this value should be converted
   * @param value The input value to be converted
   * @return The converted value.
   * @throws Throwable if an error occurs converting to the specified type
   */
  @Override
  protected Object convertToType(Class type, Object value) throws Throwable {
    // never null, super class implements this as:
    final String val = value.toString();
    if (lookup.containsKey(val)) {
      return lookup.get(val);
    }
    // try regular enum values
    try {
      T eVal = VocabularyUtils.lookupEnum(val, clazz);
      return eVal == null ? defaultValue : eVal;
    } catch (IllegalArgumentException e) {
      // cant parse, return default
      return defaultValue;
    }
  }
}
