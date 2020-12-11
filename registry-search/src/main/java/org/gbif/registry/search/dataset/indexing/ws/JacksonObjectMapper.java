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
package org.gbif.registry.search.dataset.indexing.ws;

import org.gbif.api.jackson.LicenseSerde;
import org.gbif.api.vocabulary.License;

import java.text.SimpleDateFormat;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

@Component
public class JacksonObjectMapper {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static {
    SimpleModule simpleModule = new SimpleModule();
    simpleModule.addSerializer(License.class, new LicenseSerde.LicenseJsonSerializer());
    simpleModule.addDeserializer(License.class, new LicenseSerde.LicenseJsonDeserializer());
    OBJECT_MAPPER.registerModule(simpleModule);
    OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    OBJECT_MAPPER.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS"));
  }

  public static ObjectMapper get() {
    return OBJECT_MAPPER;
  }

  @Bean("apiMapper")
  @Lazy
  ObjectMapper objectMapper() {
    return JacksonObjectMapper.get();
  }
}
