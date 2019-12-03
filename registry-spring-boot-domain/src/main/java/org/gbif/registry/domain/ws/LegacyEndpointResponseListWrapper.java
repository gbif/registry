package org.gbif.registry.domain.ws;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.util.List;

@JsonSerialize(using = LegacyEndpointResponseListWrapper.LegacyEndpointResponseListWrapperJsonSerializer.class)
@XmlRootElement(name = "legacyEndpointResponses")
public class LegacyEndpointResponseListWrapper {

  private List<LegacyEndpointResponse> legacyEndpointResponses;

  public LegacyEndpointResponseListWrapper() {
  }

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

  public static class LegacyEndpointResponseListWrapperJsonSerializer extends JsonSerializer<LegacyEndpointResponseListWrapper> {

    @Override
    public void serialize(LegacyEndpointResponseListWrapper value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
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
}
