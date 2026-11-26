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
package org.gbif.registry.service.privatesector;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.Organization;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.MachineTaggableMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.params.DatasetListParams;
import org.gbif.registry.persistence.mapper.params.OrganizationListParams;
import org.gbif.registry.search.dataset.indexing.EsDatasetRealtimeIndexer;
import org.gbif.registry.service.VocabularyPostProcessor;
import org.gbif.registry.service.WithMyBatis;
import org.gbif.registry.service.collections.utils.Vocabularies;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.client.ConceptClient;
import org.gbif.vocabulary.model.search.LookupResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.extern.slf4j.Slf4j;

import static org.gbif.registry.service.privatesector.PrivateSectorCategories.AUTO_CATEGORY_TAG;
import static org.gbif.registry.service.privatesector.PrivateSectorCategories.BUSINESS_SECTOR;
import static org.gbif.registry.service.privatesector.PrivateSectorCategories.DATASET_CATEGORY_VOCABULARY;
import static org.gbif.registry.service.privatesector.PrivateSectorCategories.EXCLUDED_CATEGORY_TAG;
import static org.gbif.registry.service.privatesector.PrivateSectorCategories.NAMESPACE;
import static org.gbif.registry.service.privatesector.PrivateSectorCategories.SECTOR_TAG;
import static org.gbif.registry.service.privatesector.PrivateSectorCategories.SYNC_USER;

/**
 * Keeps the {@code category} of datasets in line with the private sector machine tag of their
 * publishing organization ({@code privateSector.gbif.org:sector=<value>}).
 *
 * <p>Datasets of a tagged publisher get {@code BusinessSector} plus the {@code DatasetCategory}
 * child concept of {@code BusinessSector} that the sector value resolves to through a vocabulary
 * lookup. If the value doesn't resolve, only {@code BusinessSector} is added.
 *
 * <p>Every category added here is recorded with a dataset machine tag {@code
 * privateSector.gbif.org:autoCategory=<concept>}, and only those are ever removed. Categories set
 * by hand have no marker and are left alone. If an editor removes a category that has a marker,
 * the marker is turned into {@code privateSector.gbif.org:excludedCategory=<concept>} and the
 * category is not added again. Excluding {@code BusinessSector} excludes the whole feature for
 * that dataset.
 */
@Slf4j
@Service
public class PrivateSectorCategoryService implements VocabularyPostProcessor {

  private static final int PAGE_SIZE = 100;

  private final DatasetMapper datasetMapper;
  private final OrganizationMapper organizationMapper;
  private final MachineTagMapper machineTagMapper;
  private final WithMyBatis withMyBatis;
  private final EsDatasetRealtimeIndexer esDatasetRealtimeIndexer;
  private final ConceptClient conceptClient;
  // absent in the vocabulary synchronizer CLI context
  private final TransactionTemplate transactionTemplate;

  @Autowired
  public PrivateSectorCategoryService(
      DatasetMapper datasetMapper,
      OrganizationMapper organizationMapper,
      MachineTagMapper machineTagMapper,
      WithMyBatis withMyBatis,
      EsDatasetRealtimeIndexer esDatasetRealtimeIndexer,
      ConceptClient conceptClient,
      ObjectProvider<PlatformTransactionManager> transactionManager) {
    this.datasetMapper = datasetMapper;
    this.organizationMapper = organizationMapper;
    this.machineTagMapper = machineTagMapper;
    this.withMyBatis = withMyBatis;
    this.esDatasetRealtimeIndexer = esDatasetRealtimeIndexer;
    this.conceptClient = conceptClient;

    PlatformTransactionManager tm = transactionManager.getIfAvailable();
    if (tm != null) {
      this.transactionTemplate = new TransactionTemplate(tm);
      // the sync runs after the caller's transaction committed, so it needs its own
      this.transactionTemplate.setPropagationBehavior(
          TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    } else {
      this.transactionTemplate = null;
    }
  }

  @Override
  public boolean canHandle(String vocabularyName) {
    return DATASET_CATEGORY_VOCABULARY.equals(vocabularyName);
  }

  /** A new DatasetCategory release can add or deprecate sector concepts, so re-sync everything. */
  @Override
  public int process(String vocabularyName) {
    return syncAll();
  }

  /**
   * Syncs one dataset.
   *
   * @return true if the dataset was changed
   */
  public boolean syncDataset(UUID datasetKey) {
    return inTransaction(() -> doSync(datasetKey, new HashMap<>()));
  }

  /**
   * Syncs all datasets published by the organization. Failures of single datasets are logged.
   *
   * @return the number of datasets changed
   */
  public int syncOrganization(UUID organizationKey) {
    Set<UUID> keys = new LinkedHashSet<>();
    collectDatasetKeys(
        DatasetListParams.builder().publishedByOrgKey(organizationKey), keys::add);
    return syncDatasets(keys);
  }

  /**
   * Syncs every dataset of a tagged publisher, plus every dataset that still carries an {@code
   * autoCategory} marker (e.g. its publisher lost the tag). Failures of single datasets are logged.
   *
   * @return the number of datasets changed
   */
  public int syncAll() {
    log.info("Starting private sector category sync of all datasets");
    Set<UUID> keys = new LinkedHashSet<>();

    // keys are collected first: syncing changes the marker tags the second query filters on
    for (UUID orgKey : taggedOrganizationKeys()) {
      collectDatasetKeys(DatasetListParams.builder().publishedByOrgKey(orgKey), keys::add);
    }
    collectDatasetKeys(
        DatasetListParams.builder().mtNamespace(NAMESPACE).mtName(AUTO_CATEGORY_TAG), keys::add);

    int changed = syncDatasets(keys);
    log.info(
        "Finished private sector category sync: {} datasets checked, {} changed",
        keys.size(),
        changed);
    return changed;
  }

  private int syncDatasets(Set<UUID> datasetKeys) {
    // sector value -> resolved child concept, shared by the whole run
    Map<String, Optional<String>> resolved = new HashMap<>();
    int changed = 0;
    for (UUID key : datasetKeys) {
      try {
        if (Boolean.TRUE.equals(inTransaction(() -> doSync(key, resolved)))) {
          changed++;
        }
      } catch (Exception e) {
        log.error("Private sector category sync failed for dataset {}", key, e);
      }
    }
    return changed;
  }

  private boolean doSync(UUID datasetKey, Map<String, Optional<String>> resolved) {
    Dataset dataset = datasetMapper.get(datasetKey);
    if (dataset == null || dataset.getDeleted() != null) {
      return false;
    }

    Set<String> categories =
        dataset.getCategory() != null
            ? new LinkedHashSet<>(dataset.getCategory())
            : new LinkedHashSet<>();
    List<MachineTag> markers = new ArrayList<>();
    Set<String> excluded = new LinkedHashSet<>();
    for (MachineTag tag : listMachineTags(datasetMapper, datasetKey)) {
      if (NAMESPACE.equals(tag.getNamespace()) && AUTO_CATEGORY_TAG.equals(tag.getName())) {
        markers.add(tag);
      } else if (NAMESPACE.equals(tag.getNamespace())
          && EXCLUDED_CATEGORY_TAG.equals(tag.getName())) {
        excluded.add(tag.getValue());
      }
    }

    List<MachineTag> tagsToDelete = new ArrayList<>();
    List<MachineTag> tagsToAdd = new ArrayList<>();
    boolean categoryChanged = false;

    // 1. a marked category that is gone was removed by hand: remember it as excluded
    List<MachineTag> liveMarkers = new ArrayList<>();
    for (MachineTag marker : markers) {
      if (categories.contains(marker.getValue())) {
        liveMarkers.add(marker);
      } else {
        tagsToDelete.add(marker);
        if (excluded.add(marker.getValue())) {
          tagsToAdd.add(newTag(EXCLUDED_CATEGORY_TAG, marker.getValue()));
        }
      }
    }

    Set<String> desired =
        excluded.contains(BUSINESS_SECTOR)
            ? Set.of()
            : desiredCategories(dataset.getPublishingOrganizationKey(), resolved);

    // 2. remove what we added and is no longer wanted
    for (MachineTag marker : liveMarkers) {
      if (!desired.contains(marker.getValue())) {
        categories.remove(marker.getValue());
        tagsToDelete.add(marker);
        categoryChanged = true;
      }
    }

    // 3. add what is wanted, unless excluded or already there (an unmarked one is manual)
    for (String concept : desired) {
      if (!excluded.contains(concept) && !categories.contains(concept)) {
        categories.add(concept);
        tagsToAdd.add(newTag(AUTO_CATEGORY_TAG, concept));
        categoryChanged = true;
      }
    }

    if (!categoryChanged && tagsToDelete.isEmpty() && tagsToAdd.isEmpty()) {
      return false;
    }

    // category first: if a tag write fails, an unmarked category is treated as manual and kept,
    // which is safer than a marker without a category (read as a manual removal)
    if (categoryChanged) {
      dataset.setCategory(categories.isEmpty() ? null : categories);
      dataset.setModifiedBy(SYNC_USER);
      datasetMapper.update(dataset);
    }
    for (MachineTag tag : tagsToDelete) {
      datasetMapper.deleteMachineTag(datasetKey, tag.getKey());
    }
    for (MachineTag tag : tagsToAdd) {
      withMyBatis.addMachineTag(machineTagMapper, datasetMapper, datasetKey, tag);
    }

    log.info(
        "Private sector categories of dataset {} set to {} (excluded: {})",
        datasetKey,
        categories,
        excluded);
    esDatasetRealtimeIndexer.index(datasetMapper.get(datasetKey));
    return true;
  }

  /**
   * BusinessSector plus the resolved child concept of each sector value of the publisher, or
   * nothing if the publisher has no sector tag. Throws if the vocabulary can't be reached, so a
   * dataset is never stripped of its sector concept because of an outage.
   */
  private Set<String> desiredCategories(
      UUID organizationKey, Map<String, Optional<String>> resolved) {
    Set<String> desired = new LinkedHashSet<>();
    if (organizationKey == null) {
      return desired;
    }
    for (MachineTag tag : listMachineTags(organizationMapper, organizationKey)) {
      if (NAMESPACE.equals(tag.getNamespace())
          && SECTOR_TAG.equals(tag.getName())
          && tag.getValue() != null
          && !tag.getValue().isBlank()) {
        desired.add(BUSINESS_SECTOR);
        String value = tag.getValue().trim();
        Optional<String> concept = resolved.get(normalize(value));
        if (concept == null) {
          concept = resolveSector(value);
          resolved.put(normalize(value), concept);
        }
        concept.ifPresent(desired::add);
      }
    }
    return desired;
  }

  /** The non-deprecated child of BusinessSector that the value is a name or label of, if any. */
  private Optional<String> resolveSector(String value) {
    List<LookupResult> results =
        Vocabularies.lookupLatestRelease(DATASET_CATEGORY_VOCABULARY, value, conceptClient);
    if (results != null) {
      for (LookupResult result : results) {
        String name = result.getConceptName();
        if (name == null || BUSINESS_SECTOR.equals(name)) {
          continue;
        }
        ConceptView view =
            conceptClient.getFromLatestRelease(DATASET_CATEGORY_VOCABULARY, name, true, false);
        if (view != null
            && view.getConcept() != null
            && view.getConcept().getDeprecated() == null
            && view.getParents() != null
            && view.getParents().contains(BUSINESS_SECTOR)) {
          return Optional.of(name);
        }
      }
    }
    log.warn(
        "Sector value '{}' doesn't match a {} concept under {}, only {} is used",
        value,
        DATASET_CATEGORY_VOCABULARY,
        BUSINESS_SECTOR,
        BUSINESS_SECTOR);
    return Optional.empty();
  }

  private List<UUID> taggedOrganizationKeys() {
    List<UUID> keys = new ArrayList<>();
    long offset = 0;
    List<Organization> page;
    do {
      page =
          organizationMapper.list(
              OrganizationListParams.builder()
                  .mtNamespace(NAMESPACE)
                  .mtName(SECTOR_TAG)
                  .page(new PagingRequest(offset, PAGE_SIZE))
                  .build());
      page.stream().map(Organization::getKey).forEach(keys::add);
      offset += PAGE_SIZE;
    } while (page.size() == PAGE_SIZE);
    return keys;
  }

  private void collectDatasetKeys(
      DatasetListParams.DatasetListParamsBuilder<?, ?> params, Consumer<UUID> consumer) {
    long offset = 0;
    List<Dataset> page;
    do {
      page = datasetMapper.list(params.page(new PagingRequest(offset, PAGE_SIZE)).build());
      page.stream().map(Dataset::getKey).filter(Objects::nonNull).forEach(consumer);
      offset += PAGE_SIZE;
    } while (page.size() == PAGE_SIZE);
  }

  private <T> T inTransaction(Supplier<T> work) {
    return transactionTemplate != null ? transactionTemplate.execute(s -> work.get()) : work.get();
  }

  // BaseNetworkEntityMapper extends MachineTaggableMapper as a raw type
  @SuppressWarnings("unchecked")
  private static List<MachineTag> listMachineTags(MachineTaggableMapper mapper, UUID key) {
    List<MachineTag> tags = mapper.listMachineTags(key);
    return tags != null ? tags : List.of();
  }

  private static MachineTag newTag(String name, String value) {
    MachineTag tag = new MachineTag(NAMESPACE, name, value);
    tag.setCreatedBy(SYNC_USER);
    return tag;
  }

  private static String normalize(String value) {
    return value.trim().toLowerCase(Locale.ROOT);
  }
}
