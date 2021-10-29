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
package org.gbif.registry.metadata.parse.converter;

import org.gbif.api.vocabulary.IdentifierType;

import org.apache.commons.beanutils.converters.AbstractConverter;

public class IdentifierTypeConverter extends AbstractConverter {

  /** Construct a <b>IdentifierType</b> <i>Converter</i> that defaults to IdentifierType.UNKNOWN. */
  public IdentifierTypeConverter() {}

  /**
   * Construct a <b>IdentifierType</b> <i>Converter</i> that returns a default value
   * IdentifierType.UNKNOWN if no type could be determined.
   *
   * @param defaultValue The default value to be returned if the value to be converted is missing or
   *     an error occurs converting the value.
   */
  public IdentifierTypeConverter(Object defaultValue) {
    super(defaultValue);
  }

  /**
   * Convert a String into a IdentifierType. The IdentifierType is determined from the String
   * identifier. For example, where the identifier begins with "doi:10" or "http://dx.doi.org/10."
   * the IdentifierType = DOI.
   *
   * @param type Data type to which this value should be converted.
   * @param value The input value to be converted.
   * @return The determined IdentifierType, or IdentifierType.UNKNOWN if an exception occurred or
   *     the type could not be guessed
   */
  @Override
  protected Object convertToType(Class type, Object value) {
    return IdentifierType.inferFrom(value.toString());
  }

  /**
   * Return the default type this {@code Converter} handles.
   *
   * @return The default type this {@code Converter} handles.
   */
  @Override
  protected Class getDefaultType() {
    return IdentifierType.class;
  }
}
