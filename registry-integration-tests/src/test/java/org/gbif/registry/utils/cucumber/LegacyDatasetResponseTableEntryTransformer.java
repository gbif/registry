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

import org.gbif.registry.domain.ws.LegacyDatasetResponse;

import java.util.Map;

import io.cucumber.datatable.TableEntryTransformer;

public class LegacyDatasetResponseTableEntryTransformer
    implements TableEntryTransformer<LegacyDatasetResponse> {

  @Override
  public LegacyDatasetResponse transform(Map<String, String> entry) {
    LegacyDatasetResponse result = new LegacyDatasetResponse();

    result.setKey(entry.get("key"));
    result.setDescription(entry.get("description"));
    result.setDescriptionLanguage(entry.get("descriptionLanguage"));
    result.setHomepageURL(entry.get("homepageURL"));
    result.setName(entry.get("name"));
    result.setNameLanguage(entry.get("nameLanguage"));
    result.setOrganisationKey(entry.get("organisationKey"));
    result.setPrimaryContactAddress(entry.get("primaryContactAddress"));
    result.setPrimaryContactDescription(entry.get("primaryContactDescription"));
    result.setPrimaryContactEmail(entry.get("primaryContactEmail"));
    result.setPrimaryContactName(entry.get("primaryContactName"));
    result.setPrimaryContactPhone(entry.get("primaryContactPhone"));
    result.setPrimaryContactType(entry.get("primaryContactType"));

    return result;
  }
}
