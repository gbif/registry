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

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.search.dataset.indexing.EsDatasetRealtimeIndexer;
import org.gbif.registry.service.WithMyBatis;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.client.ConceptClient;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.search.LookupResult;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;

import static org.gbif.registry.service.privatesector.PrivateSectorCategories.AUTO_CATEGORY_TAG;
import static org.gbif.registry.service.privatesector.PrivateSectorCategories.BUSINESS_SECTOR;
import static org.gbif.registry.service.privatesector.PrivateSectorCategories.DATASET_CATEGORY_VOCABULARY;
import static org.gbif.registry.service.privatesector.PrivateSectorCategories.EXCLUDED_CATEGORY_TAG;
import static org.gbif.registry.service.privatesector.PrivateSectorCategories.NAMESPACE;
import static org.gbif.registry.service.privatesector.PrivateSectorCategories.SECTOR_TAG;
import static org.gbif.registry.service.privatesector.PrivateSectorCategories.SYNC_USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrivateSectorCategoryServiceTest {

  private static final UUID DATASET_KEY = UUID.randomUUID();
  private static final UUID ORG_KEY = UUID.randomUUID();

  @Mock private DatasetMapper datasetMapper;
  @Mock private OrganizationMapper organizationMapper;
  @Mock private MachineTagMapper machineTagMapper;
  @Mock private WithMyBatis withMyBatis;
  @Mock private EsDatasetRealtimeIndexer indexer;
  @Mock private ConceptClient conceptClient;

  private PrivateSectorCategoryService service;

  /** Machine tags currently on the dataset, kept in sync with the mocked writes. */
  private final List<MachineTag> datasetTags = new ArrayList<>();

  private final AtomicInteger tagKeys = new AtomicInteger(1);
  private Dataset dataset;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    ObjectProvider<PlatformTransactionManager> noTm = mock(ObjectProvider.class);
    service =
        new PrivateSectorCategoryService(
            datasetMapper,
            organizationMapper,
            machineTagMapper,
            withMyBatis,
            indexer,
            conceptClient,
            noTm);

    dataset = new Dataset();
    dataset.setKey(DATASET_KEY);
    dataset.setPublishingOrganizationKey(ORG_KEY);

    lenient().when(datasetMapper.get(DATASET_KEY)).thenAnswer(i -> dataset);
    lenient()
        .when(datasetMapper.listMachineTags(DATASET_KEY))
        .thenAnswer(i -> new ArrayList<>(datasetTags));
    lenient()
        .when(withMyBatis.addMachineTag(any(), any(), eq(DATASET_KEY), any(MachineTag.class)))
        .thenAnswer(
            i -> {
              MachineTag tag = i.getArgument(3);
              tag.setKey(tagKeys.getAndIncrement());
              datasetTags.add(tag);
              return tag.getKey();
            });
    lenient()
        .when(datasetMapper.deleteMachineTag(eq(DATASET_KEY), anyInt()))
        .thenAnswer(
            i -> {
              int key = i.getArgument(1);
              datasetTags.removeIf(t -> t.getKey() == key);
              return 1;
            });
  }

  @Test
  void addsBusinessSectorAndResolvedChild() {
    publisherSector("Consulting");
    childConcept("Consulting", "Consulting");

    assertTrue(service.syncDataset(DATASET_KEY));

    assertEquals(Set.of(BUSINESS_SECTOR, "Consulting"), dataset.getCategory());
    assertEquals(Set.of(BUSINESS_SECTOR, "Consulting"), tagValues(AUTO_CATEGORY_TAG));
    assertEquals(SYNC_USER, dataset.getModifiedBy());
    datasetTags.forEach(t -> assertEquals(SYNC_USER, t.getCreatedBy()));
    verify(datasetMapper).update(dataset);
    verify(indexer).index(dataset);
  }

  @Test
  void secondRunChangesNothing() {
    publisherSector("Consulting");
    childConcept("Consulting", "Consulting");
    service.syncDataset(DATASET_KEY);

    assertFalse(service.syncDataset(DATASET_KEY));
    verify(datasetMapper).update(any()); // only from the first run
  }

  @Test
  void unmatchedValueFallsBackToBusinessSector() {
    publisherSector("Something else");
    when(conceptClient.lookupInLatestRelease(eq(DATASET_CATEGORY_VOCABULARY), any()))
        .thenReturn(List.of());

    service.syncDataset(DATASET_KEY);

    assertEquals(Set.of(BUSINESS_SECTOR), dataset.getCategory());
  }

  @Test
  void matchOutsideBusinessSectorFallsBack() {
    publisherSector("Specimen");
    lookup("Specimen", "Specimen");
    when(conceptClient.getFromLatestRelease(DATASET_CATEGORY_VOCABULARY, "Specimen", true, false))
        .thenReturn(view("Specimen", null, List.of()));

    service.syncDataset(DATASET_KEY);

    assertEquals(Set.of(BUSINESS_SECTOR), dataset.getCategory());
  }

  @Test
  void deprecatedChildFallsBack() {
    publisherSector("Mining");
    lookup("Mining", "Mining");
    when(conceptClient.getFromLatestRelease(DATASET_CATEGORY_VOCABULARY, "Mining", true, false))
        .thenReturn(view("Mining", ZonedDateTime.now(), List.of(BUSINESS_SECTOR)));

    service.syncDataset(DATASET_KEY);

    assertEquals(Set.of(BUSINESS_SECTOR), dataset.getCategory());
  }

  @Test
  void vocabularyErrorLeavesDatasetUntouched() {
    publisherSector("Consulting");
    lookup("Consulting", "Consulting");
    when(conceptClient.getFromLatestRelease(DATASET_CATEGORY_VOCABULARY, "Consulting", true, false))
        .thenThrow(new RuntimeException("vocabulary down"));
    dataset.setCategory(set(BUSINESS_SECTOR, "Consulting"));
    marker(AUTO_CATEGORY_TAG, BUSINESS_SECTOR);
    marker(AUTO_CATEGORY_TAG, "Consulting");

    assertThrows(RuntimeException.class, () -> service.syncDataset(DATASET_KEY));

    assertEquals(Set.of(BUSINESS_SECTOR, "Consulting"), dataset.getCategory());
    verify(datasetMapper, never()).update(any());
  }

  @Test
  void manualCategoryIsKeptWithoutMarker() {
    publisherSector("Consulting");
    childConcept("Consulting", "Consulting");
    dataset.setCategory(set(BUSINESS_SECTOR));

    service.syncDataset(DATASET_KEY);

    assertEquals(Set.of(BUSINESS_SECTOR, "Consulting"), dataset.getCategory());
    assertEquals(Set.of("Consulting"), tagValues(AUTO_CATEGORY_TAG));
  }

  @Test
  void publisherWithoutTagRemovesOnlyAutoCategories() {
    // publisher has no sector tag any more
    when(organizationMapper.listMachineTags(ORG_KEY)).thenReturn(List.of());
    dataset.setCategory(set(BUSINESS_SECTOR, "Consulting", "CitizenScience"));
    marker(AUTO_CATEGORY_TAG, BUSINESS_SECTOR);
    marker(AUTO_CATEGORY_TAG, "Consulting");

    assertTrue(service.syncDataset(DATASET_KEY));

    assertEquals(Set.of("CitizenScience"), dataset.getCategory());
    assertTrue(tagValues(AUTO_CATEGORY_TAG).isEmpty());
  }

  @Test
  void manualBusinessSectorOnUntaggedPublisherIsKept() {
    when(organizationMapper.listMachineTags(ORG_KEY)).thenReturn(List.of());
    dataset.setCategory(set(BUSINESS_SECTOR));

    assertFalse(service.syncDataset(DATASET_KEY));
    assertEquals(Set.of(BUSINESS_SECTOR), dataset.getCategory());
  }

  @Test
  void manualRemovalBecomesExclusionAndIsNotReAdded() {
    publisherSector("Consulting");
    childConcept("Consulting", "Consulting");
    service.syncDataset(DATASET_KEY);

    // an editor removes the child concept
    dataset.setCategory(set(BUSINESS_SECTOR));
    service.syncDataset(DATASET_KEY);

    assertEquals(Set.of(BUSINESS_SECTOR), dataset.getCategory());
    assertEquals(Set.of("Consulting"), tagValues(EXCLUDED_CATEGORY_TAG));
    assertEquals(Set.of(BUSINESS_SECTOR), tagValues(AUTO_CATEGORY_TAG));

    // and later syncs don't bring it back
    assertFalse(service.syncDataset(DATASET_KEY));
    assertEquals(Set.of(BUSINESS_SECTOR), dataset.getCategory());
  }

  @Test
  void excludingBusinessSectorRemovesAutoChildren() {
    publisherSector("Consulting");
    childConcept("Consulting", "Consulting");
    service.syncDataset(DATASET_KEY);

    // an editor removes BusinessSector but leaves the child
    dataset.setCategory(set("Consulting"));
    service.syncDataset(DATASET_KEY);

    assertNull(dataset.getCategory());
    assertEquals(Set.of(BUSINESS_SECTOR), tagValues(EXCLUDED_CATEGORY_TAG));
    assertTrue(tagValues(AUTO_CATEGORY_TAG).isEmpty());
  }

  @Test
  void deletedDatasetIsSkipped() {
    dataset.setDeleted(new java.util.Date());

    assertFalse(service.syncDataset(DATASET_KEY));
    verify(datasetMapper, never()).update(any());
  }

  @Test
  void handlesOnlyDatasetCategoryVocabulary() {
    assertTrue(service.canHandle(DATASET_CATEGORY_VOCABULARY));
    assertFalse(service.canHandle("Discipline"));
  }

  // --- helpers ---

  private void publisherSector(String value) {
    when(organizationMapper.listMachineTags(ORG_KEY))
        .thenReturn(List.of(new MachineTag(NAMESPACE, SECTOR_TAG, value)));
  }

  private void lookup(String query, String conceptName) {
    LookupResult result = new LookupResult();
    result.setConceptName(conceptName);
    when(conceptClient.lookupInLatestRelease(
            eq(DATASET_CATEGORY_VOCABULARY),
            org.mockito.ArgumentMatchers.argThat(p -> p != null && query.equals(p.getQ()))))
        .thenReturn(List.of(result));
  }

  private void childConcept(String query, String conceptName) {
    lookup(query, conceptName);
    when(conceptClient.getFromLatestRelease(DATASET_CATEGORY_VOCABULARY, conceptName, true, false))
        .thenReturn(view(conceptName, null, List.of(BUSINESS_SECTOR)));
  }

  private static ConceptView view(String name, ZonedDateTime deprecated, List<String> parents) {
    Concept concept = new Concept();
    concept.setName(name);
    concept.setDeprecated(deprecated);
    return new ConceptView(concept).setParents(parents);
  }

  private void marker(String name, String value) {
    MachineTag tag = new MachineTag(NAMESPACE, name, value);
    tag.setKey(tagKeys.getAndIncrement());
    datasetTags.add(tag);
  }

  private Set<String> tagValues(String name) {
    return datasetTags.stream()
        .filter(t -> NAMESPACE.equals(t.getNamespace()) && name.equals(t.getName()))
        .map(MachineTag::getValue)
        .collect(Collectors.toSet());
  }

  private static Set<String> set(String... values) {
    return new LinkedHashSet<>(Arrays.asList(values));
  }
}
