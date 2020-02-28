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

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.Language;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import io.cucumber.datatable.TableEntryTransformer;

public class DatasetTableEntryTransformer implements TableEntryTransformer<Dataset> {

  @Override
  public Dataset transform(Map<String, String> entry) {
    Dataset dataset = new Dataset();
    Optional.ofNullable(entry.get("key")).map(UUID::fromString).ifPresent(dataset::setKey);
    Optional.ofNullable(entry.get("publishingOrganizationKey"))
        .map(UUID::fromString)
        .ifPresent(dataset::setPublishingOrganizationKey);
    dataset.setDescription(entry.get("description"));
    dataset.setTitle(entry.get("title"));
    Optional.ofNullable(entry.get("installationKey"))
        .map(UUID::fromString)
        .ifPresent(dataset::setInstallationKey);
    Optional.ofNullable(entry.get("type")).map(DatasetType::fromString).ifPresent(dataset::setType);
    Optional.ofNullable(entry.get("logoUrl")).map(URI::create).ifPresent(dataset::setLogoUrl);
    Optional.ofNullable(entry.get("homepage")).map(URI::create).ifPresent(dataset::setHomepage);
    Optional.ofNullable(entry.get("external"))
        .map(Boolean::parseBoolean)
        .ifPresent(dataset::setExternal);
    dataset.setCreatedBy(entry.get("createdBy"));
    dataset.setModifiedBy(entry.get("modifiedBy"));
    Optional.ofNullable(entry.get("language"))
        .map(Language::valueOf)
        .ifPresent(dataset::setLanguage);

    return dataset;
  }
}
