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
package org.gbif.registry.ws.it.persistence.mapper;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.registry.Dataset;
import org.gbif.registry.domain.ws.DerivedDataset;
import org.gbif.registry.domain.ws.DerivedDatasetUsage;
import org.gbif.registry.persistence.mapper.DerivedDatasetMapper;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DerivedDatasetMapperIT extends BaseItTest {

  private final DerivedDatasetMapper mapper;
  private final TestDataFactory testDataFactory;

  @Autowired
  public DerivedDatasetMapperIT(
      DerivedDatasetMapper mapper,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer,
      TestDataFactory testDataFactory) {
    super(principalProvider, esServer);
    this.mapper = mapper;
    this.testDataFactory = testDataFactory;
  }

  @Test
  public void testAddDatasetCitations() {
    // create datasets
    Dataset dataset1 = testDataFactory.newPersistedDataset(new DOI("10.21373/dataset1"));
    Dataset dataset2 = testDataFactory.newPersistedDataset(new DOI("10.21373/dataset2"));
    Dataset dataset3 = testDataFactory.newPersistedDataset(new DOI("10.21373/dataset3"));

    // create citations
    DerivedDataset derivedDataset1 = prepareDerivedDataset();
    derivedDataset1.setDoi(new DOI("10.21373/dd.doi1"));
    derivedDataset1.setOriginalDownloadDOI(new DOI("10.21373/dl.doi1"));
    mapper.create(derivedDataset1);

    DerivedDataset derivedDataset2 = prepareDerivedDataset();
    derivedDataset2.setDoi(new DOI("10.21373/dd.doi2"));
    derivedDataset2.setOriginalDownloadDOI(new DOI("10.21373/dl.doi2"));
    mapper.create(derivedDataset2);

    // create citation datasets
    List<DerivedDatasetUsage> citationDatasets1 = new ArrayList<>();

    citationDatasets1.add(
        new DerivedDatasetUsage(
            dataset1.getKey(), dataset1.getDoi(), null, 1L, dataset1.getTitle()));
    citationDatasets1.add(
        new DerivedDatasetUsage(
            dataset2.getKey(), dataset2.getDoi(), null, 2L, dataset2.getTitle()));
    citationDatasets1.add(
        new DerivedDatasetUsage(dataset3.getKey(), null, null, 3L, dataset3.getTitle()));

    List<DerivedDatasetUsage> citationDatasets2 = new ArrayList<>();
    citationDatasets2.add(
        new DerivedDatasetUsage(
            dataset3.getKey(), dataset3.getDoi(), null, 3L, dataset3.getTitle()));

    mapper.addUsagesToDerivedDataset(derivedDataset1.getDoi(), citationDatasets1);
    mapper.addUsagesToDerivedDataset(derivedDataset2.getDoi(), citationDatasets2);

    // test methods
    List<DerivedDatasetUsage> usages =
        mapper.listDerivedDatasetUsages(derivedDataset2.getDoi(), new PagingRequest());
    assertNotNull(usages);
    assertEquals(1, usages.size());
    assertNotNull(usages.get(0).getCitation());

    usages = mapper.listDerivedDatasetUsages(derivedDataset1.getDoi(), new PagingRequest());
    assertNotNull(usages);
    assertEquals(3, usages.size());
    assertNotNull(usages.get(0).getCitation());
  }

  @Test
  public void testListByRegistrationDate() {
    // create derivedDatasets
    DerivedDataset derivedDataset1 = prepareDerivedDataset();
    derivedDataset1.setDoi(new DOI("10.21373/dd.doi1"));
    // day before date
    derivedDataset1.setRegistrationDate(
        Date.from(
            LocalDateTime.of(2020, Month.APRIL, 19, 20, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()));
    mapper.create(derivedDataset1);

    DerivedDataset derivedDataset2 = prepareDerivedDataset();
    derivedDataset2.setDoi(new DOI("10.21373/dd.doi2"));
    // the same date but an hour later
    derivedDataset2.setRegistrationDate(
        Date.from(
            LocalDateTime.of(2020, Month.APRIL, 20, 20, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()));
    mapper.create(derivedDataset2);

    DerivedDataset derivedDataset3 = prepareDerivedDataset();
    derivedDataset3.setDoi(new DOI("10.21373/dd.doi3"));
    // day after date
    derivedDataset3.setRegistrationDate(
        Date.from(
            LocalDateTime.of(2020, Month.APRIL, 21, 20, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()));
    mapper.create(derivedDataset3);

    List<DerivedDataset> derivedDatasets =
        mapper.listByRegistrationDate(
            Date.from(
                LocalDateTime.of(2020, Month.APRIL, 20, 19, 0)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()));
    assertNotNull(derivedDatasets);
    assertEquals(1, derivedDatasets.size());
  }

  private DerivedDataset prepareDerivedDataset() {
    DerivedDataset derivedDataset = new DerivedDataset();
    derivedDataset.setCreated(new Date());
    derivedDataset.setModified(new Date());
    derivedDataset.setDescription("DerivedDataset description");
    derivedDataset.setCreatedBy("WS_IT");
    derivedDataset.setModifiedBy("WS_IT");
    derivedDataset.setSourceUrl(URI.create("https://github.com/gbif/registry"));
    derivedDataset.setTitle("DerivedDataset title");
    derivedDataset.setCitation("New derivedDataset text");

    return derivedDataset;
  }
}
