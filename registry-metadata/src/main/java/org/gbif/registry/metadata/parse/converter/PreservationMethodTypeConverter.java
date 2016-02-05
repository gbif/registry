package org.gbif.registry.metadata.parse.converter;

import org.gbif.api.vocabulary.PreservationMethodType;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

/**
 * {@link org.apache.commons.beanutils.Converter} implementation that handles conversion
 * to and from <b>org.gbif.registry.api.model.vocabulary.PreservationMethodType</b> objects.
 */
public class PreservationMethodTypeConverter extends EnumTypeConverter<PreservationMethodType> {

  private static final Map<String, PreservationMethodType> LOOKUP = ImmutableMap.<String, PreservationMethodType>builder()
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

  /**
   * Construct a <b>PreservationMethodType</b> <i>Converter</i> that throws
   * a <code>ConversionException</code> if an error occurs.
   */
  public PreservationMethodTypeConverter(PreservationMethodType defaultValue) {
      super(PreservationMethodType.class, defaultValue);
      addMappings(LOOKUP);
  }

}
