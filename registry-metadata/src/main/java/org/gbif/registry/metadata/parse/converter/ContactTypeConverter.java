package org.gbif.registry.metadata.parse.converter;


import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.ContactType;

import org.apache.commons.beanutils.converters.AbstractConverter;

/**
 * {@link org.apache.commons.beanutils.Converter} implementation that handles conversion
 * to and from <b>InterpretedEnum(value, ContactType)</b> objects.
 */
public final class ContactTypeConverter extends AbstractConverter {
  private static final ContactType DEFAULT_CONTACT_TYPE = ContactType.ADMINISTRATIVE_POINT_OF_CONTACT;

  /**
   * Construct a <b>InterpretedEnum(value, ContactType)</b> <i>Converter</i> that throws
   * a {@code ConversionException} if an error occurs.
   */
  public ContactTypeConverter() {
  }

  /**
   * Construct a <b>InterpretedEnum<String, ContactType</b> <i>Converter</i> that returns
   * a default value if an error occurs.
   *
   * @param defaultValue The default value to be returned
   *                     if the value to be converted is missing or an error
   *                     occurs converting the value.
   */
  public ContactTypeConverter(Object defaultValue) {
    super(defaultValue);
  }

  /**
   * Return the default type this {@code Converter} handles.
   *
   * @return The default type this {@code Converter} handles.
   */
  protected Class getDefaultType() {
    return ContactType.class;
  }

  /**
   * <p>Convert String into an InterpretedEnum<String, ContactType.</p> Checks map with alternative values for each
   * ContactType to get the interpreted ContactType.
   *
   * @param type  Data type to which this value should be converted.
   * @param value The input value to be converted.
   *
   * @return The converted InterpretedEnum with interpreted ContactType, or default ContactType if it could not be
   *         interpreted.
   *
   * @throws Throwable if an error occurs converting to the specified type
   */
  protected Object convertToType(Class type, Object value) throws Throwable {

    ContactType infer = (ContactType) VocabularyUtils.lookupEnum(value.toString(), ContactType.class);
    return infer == null ? DEFAULT_CONTACT_TYPE : infer;
  }
}


