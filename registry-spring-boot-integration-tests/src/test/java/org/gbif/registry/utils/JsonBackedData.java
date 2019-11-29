package org.gbif.registry.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import java.io.IOException;

/**
 * Base class providing basic Jackson based factories for new object instances.
 */
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
