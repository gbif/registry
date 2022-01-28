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

import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.ContactType;

import org.apache.commons.beanutils.converters.AbstractConverter;

/**
 * {@link org.apache.commons.beanutils.Converter} implementation that handles conversion to and from
 * <b>InterpretedEnum(value, ContactType)</b> objects.
 */
public final class ContactTypeConverter extends AbstractConverter {
  private static final ContactType DEFAULT_CONTACT_TYPE =
      ContactType.ADMINISTRATIVE_POINT_OF_CONTACT;

  /**
   * Construct a <b>InterpretedEnum(value, ContactType)</b> <i>Converter</i> that throws a {@code
   * ConversionException} if an error occurs.
   */
  public ContactTypeConverter() {}

  /**
   * Construct a <b>InterpretedEnum<String, ContactType</b> <i>Converter</i> that returns a default
   * value if an error occurs.
   *
   * @param defaultValue The default value to be returned if the value to be converted is missing or
   *     an error occurs converting the value.
   */
  public ContactTypeConverter(Object defaultValue) {
    super(defaultValue);
  }

  /**
   * Return the default type this {@code Converter} handles.
   *
   * @return The default type this {@code Converter} handles.
   */
  @Override
  protected Class getDefaultType() {
    return ContactType.class;
  }

  /**
   * Convert String into an InterpretedEnum<String, ContactType. Checks map with alternative values
   * for each ContactType to get the interpreted ContactType.
   *
   * @param type Data type to which this value should be converted.
   * @param value The input value to be converted.
   * @return The converted InterpretedEnum with interpreted ContactType, or default ContactType if
   *     it could not be interpreted.
   * @throws Throwable if an error occurs converting to the specified type
   */
  @Override
  protected Object convertToType(Class type, Object value) throws Throwable {
    ContactType infer;

    if ("metadataProvider".equalsIgnoreCase(value.toString())) {
      infer = ContactType.METADATA_AUTHOR;
    } else {
      infer = VocabularyUtils.lookupEnum(value.toString(), ContactType.class);
    }

    return infer == null ? DEFAULT_CONTACT_TYPE : infer;
  }
}
