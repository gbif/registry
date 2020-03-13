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
package org.gbif.registry.domain.ws;

import java.io.IOException;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(
    using =
        LegacyOrganizationBriefResponseListWrapper
            .LegacyOrganizationBriefResponseListWrapperJsonSerializer.class)
@XmlRootElement(name = "legacyOrganizationBriefResponses")
public class LegacyOrganizationBriefResponseListWrapper {

  private List<LegacyOrganizationBriefResponse> legacyOrganizationBriefResponses;

  public LegacyOrganizationBriefResponseListWrapper() {}

  public LegacyOrganizationBriefResponseListWrapper(
      List<LegacyOrganizationBriefResponse> legacyOrganizationBriefResponses) {
    this.legacyOrganizationBriefResponses = legacyOrganizationBriefResponses;
  }

  @XmlElement(name = "organisation")
  public List<LegacyOrganizationBriefResponse> getLegacyOrganizationBriefResponses() {
    return legacyOrganizationBriefResponses;
  }

  public void setLegacyOrganizationBriefResponses(
      List<LegacyOrganizationBriefResponse> legacyOrganizationBriefResponses) {
    this.legacyOrganizationBriefResponses = legacyOrganizationBriefResponses;
  }

  public static class LegacyOrganizationBriefResponseListWrapperJsonSerializer
      extends JsonSerializer<LegacyOrganizationBriefResponseListWrapper> {

    @Override
    public void serialize(
        LegacyOrganizationBriefResponseListWrapper value,
        JsonGenerator jgen,
        SerializerProvider provider)
        throws IOException {
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
