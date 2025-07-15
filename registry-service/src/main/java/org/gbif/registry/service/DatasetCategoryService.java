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
package org.gbif.registry.service;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.api.ConceptListParams;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.api.model.registry.Dataset;
import org.gbif.registry.domain.ws.DerivedDataset;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.DerivedDatasetMapper;
import org.gbif.registry.search.dataset.indexing.EsDatasetRealtimeIndexer;
import org.gbif.vocabulary.client.ConceptClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DatasetCategoryService implements VocabularyPostProcessor {

  private final ConceptClient conceptClient;
  private final DatasetMapper datasetMapper;
  private final DerivedDatasetMapper derivedDatasetMapper;
  private final EsDatasetRealtimeIndexer esDatasetRealtimeIndexer;
  private static final String DATASET_CATEGORY_VOCABULARY = "DatasetCategory";

  @Autowired
  public DatasetCategoryService(ConceptClient conceptClient,
                              DatasetMapper datasetMapper,
                              DerivedDatasetMapper derivedDatasetMapper,
                              EsDatasetRealtimeIndexer esDatasetRealtimeIndexer) {
    this.conceptClient = conceptClient;
    this.datasetMapper = datasetMapper;
    this.derivedDatasetMapper = derivedDatasetMapper;
    this.esDatasetRealtimeIndexer = esDatasetRealtimeIndexer;
  }

  @Override
  public boolean canHandle(String vocabularyName) {
    return DATASET_CATEGORY_VOCABULARY.equals(vocabularyName);
  }

  @Override
  @Transactional
  public int process(String vocabularyName) {
    return cleanupDeprecatedCategories(vocabularyName);
  }

  /**
   * Cleans up deprecated categories from datasets for a specific vocabulary.
   * This method finds all datasets that have deprecated categories from the given vocabulary and removes them.
   *
   * @param vocabularyName the vocabulary name to check (e.g., "dataset_category")
   * @return the number of datasets that were updated
   */
  public int cleanupDeprecatedCategories(String vocabularyName) {
    log.info("Starting cleanup of deprecated dataset categories for vocabulary: {}", vocabularyName);

    // Get deprecated concepts directly from the vocabulary
    Set<String> deprecatedCategories = findDeprecatedCategoriesFromVocabulary(vocabularyName);
    log.info("Found {} deprecated categories from vocabulary '{}': {}",
             deprecatedCategories.size(), vocabularyName, deprecatedCategories);

    if (deprecatedCategories.isEmpty()) {
      log.info("No deprecated categories found for vocabulary: {}, cleanup complete", vocabularyName);
      return 0;
    }

    // Find datasets with deprecated categories using direct mapper
    List<Dataset> datasetsToUpdate = datasetMapper.findDatasetsWithDeprecatedCategories(deprecatedCategories);
    log.info("Found {} datasets with deprecated categories from vocabulary: {}", datasetsToUpdate.size(), vocabularyName);

    // Remove deprecated categories from each dataset
    int updatedCount = 0;
    for (Dataset dataset : datasetsToUpdate) {
      try {
        boolean updated = false;

        // Remove each deprecated category individually
        for (String deprecatedCategory : deprecatedCategories) {
          if (dataset.getCategory() != null && dataset.getCategory().contains(deprecatedCategory)) {
            dataset.getCategory().remove(deprecatedCategory);
            updated = true;
          }
        }

        // Update dataset directly in database and elasticsearch
        if (updated) {
          datasetMapper.update(dataset);
          esDatasetRealtimeIndexer.index(dataset);
          log.debug("Updated dataset in database and elasticsearch for dataset: {}", dataset.getKey());
        }

        updatedCount++;
      } catch (Exception e) {
        log.error("Failed to remove deprecated categories from dataset: {}", dataset.getKey(), e);
      }
    }

    // Clean deprecated categories from derived datasets
    log.info("Cleaning deprecated categories from derived datasets");

    List<DerivedDataset> derivedDatasetsWithDeprecatedCategories =
        derivedDatasetMapper.findDerivedDatasetsWithDeprecatedCategories(deprecatedCategories);

    log.info("Found {} derived datasets with deprecated categories", derivedDatasetsWithDeprecatedCategories.size());

    for (DerivedDataset derivedDataset : derivedDatasetsWithDeprecatedCategories) {
      try {
        boolean updated = false;

        // Remove each deprecated category individually
        for (String deprecatedCategory : deprecatedCategories) {
          if (derivedDataset.getCategory() != null && derivedDataset.getCategory().contains(deprecatedCategory)) {
            derivedDataset.getCategory().remove(deprecatedCategory);
            updated = true;
          }
        }

        // Update derived dataset directly in database
        if (updated) {
          derivedDatasetMapper.update(derivedDataset);
          log.debug("Updated derived dataset in database for DOI: {}", derivedDataset.getDoi());
        }
      } catch (Exception e) {
        log.error("Failed to remove deprecated categories from derived dataset: {}", derivedDataset.getDoi(), e);
      }
    }

    log.info("Successfully cleaned up deprecated categories from {} datasets and derived datasets for vocabulary: {}",
             updatedCount, vocabularyName);
    return updatedCount;
  }

  /**
   * Finds deprecated categories directly from the vocabulary.
   *
   * @param vocabularyName the vocabulary name to check
   * @return set of deprecated category names
   */
  private Set<String> findDeprecatedCategoriesFromVocabulary(String vocabularyName) {
    Set<String> deprecatedCategories = new HashSet<>();

    try {
      // Get all concepts from the vocabulary (same approach as VocabularyConceptService)
      List<ConceptView> allConcepts = new ArrayList<>();
      PagingRequest page = new PagingRequest(0, 100);
      PagingResponse<ConceptView> response;

      do {
        ConceptListParams params = ConceptListParams.builder()
            .includeChildren(true)
            .includeParents(true)
            .offset(page.getOffset())
            .limit(page.getLimit())
            .build();

        response = conceptClient.listConceptsLatestRelease(vocabularyName, params);

        if (response != null && response.getResults() != null) {
          allConcepts.addAll(response.getResults());
        }

        page = new PagingRequest(page.getOffset() + (response != null ? response.getResults().size() : 0), 100);
      } while (response != null && !response.isEndOfRecords());

      // Filter deprecated concepts from all concepts
      for (ConceptView conceptView : allConcepts) {
        if (conceptView.getConcept() != null && conceptView.getConcept().getDeprecated() != null) {
          deprecatedCategories.add(conceptView.getConcept().getName());
        }
      }

      log.debug("Found {} deprecated concepts in vocabulary '{}': {}",
               deprecatedCategories.size(), vocabularyName, deprecatedCategories);

    } catch (Exception e) {
      log.error("Error fetching deprecated concepts from vocabulary '{}': {}", vocabularyName, e.getMessage());
    }

    return deprecatedCategories;
  }
}
