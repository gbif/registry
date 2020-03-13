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
package org.gbif.registry.utils;

import java.io.IOException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

/** Base class providing basic Jackson based factories for new object instances. */
abstract class JsonBackedData<T> {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final TypeReference<T> type;
  private final String json; // for reuse to save file IO

  // only for instantiation by subclasses, type references are required to keep typing at runtime
  protected JsonBackedData(String file, TypeReference<T> type) {
    json = getJson(file);
    this.type = type;
  }

  protected T newTypedInstance() {
    try {
      return MAPPER.readValue(json, type);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // utility method to read the file, and throw RTE if there is a problem
  private String getJson(String file) {
    try {
      return Resources.toString(Resources.getResource(file), Charsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected String newInstanceAsRawJson() {
    return json;
  }
}
