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
package org.gbif.registry.domain.ws;

import java.io.IOException;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(
    using = LegacyEndpointResponseListWrapper.LegacyEndpointResponseListWrapperJsonSerializer.class)
@JsonDeserialize(
    using =
        LegacyEndpointResponseListWrapper.LegacyEndpointResponseListWrapperJsonDeserializer.class)
@XmlRootElement(name = "legacyEndpointResponses")
public class LegacyEndpointResponseListWrapper {

  private List<LegacyEndpointResponse> legacyEndpointResponses;

  public LegacyEndpointResponseListWrapper() {}

  public LegacyEndpointResponseListWrapper(List<LegacyEndpointResponse> legacyEndpointResponses) {
    this.legacyEndpointResponses = legacyEndpointResponses;
  }

  @XmlElement(name = "service")
  public List<LegacyEndpointResponse> getLegacyEndpointResponses() {
    return legacyEndpointResponses;
  }

  public void setLegacyEndpointResponses(List<LegacyEndpointResponse> legacyEndpointResponses) {
    this.legacyEndpointResponses = legacyEndpointResponses;
  }

  public static class LegacyEndpointResponseListWrapperJsonSerializer
      extends JsonSerializer<LegacyEndpointResponseListWrapper> {

    @Override
    public void serialize(
        LegacyEndpointResponseListWrapper value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException {
      if (value == null) {
        jgen.writeNull();
        return;
      }

      jgen.writeStartArray();
      if (value.getLegacyEndpointResponses() != null) {
        for (LegacyEndpointResponse item : value.getLegacyEndpointResponses()) {
          jgen.writeObject(item);
        }
      }
      jgen.writeEndArray();
    }
  }

  public static class LegacyEndpointResponseListWrapperJsonDeserializer
      extends JsonDeserializer<LegacyEndpointResponseListWrapper> {

    @Override
    public LegacyEndpointResponseListWrapper deserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException {
      if (p.getCurrentToken() != JsonToken.START_ARRAY) {
        throw new JsonParseException(p, "Start array expected");
      }

      List<LegacyEndpointResponse> legacyEndpointResponses =
          p.readValueAs(new TypeReference<List<LegacyEndpointResponse>>() {});

      return new LegacyEndpointResponseListWrapper(legacyEndpointResponses);
    }
  }
}
