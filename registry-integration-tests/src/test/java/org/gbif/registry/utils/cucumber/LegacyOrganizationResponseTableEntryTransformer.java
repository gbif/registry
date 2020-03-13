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
package org.gbif.registry.utils.cucumber;

import org.gbif.registry.domain.ws.LegacyOrganizationResponse;

import java.util.Map;

import io.cucumber.datatable.TableEntryTransformer;

public class LegacyOrganizationResponseTableEntryTransformer
    implements TableEntryTransformer<LegacyOrganizationResponse> {

  @Override
  public LegacyOrganizationResponse transform(Map<String, String> entry) {
    LegacyOrganizationResponse result = new LegacyOrganizationResponse();

    result.setKey(entry.get("key"));
    result.setName(entry.get("name"));
    result.setNameLanguage(entry.get("nameLanguage"));
    result.setDescription(entry.get("description"));
    result.setDescriptionLanguage(entry.get("descriptionLanguage"));
    result.setHomepageURL(entry.get("homepageURL"));
    result.setPrimaryContactType(entry.get("primaryContactType"));
    result.setPrimaryContactName(entry.get("primaryContactName"));
    result.setPrimaryContactEmail(entry.get("primaryContactEmail"));
    result.setPrimaryContactAddress(entry.get("primaryContactAddress"));
    result.setPrimaryContactPhone(entry.get("primaryContactPhone"));
    result.setPrimaryContactDescription(entry.get("primaryContactDescription"));
    result.setNodeKey(entry.get("nodeKey"));
    result.setNodeName(entry.get("nodeName"));
    result.setNodeContactEmail(entry.get("nodeContactEmail"));

    return result;
  }
}
