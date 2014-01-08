package org.gbif.registry.metadata.parse.converter;

import org.gbif.api.vocabulary.PreservationMethodType;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.beanutils.converters.AbstractConverter;

/**
 * {@link org.apache.commons.beanutils.Converter} implementation that handles conversion
 * to and from <b>org.gbif.registry.api.model.vocabulary.PreservationMethodType</b> objects.
 */
public class PreservationMethodTypeConverter extends AbstractConverter {

  private static final PreservationMethodType DEFAULT_TYPE = PreservationMethodType.OTHER;

  public static final Map<String, PreservationMethodType> LOOKUP = ImmutableMap.<String, PreservationMethodType>builder()
    .put("noTreatment", PreservationMethodType.NO_TREATMENT)
    .put("alcohol", PreservationMethodType.ALCOHOL)
    .put("deepFrozen", PreservationMethodType.DEEP_FROZEN)
    .put("dried", PreservationMethodType.DRIED)
    .put("driedAndPressed", PreservationMethodType.DRIED_AND_PRESSED)
    .put("formalin", PreservationMethodType.FORMALIN)
    .put("refrigerated", PreservationMethodType.REFRIGERATED)
    .put("freezeDried", PreservationMethodType.FREEZE_DRIED)
    .put("glycerin", PreservationMethodType.GLYCERIN)
    .put("gumArabic", PreservationMethodType.GUM_ARABIC)
    .put("microscopicPreparation", PreservationMethodType.MICROSCOPIC_PREPARATION)
    .put("mounted", PreservationMethodType.MOUNTED)
    .put("pinned", PreservationMethodType.PINNED)
    .put("other", PreservationMethodType.OTHER)
    .build();

  static {
  }

  /**
   * Construct a <b>PreservationMethodType</b> <i>Converter</i> that throws
   * a <code>ConversionException</code> if an error occurs.
   */
  public PreservationMethodTypeConverter() {
  }

  /**
   * Construct a <b>PreservationMethodType</b> <i>Converter</i> that returns
   * a default value if an error occurs.
   *
   * @param defaultValue The default value to be returned
   *                     if the value to be converted is missing or an error
   *                     occurs converting the value.
   */
  public PreservationMethodTypeConverter(Object defaultValue) {
    super(defaultValue);
  }

  /**
   * Return the default type this <code>Converter</code> handles.
   *
   * @return The default type this <code>Converter</code> handles.
   */
  protected Class getDefaultType() {
    return PreservationMethodType.class;
  }

  /**
   * <p>Convert a PreservationMethodType or object into a String.</p> Checks map with alternative values for each
   * PreservationMethodType before returning the default value.
   *
   * @param type  Data type to which this value should be converted
   * @param value The input value to be converted
   *
   * @return The converted value.
   *
   * @throws Throwable if an error occurs converting to the specified type
   */
  protected Object convertToType(Class type, Object value) throws Throwable {
    // never null, super class implements this as:
    // return value.toString();
    if (LOOKUP.containsKey(value.toString())) {
      return LOOKUP.get(value.toString());
    }
    return DEFAULT_TYPE;
  }
}
