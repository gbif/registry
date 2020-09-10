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
package org.gbif.registry.ws.it;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.Dataset;
import org.gbif.registry.domain.ws.Citation;
import org.gbif.registry.domain.ws.CitationCreationRequest;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.resources.CitationResource;
import org.gbif.registry.ws.resources.OccurrenceDownloadResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static java.util.stream.Collectors.toMap;
import static org.gbif.registry.ws.it.OccurrenceDownloadIT.getTestInstancePredicateDownload;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CitationIT extends BaseItTest {

  private static final PagingRequest REGULAR_PAGE = new PagingRequest();

  private final CitationResource citationResource;
  private final TestDataFactory testDataFactory;
  private final OccurrenceDownloadResource occurrenceDownloadService;

  @Autowired
  public CitationIT(
      CitationResource citationResource,
      @Nullable SimplePrincipalProvider simplePrincipalProvider,
      EsManageServer esServer,
      TestDataFactory testDataFactory,
      OccurrenceDownloadResource occurrenceDownloadService) {
    super(simplePrincipalProvider, esServer);
    this.citationResource = citationResource;
    this.testDataFactory = testDataFactory;
    this.occurrenceDownloadService = occurrenceDownloadService;
  }

  @Test
  public void testCreateCitation() {
    // create download
    Download occurrenceDownload = getTestInstancePredicateDownload();
    occurrenceDownloadService.create(occurrenceDownload);
    occurrenceDownload = occurrenceDownloadService.get(occurrenceDownload.getKey());
    assertNotNull(occurrenceDownload);

    // create datasets
    Dataset firstDataset = testDataFactory.newPersistedDataset(new DOI("10.21373/dataset1"));
    Dataset secondDataset = testDataFactory.newPersistedDataset(new DOI("10.21373/dataset2"));

    // prepare request
    CitationCreationRequest requestData = new CitationCreationRequest();
    requestData.setTitle("Let's test citation");
    requestData.setOriginalDownloadDOI(occurrenceDownload.getDoi());
    requestData.setTarget(URI.create("https://www.gbif.org"));
    Map<String, Long> relatedDatasets = new HashMap<>();
    relatedDatasets.put(firstDataset.getKey().toString(), 1L);
    relatedDatasets.put(secondDataset.getDoi().getDoiName(), 2L);
    requestData.setRelatedDatasets(relatedDatasets);
    Citation citation = citationResource.createCitation(requestData);

    // create citation
    Citation actual =
        citationResource.getCitation(citation.getDoi().getPrefix(), citation.getDoi().getSuffix());

    assertNotNull(actual);
    assertEquals(citation.getDoi(), actual.getDoi());
    assertEquals(requestData.getOriginalDownloadDOI(), actual.getOriginalDownloadDOI());
    assertEquals(requestData.getTarget(), actual.getTarget());
    assertEquals(requestData.getTitle(), actual.getTitle());
    assertNotNull(actual.getCreated());
    assertNotNull(actual.getCreatedBy());
    assertNotNull(actual.getModified());
    assertNotNull(actual.getModifiedBy());
  }

  @Test
  public void testDatasetCitation() {
    // create download
    Download occurrenceDownload = getTestInstancePredicateDownload();
    occurrenceDownloadService.create(occurrenceDownload);
    occurrenceDownload = occurrenceDownloadService.get(occurrenceDownload.getKey());
    assertNotNull(occurrenceDownload);

    // create datasets
    Dataset firstDataset = testDataFactory.newPersistedDataset(new DOI("10.21373/dataset1"));
    Dataset secondDataset = testDataFactory.newPersistedDataset(new DOI("10.21373/dataset2"));
    Dataset thirdDataset = testDataFactory.newPersistedDataset(new DOI("10.21373/dataset3"));

    // prepare requests
    CitationCreationRequest requestData1 = newCitationRequest(occurrenceDownload.getDoi(), new HashMap<>());
    String str = secondDataset.getKey() + ",1\n"
        + firstDataset.getDoi() + ",2";
    MultipartFile relatedDatasetsFile = new MockMultipartFile("file.csv", str.getBytes());

    CitationCreationRequest requestData2 =
        newCitationRequest(
            occurrenceDownload.getDoi(),
            Stream.of(
                    new String[][] {
                      {secondDataset.getDoi().getDoiName(), "1"},
                      {firstDataset.getKey().toString(), "2"},
                    })
                .collect(toMap(data -> data[0], data -> Long.valueOf(data[1]))));

    CitationCreationRequest requestData3 =
        newCitationRequest(
            occurrenceDownload.getDoi(),
            Stream.of(new String[][] {{thirdDataset.getDoi().getDoiName(), "1"}})
                .collect(toMap(data -> data[0], data -> Long.valueOf(data[1]))));

    // create citations
    Citation citation1 = citationResource.createCitation(requestData1, relatedDatasetsFile);
    Citation citation2 = citationResource.createCitation(requestData2);
    Citation citation3 = citationResource.createCitation(requestData3);

    PagingResponse<Dataset> citationDatasetsPage1 =
        citationResource.getCitationDatasets(citation1.getDoi(), REGULAR_PAGE);
    assertNotNull(citationDatasetsPage1);
    assertEquals(2, citationDatasetsPage1.getCount());

    PagingResponse<Dataset> citationDatasetsPage2 =
        citationResource.getCitationDatasets(citation3.getDoi(), REGULAR_PAGE);
    assertNotNull(citationDatasetsPage2);
    assertEquals(1, citationDatasetsPage2.getCount());

    PagingResponse<Citation> datasetCitationPage1 =
        citationResource.getDatasetCitations(firstDataset.getKey(), REGULAR_PAGE);
    assertNotNull(datasetCitationPage1);
    assertEquals(2, datasetCitationPage1.getCount());

    PagingResponse<Citation> datasetCitationPage2 =
        citationResource.getDatasetCitations(thirdDataset.getKey(), REGULAR_PAGE);
    assertNotNull(datasetCitationPage2);
    assertEquals(1, datasetCitationPage2.getCount());
  }

  private CitationCreationRequest newCitationRequest(
      DOI originalDownloadDOI, Map<String, Long> relatedDatasets) {
    CitationCreationRequest creationRequest = new CitationCreationRequest();
    creationRequest.setTitle("Let's test citation");
    creationRequest.setOriginalDownloadDOI(originalDownloadDOI);
    creationRequest.setTarget(URI.create("https://www.gbif.org"));
    creationRequest.setRelatedDatasets(relatedDatasets);

    return creationRequest;
  }
}
