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
import org.gbif.registry.domain.ws.Citation;
import org.gbif.registry.domain.ws.CitationDatasetUsage;
import org.gbif.registry.persistence.mapper.CitationMapper;
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

public class CitationMapperIT extends BaseItTest {

  private final CitationMapper mapper;
  private final TestDataFactory testDataFactory;

  @Autowired
  public CitationMapperIT(
      CitationMapper mapper,
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
    Citation citation1 = prepareCitation();
    citation1.setDoi(new DOI("10.21373/dd.doi1"));
    citation1.setOriginalDownloadDOI(new DOI("10.21373/dl.doi1"));
    mapper.create(citation1);

    Citation citation2 = prepareCitation();
    citation2.setDoi(new DOI("10.21373/dd.doi2"));
    citation2.setOriginalDownloadDOI(new DOI("10.21373/dl.doi2"));
    mapper.create(citation2);

    // create citation datasets
    List<CitationDatasetUsage> citationDatasets1 = new ArrayList<>();

    citationDatasets1.add(new CitationDatasetUsage(dataset1.getKey(), dataset1.getDoi(), 1L));
    citationDatasets1.add(new CitationDatasetUsage(dataset2.getKey(), dataset2.getDoi(), 2L));
    citationDatasets1.add(new CitationDatasetUsage(dataset3.getKey(), null, 3L));

    List<CitationDatasetUsage> citationDatasets2 = new ArrayList<>();
    citationDatasets2.add(new CitationDatasetUsage(dataset3.getKey(), dataset3.getDoi(), 3L));

    mapper.addCitationDatasets(citation1.getDoi(), citationDatasets1);
    mapper.addCitationDatasets(citation2.getDoi(), citationDatasets2);

    // test methods
    List<Dataset> datasets = mapper.listByCitation(citation2.getDoi(), new PagingRequest());
    assertNotNull(datasets);
    assertEquals(1, datasets.size());

    datasets = mapper.listByCitation(citation1.getDoi(), new PagingRequest());
    assertNotNull(datasets);
    assertEquals(3, datasets.size());
  }

  @Test
  public void testListByRegistrationDate() {
    // create citations
    Citation citation1 = prepareCitation();
    citation1.setDoi(new DOI("10.21373/dd.doi1"));
    // day before date
    citation1.setRegistrationDate(
        Date.from(
            LocalDateTime.of(2020, Month.APRIL, 19, 20, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()));
    mapper.create(citation1);

    Citation citation2 = prepareCitation();
    citation2.setDoi(new DOI("10.21373/dd.doi2"));
    // the same date but an hour later
    citation2.setRegistrationDate(
        Date.from(
            LocalDateTime.of(2020, Month.APRIL, 20, 20, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()));
    mapper.create(citation2);

    Citation citation3 = prepareCitation();
    citation3.setDoi(new DOI("10.21373/dd.doi3"));
    // day after date
    citation3.setRegistrationDate(
        Date.from(
            LocalDateTime.of(2020, Month.APRIL, 21, 20, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()));
    mapper.create(citation3);

    List<Citation> citations =
        mapper.listByRegistrationDate(
            Date.from(
                LocalDateTime.of(2020, Month.APRIL, 20, 19, 0)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()));
    assertNotNull(citations);
    assertEquals(1, citations.size());
  }

  private Citation prepareCitation() {
    Citation citation = new Citation();
    citation.setCreated(new Date());
    citation.setModified(new Date());
    citation.setCreatedBy("WS_IT");
    citation.setModifiedBy("WS_IT");
    citation.setTarget(URI.create("https://github.com/gbif/registry"));
    citation.setTitle("Citation title");
    citation.setCitation("New citation text");

    return citation;
  }
}
