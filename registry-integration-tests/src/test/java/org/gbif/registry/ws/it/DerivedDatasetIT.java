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
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.domain.ws.DerivedDataset;
import org.gbif.registry.domain.ws.DerivedDatasetCreationRequest;
import org.gbif.registry.domain.ws.DerivedDatasetUpdateRequest;
import org.gbif.registry.domain.ws.DerivedDatasetUsage;
import org.gbif.registry.persistence.mapper.DerivedDatasetMapper;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.it.fixtures.RequestTestFixture;
import org.gbif.registry.ws.resources.DerivedDatasetResource;
import org.gbif.registry.ws.resources.OccurrenceDownloadResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.multipart.MultipartFile;

import static java.util.stream.Collectors.toMap;
import static org.gbif.registry.ws.it.OccurrenceDownloadIT.getTestInstancePredicateDownload;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DerivedDatasetIT extends BaseItTest {

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = TestCaseDatabaseInitializer.builder()
    .dataSource(database.getTestDatabase())
    .build();

  private static final PagingRequest REGULAR_PAGE = new PagingRequest();

  private final RequestTestFixture requestTestFixture;
  private final DerivedDatasetResource derivedDatasetResource;
  private final DerivedDatasetMapper derivedDatasetMapper;
  private final TestDataFactory testDataFactory;
  private final OccurrenceDownloadResource occurrenceDownloadService;

  @Autowired
  public DerivedDatasetIT(
      DerivedDatasetResource derivedDatasetResource,
      @Nullable SimplePrincipalProvider simplePrincipalProvider,
      EsManageServer esServer,
      RequestTestFixture requestTestFixture,
      DerivedDatasetMapper derivedDatasetMapper,
      TestDataFactory testDataFactory,
      OccurrenceDownloadResource occurrenceDownloadService) {
    super(simplePrincipalProvider, esServer);
    this.derivedDatasetResource = derivedDatasetResource;
    this.requestTestFixture = requestTestFixture;
    this.derivedDatasetMapper = derivedDatasetMapper;
    this.testDataFactory = testDataFactory;
    this.occurrenceDownloadService = occurrenceDownloadService;
  }

  @Test
  public void testCreateUpdateDerivedDataset() {
    // create download
    Download occurrenceDownload = getTestInstancePredicateDownload();
    occurrenceDownloadService.create(occurrenceDownload);
    occurrenceDownload = occurrenceDownloadService.get(occurrenceDownload.getKey());
    assertNotNull(occurrenceDownload);

    // create datasets
    Dataset firstDataset = testDataFactory.newPersistedDataset(new DOI("10.21373/dataset1"));
    Dataset secondDataset = testDataFactory.newPersistedDataset(new DOI("10.21373/dataset2"));

    // prepare request
    DerivedDatasetCreationRequest requestData = new DerivedDatasetCreationRequest();
    requestData.setTitle("Let's test derivedDataset");
    requestData.setOriginalDownloadDOI(occurrenceDownload.getDoi());
    requestData.setSourceUrl(URI.create("https://www.gbif.org"));
    Map<String, Long> relatedDatasets = new HashMap<>();
    relatedDatasets.put(firstDataset.getKey().toString(), 1L);
    relatedDatasets.put(secondDataset.getDoi().getDoiName(), 2L);
    requestData.setRelatedDatasets(relatedDatasets);
    // create derivedDataset
    DerivedDataset derivedDataset = derivedDatasetResource.create(requestData);

    // check created
    DerivedDataset created = derivedDatasetResource.getDerivedDataset(derivedDataset.getDoi());

    assertNotNull(created);
    assertEquals(derivedDataset.getDoi(), created.getDoi());
    assertEquals(requestData.getOriginalDownloadDOI(), created.getOriginalDownloadDOI());
    assertEquals(requestData.getDescription(), created.getDescription());
    assertEquals(requestData.getSourceUrl(), created.getSourceUrl());
    assertEquals(requestData.getTitle(), created.getTitle());
    assertNotNull(created.getCreated());
    assertNotNull(created.getCreatedBy());
    assertNotNull(created.getModified());
    assertNotNull(created.getModifiedBy());

    // Try update
    DerivedDatasetUpdateRequest updateRequest = new DerivedDatasetUpdateRequest();
    updateRequest.setTitle("Updated title");
    updateRequest.setDescription("Updated description");
    updateRequest.setSourceUrl(URI.create("gbif.org"));
    // update derivedDataset
    derivedDatasetResource.update(derivedDataset.getDoi(), updateRequest);

    // check updated
    DerivedDataset updated = derivedDatasetResource.getDerivedDataset(derivedDataset.getDoi());
    assertEquals("Updated title", updated.getTitle());
    assertEquals("Updated description", updated.getDescription());
    assertNotEquals(created.getSourceUrl(), updated.getSourceUrl());
    assertEquals(created.getCreated(), updated.getCreated());
    assertEquals(created.getCreatedBy(), updated.getCreatedBy());
    assertNotEquals(created.getModified(), updated.getModified());
  }

  @Test
  public void testDerivedDatasetDatasetUsages() {
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
    DerivedDatasetCreationRequest requestData1 =
        newDerivedDatasetCreationRequest(occurrenceDownload.getDoi(), new HashMap<>());
    String str = secondDataset.getKey() + ",1\n" + firstDataset.getDoi() + ",2";
    MultipartFile relatedDatasetsFile = new MockMultipartFile("file.csv", str.getBytes(StandardCharsets.UTF_8));

    DerivedDatasetCreationRequest requestData2 =
        newDerivedDatasetCreationRequest(
            occurrenceDownload.getDoi(),
            Stream.of(
                    new String[][] {
                      {secondDataset.getDoi().getDoiName(), "1"},
                      {firstDataset.getKey().toString(), "2"},
                    })
                .collect(toMap(data -> data[0], data -> Long.valueOf(data[1]))));

    DerivedDatasetCreationRequest requestData3 =
        newDerivedDatasetCreationRequest(
            occurrenceDownload.getDoi(),
            Stream.of(new String[][] {{thirdDataset.getDoi().getDoiName(), "1"}})
                .collect(toMap(data -> data[0], data -> Long.valueOf(data[1]))));

    // create citations
    DerivedDataset derivedDataset1 =
        derivedDatasetResource.create(requestData1, relatedDatasetsFile);
    DerivedDataset derivedDataset2 = derivedDatasetResource.create(requestData2);
    DerivedDataset derivedDataset3 = derivedDatasetResource.create(requestData3);

    PagingResponse<DerivedDatasetUsage> citationDatasetsPage1 =
        derivedDatasetResource.getRelatedDatasets(derivedDataset1.getDoi(), REGULAR_PAGE);
    assertNotNull(citationDatasetsPage1);
    assertEquals(2, citationDatasetsPage1.getCount());

    PagingResponse<DerivedDatasetUsage> citationDatasetsPage2 =
        derivedDatasetResource.getRelatedDatasets(derivedDataset3.getDoi(), REGULAR_PAGE);
    assertNotNull(citationDatasetsPage2);
    assertEquals(1, citationDatasetsPage2.getCount());

    PagingResponse<DerivedDataset> datasetCitationPage1 =
        derivedDatasetResource.getDerivedDatasets(firstDataset.getKey(), REGULAR_PAGE);
    assertNotNull(datasetCitationPage1);
    assertEquals(2, datasetCitationPage1.getCount());

    PagingResponse<DerivedDataset> datasetCitationPage2 =
        derivedDatasetResource.getDerivedDatasets(thirdDataset.getKey(), REGULAR_PAGE);
    assertNotNull(datasetCitationPage2);
    assertEquals(1, datasetCitationPage2.getCount());
  }

  @Test
  public void testDerivedDatasetListByUser() {
    // prepare derived datasets
    prepareDerivedDataset("10.21373/dd.abcd1", "john");
    prepareDerivedDataset("10.21373/dd.abcd2", "james");
    prepareDerivedDataset("10.21373/dd.abcd3", "james");
    prepareDerivedDataset("10.21373/dd.abcd4", "james");

    // get derived datasets
    PagingResponse<DerivedDataset> johnDerivedDatasets
        = derivedDatasetResource.listByUser("john", REGULAR_PAGE);
    PagingResponse<DerivedDataset> jamesDerivedDatasets
        = derivedDatasetResource.listByUser("james", REGULAR_PAGE);
    PagingResponse<DerivedDataset> randomUserDerivedDatasets
        = derivedDatasetResource.listByUser("random", REGULAR_PAGE);

    assertNotNull(johnDerivedDatasets);
    assertEquals(1, johnDerivedDatasets.getCount());
    assertNotNull(jamesDerivedDatasets);
    assertEquals(3, jamesDerivedDatasets.getCount());
    assertNotNull(randomUserDerivedDatasets);
    assertEquals(0, randomUserDerivedDatasets.getCount());
  }

  @Test
  public void testGetCitationTextDoiUrlEncoded() throws Exception {
    prepareDerivedDataset("10.21373/dd.abcd1", "test");

    ResultActions actions =
        requestTestFixture
            .getRequest(new URI("/derivedDataset/10.21373%2Fdd.abcd1/citation"))
            .andExpect(status().isOk());

    String response =
        requestTestFixture.extractResponse(actions);

    assertEquals("Derived dataset GBIF.org", response);
  }

  @Test
  public void testGetDerivedDatasetDoiUrlEncoded() throws Exception {
    prepareDerivedDataset("10.21373/dd.abcd1", "test");

    ResultActions actions =
        requestTestFixture
            .getRequest(new URI("/derivedDataset/10.21373%2Fdd.abcd1"))
            .andExpect(status().isOk());

    DerivedDataset response =
        requestTestFixture.extractJsonResponse(actions, DerivedDataset.class);

    assertEquals(new DOI("10.21373/dd.abcd1"), response.getDoi());
  }

  public void prepareDerivedDataset(String doi, String creator) {
    DerivedDataset derivedDataset = new DerivedDataset();
    derivedDataset.setOriginalDownloadDOI(new DOI("10.21373/dl.abcdef"));
    derivedDataset.setCitation("Derived dataset GBIF.org");
    derivedDataset.setCreated(new Date());
    derivedDataset.setCreatedBy(creator);
    derivedDataset.setModified(new Date());
    derivedDataset.setModifiedBy(creator);
    derivedDataset.setDoi(new DOI(doi));
    derivedDataset.setRegistrationDate(new Date());
    derivedDataset.setSourceUrl(URI.create("gbif.org"));
    derivedDataset.setTitle("Derived dataset title");

    derivedDatasetMapper.create(derivedDataset);
  }

  private DerivedDatasetCreationRequest newDerivedDatasetCreationRequest(
      DOI originalDownloadDOI, Map<String, Long> relatedDatasets) {
    DerivedDatasetCreationRequest creationRequest = new DerivedDatasetCreationRequest();
    creationRequest.setTitle("Let's test citation");
    creationRequest.setDescription("Derived dataset description");
    creationRequest.setOriginalDownloadDOI(originalDownloadDOI);
    creationRequest.setSourceUrl(URI.create("https://www.gbif.org"));
    creationRequest.setRelatedDatasets(relatedDatasets);

    return creationRequest;
  }
}
