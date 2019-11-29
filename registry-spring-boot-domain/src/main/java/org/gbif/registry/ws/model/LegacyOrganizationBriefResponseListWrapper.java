package org.gbif.registry.ws.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.util.List;

@JsonSerialize(using = LegacyOrganizationBriefResponseListWrapper.LegacyOrganizationBriefResponseListWrapperJsonSerializer.class)
@XmlRootElement(name = "legacyOrganizationBriefResponses")
public class LegacyOrganizationBriefResponseListWrapper {

  private List<LegacyOrganizationBriefResponse> legacyOrganizationBriefResponses;

  public LegacyOrganizationBriefResponseListWrapper() {
  }

  public LegacyOrganizationBriefResponseListWrapper(List<LegacyOrganizationBriefResponse> legacyOrganizationBriefResponses) {
    this.legacyOrganizationBriefResponses = legacyOrganizationBriefResponses;
  }

  @XmlElement(name = "organisation")
  public List<LegacyOrganizationBriefResponse> getLegacyOrganizationBriefResponses() {
    return legacyOrganizationBriefResponses;
  }

  public void setLegacyOrganizationBriefResponses(List<LegacyOrganizationBriefResponse> legacyOrganizationBriefResponses) {
    this.legacyOrganizationBriefResponses = legacyOrganizationBriefResponses;
  }

  public static class LegacyOrganizationBriefResponseListWrapperJsonSerializer extends JsonSerializer<LegacyOrganizationBriefResponseListWrapper> {

    @Override
    public void serialize(LegacyOrganizationBriefResponseListWrapper value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
      if (value == null) {
        jgen.writeNull();
        return;
      }

      jgen.writeStartArray();
      if (value.getLegacyOrganizationBriefResponses() != null) {
        for (LegacyOrganizationBriefResponse item : value.getLegacyOrganizationBriefResponses()) {
          jgen.writeObject(item);
        }
      }
      jgen.writeEndArray();
    }
  }
}
