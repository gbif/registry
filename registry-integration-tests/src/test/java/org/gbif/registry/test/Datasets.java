/*
 * Copyright 2020-2021 Global Biodiversity Information Facility (GBIF)
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
package org.gbif.registry.test;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Citation;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.License;
import org.gbif.registry.metadata.CitationGenerator;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class Datasets extends JsonBackedData<Dataset> {

  private static DatasetService datasetService;

  public static final String DATASET_ALIAS = "BGBM";
  public static final String DATASET_ABBREVIATION = "BGBM";
  public static final Language DATASET_LANGUAGE = Language.DANISH;
  public static final String DATASET_RIGHTS = "The rights";
  public static final License DATASET_LICENSE = License.CC_BY_NC_4_0;
  public static final DOI DATASET_DOI = new DOI(DOI.TEST_PREFIX, "gbif.2014.XSD123");
  public static final Citation DATASET_CITATION =
      new Citation("This is a citation text", "ABC", true);
  public static final String DATASET_TITLE =
      "Pontaurus needs more than 255 characters for it's title. It is a very, very, very, very long title in the German language. Word by word and character by character it's exact title is: \"Vegetationskundliche Untersuchungen in der Hochgebirgsregion der Bolkar Daglari & Aladaglari, Türkei\"";
  public static final String DATASET_DESCRIPTION = "The Berlin Botanical...";

  @Autowired
  public Datasets(
      DatasetService datasetService,
      ObjectMapper objectMapper,
      SimplePrincipalProvider simplePrincipalProvider) {
    super(
        "data/dataset.json",
        new TypeReference<Dataset>() {},
        objectMapper,
        simplePrincipalProvider);
    this.datasetService = datasetService;
  }

  public Dataset newInstance(UUID publishingOrganizationKey, UUID installationKey) {
    Dataset d = super.newInstance();
    d.setPublishingOrganizationKey(publishingOrganizationKey);
    d.setInstallationKey(installationKey);
    d.setDoi(DATASET_DOI);
    d.setLicense(DATASET_LICENSE);
    return d;
  }

  /**
   * Persist a new Dataset associated to an publishing organization and installation for use in Unit
   * Tests.
   *
   * @param doi dataset DOI
   * @param organizationKey publishing organization key
   * @param installationKey installation key
   * @return persisted Dataset
   */
  public Dataset newPersistedInstance(DOI doi, UUID organizationKey, UUID installationKey) {
    Dataset dataset = newInstance(organizationKey, installationKey);
    dataset.setDoi(doi);
    UUID key = datasetService.create(dataset);
    // some properties like created, modified are only set when the dataset is retrieved anew
    return datasetService.get(key);
  }

  /**
   * Persist a new Dataset associated to an publishing organization and installation for use in Unit
   * Tests.
   *
   * @param organizationKey publishing organization key
   * @param installationKey installation key
   * @return persisted Dataset
   */
  public Dataset newPersistedInstance(UUID organizationKey, UUID installationKey) {
    Dataset dataset = newInstance(organizationKey, installationKey);
    UUID key = datasetService.create(dataset);
    // some properties like created, modified are only set when the dataset is retrieved anew
    return datasetService.get(key);
  }

  public static CitationGenerator.CitationData buildExpectedCitation(
      Dataset dataset, String organizationTitle) {
    return CitationGenerator.generateCitation(dataset, organizationTitle);
  }

  /**
   * Processed properties are properties that are expected to NOT matched the provided data.
   *
   * @param dataset
   * @return a mutable mpa in case more properties shall be added.
   */
  public static Map<String, Object> buildExpectedProcessedProperties(Dataset dataset) {
    CitationGenerator.CitationData expectedCitation =
        buildExpectedCitation(dataset, Organizations.ORGANIZATION_TITLE);
    Map<String, Object> processedProperties = new HashMap<>();
    processedProperties.put("citation.text", expectedCitation.getCitation().getText());
    return processedProperties;
  }
}
