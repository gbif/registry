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

import java.util.Map;

import com.google.common.collect.ImmutableMap;

/**
 * {@link org.apache.commons.beanutils.Converter} implementation that handles conversion to and from
 * <b>org.gbif.registry.api.model.vocabulary.PreservationMethodType</b> objects.
 */
public class PreservationMethodTypeConverter extends EnumTypeConverter<PreservationMethodType> {

  private static final Map<String, PreservationMethodType> LOOKUP =
      ImmutableMap.<String, PreservationMethodType>builder()
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
   * Construct a <b>PreservationMethodType</b> <i>Converter</i> that throws a <code>
   * ConversionException</code> if an error occurs.
   */
  public PreservationMethodTypeConverter(PreservationMethodType defaultValue) {
    super(PreservationMethodType.class, defaultValue);
    addMappings(LOOKUP);
  }
}
