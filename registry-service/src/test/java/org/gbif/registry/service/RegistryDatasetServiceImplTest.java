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

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.License;
import org.gbif.registry.domain.ws.DerivedDatasetUsage;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.params.DatasetListParams;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistryDatasetServiceImplTest {

  private final String UUID_1_STR = "97ade88b-042b-4a56-94a4-165c970c5506";
  private final UUID UUID_1 = UUID.fromString(UUID_1_STR);
  private final UUID UUID_2 = UUID.fromString("7b342436-9e66-4683-9c2d-da9c546eaba9");
  private final String DOI_1_STR = "10.21373/12345";
  private final DOI DOI_1 = new DOI("10.21373/12345");
  private final DOI DOI_2 = new DOI("10.21373/54321");

  @Mock private DatasetMapper datasetMapper;
  @InjectMocks private RegistryDatasetServiceImpl registryDatasetService;

  @Test
  public void testEnsureDerivedDatasetDatasetUsagesValidDuplicates() {
    // given
    Dataset dataset = prepareDataset(UUID_1, DOI_1);
    Map<String, Long> datasetUsages = new HashMap<>();
    datasetUsages.put(UUID_1_STR, 1L);
    datasetUsages.put(DOI_1_STR, 2L);
    when(datasetMapper.get(UUID_1)).thenReturn(dataset);
    when(datasetMapper.list(any(DatasetListParams.class)))
        .thenReturn(Collections.singletonList(dataset));

    // when & then
    assertThrows(
        IllegalArgumentException.class,
        () -> registryDatasetService.ensureDerivedDatasetDatasetUsagesValid(datasetUsages));
    verify(datasetMapper).get(UUID_1);
    verify(datasetMapper).list(any(DatasetListParams.class));
  }

  @Test
  public void testEnsureDerivedDatasetDatasetUsagesValidNoDatasetWithUuid() {
    // given
    Map<String, Long> datasetUsages = new HashMap<>();
    datasetUsages.put(UUID_1_STR, 1L); // no dataset with this UUID
    datasetUsages.put(DOI_1_STR, 2L);

    // when & then
    assertThrows(
        IllegalArgumentException.class,
        () -> registryDatasetService.ensureDerivedDatasetDatasetUsagesValid(datasetUsages));
    verify(datasetMapper).get(UUID_1);
  }

  @Test
  public void testEnsureDerivedDatasetDatasetUsagesValidNoDatasetWithDoi() {
    // given
    Map<String, Long> datasetUsages = new HashMap<>();
    datasetUsages.put(DOI_1_STR, 2L); // no dataset with this DOI

    // when & then
    assertThrows(
        IllegalArgumentException.class,
        () -> registryDatasetService.ensureDerivedDatasetDatasetUsagesValid(datasetUsages));
  }

  @Test
  public void testEnsureDerivedDatasetDatasetUsagesValidWrongInvalidUuid() {
    // given
    Map<String, Long> datasetUsages = new HashMap<>();
    datasetUsages.put("1111-1111", 1L);

    // when & then
    assertThrows(
        IllegalArgumentException.class,
        () -> registryDatasetService.ensureDerivedDatasetDatasetUsagesValid(datasetUsages));
  }

  @Test
  public void testEnsureDerivedDatasetDatasetUsagesValid() {
    // given
    Dataset dataset1 = prepareDataset(UUID_1, DOI_2);
    Dataset dataset2 = prepareDataset(UUID_2, DOI_1);
    Map<String, Long> datasetUsages = new HashMap<>();
    datasetUsages.put(UUID_1_STR, 1L);
    datasetUsages.put(DOI_1_STR, 2L);
    when(datasetMapper.get(UUID_1)).thenReturn(dataset1);
    when(datasetMapper.list(any(DatasetListParams.class)))
        .thenReturn(Collections.singletonList(dataset2));

    // when
    List<DerivedDatasetUsage> derivedDatasetUsages =
        registryDatasetService.ensureDerivedDatasetDatasetUsagesValid(datasetUsages);

    // then
    assertFalse(derivedDatasetUsages.isEmpty());
    assertEquals(2, derivedDatasetUsages.size());
    verify(datasetMapper).get(UUID_1);
    verify(datasetMapper).list(any(DatasetListParams.class));
  }

  private Dataset prepareDataset(UUID key, DOI doi) {
    Dataset dataset = new Dataset();
    dataset.setKey(key);
    dataset.setLicense(License.CC_BY_NC_4_0);
    dataset.setTitle("Title");
    dataset.setDoi(doi);

    return dataset;
  }
}
