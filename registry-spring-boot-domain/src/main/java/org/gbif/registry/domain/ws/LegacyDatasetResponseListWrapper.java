package org.gbif.registry.domain.ws;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.util.List;

@JsonSerialize(using = LegacyDatasetResponseListWrapper.LegacyDatasetResponseListWrapperJsonSerializer.class)
@XmlRootElement(name = "legacyDatasetResponses")
public class LegacyDatasetResponseListWrapper {

  private List<LegacyDatasetResponse> legacyDatasetResponses;

  public LegacyDatasetResponseListWrapper() {
  }

  public LegacyDatasetResponseListWrapper(List<LegacyDatasetResponse> legacyDatasetResponses) {
    this.legacyDatasetResponses = legacyDatasetResponses;
  }

  @XmlElement(name = "resource")
  public List<LegacyDatasetResponse> getLegacyDatasetResponses() {
    return legacyDatasetResponses;
  }

  public void setLegacyDatasetResponses(List<LegacyDatasetResponse> legacyDatasetResponses) {
    this.legacyDatasetResponses = legacyDatasetResponses;
  }

  public static class LegacyDatasetResponseListWrapperJsonSerializer extends JsonSerializer<LegacyDatasetResponseListWrapper> {

    @Override
    public void serialize(LegacyDatasetResponseListWrapper value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
      if (value == null) {
        jgen.writeNull();
        return;
      }

      jgen.writeStartArray();
      if (value.getLegacyDatasetResponses() != null) {
        for (LegacyDatasetResponse item : value.getLegacyDatasetResponses()) {
          jgen.writeObject(item);
        }
      }
      jgen.writeEndArray();
    }
  }
}
