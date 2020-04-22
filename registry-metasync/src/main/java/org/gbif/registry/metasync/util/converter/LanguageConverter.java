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
package org.gbif.registry.metasync.util.converter;

import org.gbif.api.vocabulary.Language;

import org.apache.commons.beanutils.Converter;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Used by commons-digester (via commons-beanutils) to convert Strings into our own {@link Language}
 * objects using its {@link Language#fromIsoCode(String)} method. That method returns {@link
 * Language#UNKNOWN} if it can't find a match.
 */
public class LanguageConverter implements Converter {

  @Override
  public Object convert(Class type, Object value) {
    checkNotNull(type, "type cannot be null");

    if (value == null) {
      return Language.ENGLISH;
    }

    checkArgument(
        type.equals(Language.class),
        "Conversion target should be org.gbif.api.vocabulary.Language, but is %s",
        type);
    checkArgument(
        String.class.isAssignableFrom(value.getClass()),
        "Value should be a string, but is a %s",
        value.getClass());

    return Language.fromIsoCode((String) value);
  }
}
