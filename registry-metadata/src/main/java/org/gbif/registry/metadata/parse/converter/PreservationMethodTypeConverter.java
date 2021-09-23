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

import org.gbif.api.vocabulary.PreservationMethodType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link org.apache.commons.beanutils.Converter} implementation that handles conversion to and from
 * <b>org.gbif.registry.api.model.vocabulary.PreservationMethodType</b> objects.
 */
public class PreservationMethodTypeConverter extends EnumTypeConverter<PreservationMethodType> {

  private static final Map<String, PreservationMethodType> LOOKUP;

  static {
    Map<String, PreservationMethodType> lookupInternal = new HashMap<>();
    lookupInternal.put("noTreatment", PreservationMethodType.NO_TREATMENT);
    lookupInternal.put("alcohol", PreservationMethodType.ALCOHOL);
    lookupInternal.put("deepFrozen", PreservationMethodType.DEEP_FROZEN);
    lookupInternal.put("dried", PreservationMethodType.DRIED);
    lookupInternal.put("driedAndPressed", PreservationMethodType.DRIED_AND_PRESSED);
    lookupInternal.put("formalin", PreservationMethodType.FORMALIN);
    lookupInternal.put("refrigerated", PreservationMethodType.REFRIGERATED);
    lookupInternal.put("freezeDried", PreservationMethodType.FREEZE_DRIED);
    lookupInternal.put("glycerin", PreservationMethodType.GLYCERIN);
    lookupInternal.put("gumArabic", PreservationMethodType.GUM_ARABIC);
    lookupInternal.put("microscopicPreparation", PreservationMethodType.MICROSCOPIC_PREPARATION);
    lookupInternal.put("mounted", PreservationMethodType.MOUNTED);
    lookupInternal.put("pinned", PreservationMethodType.PINNED);
    lookupInternal.put("other", PreservationMethodType.OTHER);

    LOOKUP = Collections.unmodifiableMap(lookupInternal);
  }

  /**
   * Construct a <b>PreservationMethodType</b> <i>Converter</i> that throws a <code>
   * ConversionException</code> if an error occurs.
   */
  public PreservationMethodTypeConverter(PreservationMethodType defaultValue) {
    super(PreservationMethodType.class, defaultValue);
    addMappings(LOOKUP);
  }
}
