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

import org.gbif.utils.CommonStringUtils;

import java.net.URI;
import java.net.URL;
import java.util.Objects;

import org.apache.commons.beanutils.converters.AbstractConverter;
import org.apache.commons.lang3.StringUtils;

/**
 * Greedy String to URI converter. Greedy in the sense that it will try to add a default protocol
 * (http://) in cases where none exists. For example, this captures URLs such as www.gbif.org that
 * otherwise would get converted into a wrong URI.
 */
public final class GreedyUriConverter extends AbstractConverter {

  public static final String DEFAULT_PROTOCOL = "http://";

  /**
   * Construct a <b>URI</b> <i>Converter</i> that throws a {@code ConversionException} if an error
   * occurs.
   */
  public GreedyUriConverter() {}

  /**
   * Construct a <b>URI</b> <i>Converter</i> that returns a default value if an error occurs.
   *
   * @param defaultValue The default value to be returned if the value to be converted is missing or
   *     an error occurs converting the value.
   */
  public GreedyUriConverter(Object defaultValue) {
    super(defaultValue);
  }

  /**
   * Convert a String into a java.net.URI. In case its missing the protocol prefix, it is prefixed
   * with the default protocol.
   *
   * @param type Data type to which this value should be converted.
   * @param value The input value to be converted.
   * @return The converted value, or null if an exception occurred
   */
  @Override
  protected Object convertToType(Class type, Object value) {
    Objects.requireNonNull(value, "Must provide a type to convert to null");
    String valueAsString =
        StringUtils.trimToNull(CommonStringUtils.trim(String.valueOf(value)));

    URI uri = null;
    if (StringUtils.isNotEmpty(valueAsString)) {
      try {
        uri = URI.create(valueAsString);
        // try adding the default scheme if the URI is opaque or missing
        if (uri.getScheme() == null || uri.isOpaque()) {
          try {
            uri = URI.create(DEFAULT_PROTOCOL + valueAsString);
          } catch (IllegalArgumentException e) {
            // keep the previous scheme-less result
          }
        }
      } catch (IllegalArgumentException ignored) {
      }
    }
    return uri;
  }

  /**
   * Return the default type this {@code Converter} handles.
   *
   * @return The default type this {@code Converter} handles.
   */
  @Override
  protected Class getDefaultType() {
    return URI.class;
  }

  /**
   * Convert a String into a java.net.URI. In case its missing the protocol prefix, it is prefixed
   * with the default protocol.
   *
   * @param value The input value to be converted.
   * @return The converted value, or null if an exception occurred
   */
  public URI convert(String value) {
    return (URI) convertToType(URL.class, value);
  }
}
