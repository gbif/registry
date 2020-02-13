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
package org.gbif.directory.client.retrofit;

import org.gbif.api.jackson.LicenseSerde;
import org.gbif.api.vocabulary.License;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class JacksonObjectMapper {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static {
    SimpleModule simpleModule = new SimpleModule();
    simpleModule.addSerializer(License.class, new LicenseSerde.LicenseJsonSerializer());
    simpleModule.addDeserializer(License.class, new LicenseSerde.LicenseJsonDeserializer());
    simpleModule.addSerializer(
        LocalDate.class,
        new JsonSerializer<LocalDate>() {
          @Override
          public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers)
              throws IOException {
            gen.writeString(value.toString());
          }
        });
    simpleModule.addDeserializer(
        LocalDate.class,
        new JsonDeserializer<LocalDate>() {
          @Override
          public LocalDate deserialize(JsonParser p, DeserializationContext ctxt)
              throws IOException, JsonProcessingException {
            try {
              if (p != null && p.getTextLength() > 0) {
                return LocalDate.parse(p.getText());
              }
              return null;
            } catch (DateTimeParseException e) {
              throw new IOException(
                  "Unable to deserialize LocalDate from provided value (not an ISO date?): "
                      + p.getText());
            }
          }
        });
    OBJECT_MAPPER.registerModule(simpleModule);
    OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public static ObjectMapper get() {
    return OBJECT_MAPPER;
  }

  ObjectMapper objectMapper() {
    return JacksonObjectMapper.get();
  }
}
