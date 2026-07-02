/*
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
package org.gbif.registry.persistence.mapper.handler;

import org.gbif.api.model.common.search.SearchParameter;
import org.gbif.api.model.event.search.EventSearchParameter;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;

import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Jackson deserializers for predicates stored on {@code occurrence_download}.
 *
 * <p>{@code occurrence_download} holds downloads for both {@link org.gbif.api.model.occurrence.DownloadType#OCCURRENCE}
 * and {@link org.gbif.api.model.occurrence.DownloadType#EVENT}, but predicate filters may reference
 * parameters from either {@link OccurrenceSearchParameter} or {@link EventSearchParameter}. For
 * example, occurrence downloads can filter on parent event Humboldt fields.
 */
final class OccurrenceDownloadSearchParameterDeserializers {

  private OccurrenceDownloadSearchParameterDeserializers() {}

  static Optional<SearchParameter> lookup(String name) {
    if (name == null || name.isBlank()) {
      return Optional.empty();
    }
    return OccurrenceSearchParameter.lookup(name)
        .map(SearchParameter.class::cast)
        .or(() -> EventSearchParameter.lookupEventParam(name).map(SearchParameter.class::cast));
  }

  static SimpleModule module() {
    var deserializer = new SearchParameterDeserializer();
    var keyDeserializer = new SearchParameterKeyDeserializer();
    return new SimpleModule()
        .addDeserializer(SearchParameter.class, deserializer)
        .addKeyDeserializer(SearchParameter.class, keyDeserializer)
        .addDeserializer(
            OccurrenceSearchParameter.class, asTyped(deserializer, OccurrenceSearchParameter.class))
        .addKeyDeserializer(
            OccurrenceSearchParameter.class,
            asTypedKey(keyDeserializer, OccurrenceSearchParameter.class))
        .addDeserializer(EventSearchParameter.class, asTyped(deserializer, EventSearchParameter.class))
        .addKeyDeserializer(
            EventSearchParameter.class, asTypedKey(keyDeserializer, EventSearchParameter.class));
  }

  private static <T extends SearchParameter> JsonDeserializer<T> asTyped(
      JsonDeserializer<SearchParameter> delegate, Class<T> type) {
    return new JsonDeserializer<T>() {
      @Override
      public T deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        SearchParameter parameter = delegate.deserialize(parser, ctxt);
        return type.isInstance(parameter) ? type.cast(parameter) : null;
      }
    };
  }

  private static <T extends SearchParameter> KeyDeserializer asTypedKey(
      KeyDeserializer delegate, Class<T> type) {
    return new KeyDeserializer() {
      @Override
      public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
        Object parameter = delegate.deserializeKey(key, ctxt);
        return type.isInstance(parameter) ? type.cast(parameter) : null;
      }
    };
  }

  private static final class SearchParameterDeserializer extends JsonDeserializer<SearchParameter> {

    @Override
    public SearchParameter deserialize(JsonParser parser, DeserializationContext ctxt)
        throws IOException {
      if (parser.currentToken() == null) {
        parser.nextToken();
      }
      if (parser.currentToken() == JsonToken.VALUE_STRING) {
        return lookup(parser.getText()).orElse(null);
      }
      if (parser.currentToken() == JsonToken.START_OBJECT) {
        ObjectNode node = parser.getCodec().readTree(parser);
        if (node.has("name")) {
          return lookup(node.get("name").asText()).orElse(null);
        }
      }
      return null;
    }
  }

  private static final class SearchParameterKeyDeserializer extends KeyDeserializer {

    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt) {
      return lookup(key).orElse(null);
    }
  }
}
