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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    Citation citation1 = new Citation();
    citation1.setCitation("New citation text");
    citation1.setDoi(new DOI("10.21373/dd.doi1"));
    citation1.setOriginalDownloadDOI(new DOI("10.21373/dl.doi1"));
    citation1.setTarget(URI.create("https://github.com/gbif/registry"));
    citation1.setTitle("Citation title");
    citation1.setCreated(new Date());
    citation1.setModified(new Date());
    citation1.setCreatedBy("IT");
    citation1.setModifiedBy("IT");
    mapper.create(citation1);

    Citation citation2 = new Citation();
    citation2.setCitation("New citation text");
    citation2.setDoi(new DOI("10.21373/dd.doi2"));
    citation2.setOriginalDownloadDOI(new DOI("10.21373/dl.doi2"));
    citation2.setTarget(URI.create("https://github.com/gbif/registry"));
    citation2.setTitle("Citation title");
    citation2.setCreated(new Date());
    citation2.setModified(new Date());
    citation2.setCreatedBy("IT");
    citation2.setModifiedBy("IT");
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
}
