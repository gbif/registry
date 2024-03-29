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
package org.gbif.registry.ws.it;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetOccurrenceDownloadUsageService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.client.DatasetOccurrenceDownloadUsageClient;
import org.gbif.registry.ws.client.OccurrenceDownloadClient;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Runs tests for the {@link OccurrenceDownloadService} implementations. This is parameterized to
 * run the same test routines for the following:
 *
 * <ol>
 *   <li>The persistence layer
 *   <li>The WS service layer
 *   <li>The WS service client layer
 * </ol>
 */
public class DatasetOccurrenceDownloadIT extends BaseItTest {

  private TestDataFactory testDataFactory;
  private final OccurrenceDownloadClient occurrenceDownloadClient;
  private final OccurrenceDownloadService occurrenceDownloadResource;
  private final DatasetOccurrenceDownloadUsageClient datasetOccurrenceDownloadUsageClient;
  private final DatasetOccurrenceDownloadUsageService datasetOccurrenceDownloadUsageResource;

  // The following services are required to create dataset instances
  private final DatasetService datasetService;
  private final OrganizationService organizationService;
  private final NodeService nodeService;
  private final InstallationService installationService;

  @Autowired
  public DatasetOccurrenceDownloadIT(
      OccurrenceDownloadService occurrenceDownloadResource,
      OrganizationService organizationService,
      DatasetService datasetService,
      NodeService nodeService,
      InstallationService installationService,
      SimplePrincipalProvider simplePrincipalProvider,
      @Qualifier("datasetOccurrenceDownloadUsageResource")
          DatasetOccurrenceDownloadUsageService datasetOccurrenceDownloadUsageResource,
      TestDataFactory testDataFactory,
      EsManageServer esServer,
      @LocalServerPort int localServerPort,
      KeyStore keyStore) {
    super(simplePrincipalProvider, esServer);
    this.occurrenceDownloadResource = occurrenceDownloadResource;
    this.occurrenceDownloadClient =
        prepareClient(localServerPort, keyStore, OccurrenceDownloadClient.class);
    this.organizationService = organizationService;
    this.datasetService = datasetService;
    this.nodeService = nodeService;
    this.installationService = installationService;
    this.datasetOccurrenceDownloadUsageResource = datasetOccurrenceDownloadUsageResource;
    this.datasetOccurrenceDownloadUsageClient =
        prepareClient(localServerPort, keyStore, DatasetOccurrenceDownloadUsageClient.class);
    this.testDataFactory = testDataFactory;
  }

  /**
   * Creates a test dataset. The dataset is persisted in the data base. The installation and
   * organization related to the dataset are created too.
   */
  private Dataset createTestDataset() {
    // endorsing node for the organization
    UUID nodeKey = nodeService.create(testDataFactory.newNode());

    // publishing organization (required field)
    Organization o = testDataFactory.newOrganization(nodeKey);
    UUID organizationKey = organizationService.create(o);

    Installation i = testDataFactory.newInstallation(organizationKey);
    UUID installationKey = installationService.create(i);
    Dataset dataset = testDataFactory.newDataset(organizationKey, installationKey);
    dataset.setKey(datasetService.create(dataset));
    return dataset;
  }

  /**
   * Tests the process of persist a dataset occurrence download and list the downloads by dataset
   * key.
   */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testAddAndGetOccurrenceDatasetOne(ServiceType serviceType) {
    OccurrenceDownloadService occurrenceDownloadService =
        getService(serviceType, occurrenceDownloadResource, occurrenceDownloadClient);
    DatasetOccurrenceDownloadUsageService datasetOccurrenceDownloadUsageService =
        getService(
            serviceType,
            datasetOccurrenceDownloadUsageResource,
            datasetOccurrenceDownloadUsageClient);
    Download occurrenceDownload = OccurrenceDownloadIT.getTestInstancePredicateDownload();
    final Dataset testDataset = createTestDataset();

    occurrenceDownloadService.create(occurrenceDownload);
    Map<UUID, Long> datasetCitation = new HashMap<>();
    datasetCitation.put(testDataset.getKey(), 1000L);
    occurrenceDownloadService.createUsages(occurrenceDownload.getKey(), datasetCitation);

    assertEquals(
        1,
        datasetOccurrenceDownloadUsageService
            .listByDataset(testDataset.getKey(), true, new PagingRequest(0, 3))
            .getResults()
            .size(),
        "List operation should return 1 record");
    Download occDownload2 = occurrenceDownloadService.get(occurrenceDownload.getKey());
    assertEquals(1, occDownload2.getNumberDatasets());

    // we add it again to check that the usage is updated and doesn't create a new one
    datasetCitation.put(testDataset.getKey(), 2000L);
    occurrenceDownloadService.createUsages(occurrenceDownload.getKey(), datasetCitation);
    List<DatasetOccurrenceDownloadUsage> usages =
        datasetOccurrenceDownloadUsageService
            .listByDataset(testDataset.getKey(), true, new PagingRequest(0, 3))
            .getResults();
    assertEquals(1, usages.size(), "List operation should return 1 record");
    assertEquals(2000L, usages.get(0).getNumberRecords());
    occDownload2 = occurrenceDownloadService.get(occurrenceDownload.getKey());
    assertEquals(1, occDownload2.getNumberDatasets());
  }

  /**
   * Tests the process of persist a list of dataset occurrence download and list the downloads by
   * dataset key.
   */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testAddAndGetOccurrenceDatasetMany(ServiceType serviceType) {
    OccurrenceDownloadService occurrenceDownloadService =
        getService(serviceType, occurrenceDownloadResource, occurrenceDownloadClient);
    DatasetOccurrenceDownloadUsageService datasetOccurrenceDownloadUsageService =
        getService(
            serviceType,
            datasetOccurrenceDownloadUsageResource,
            datasetOccurrenceDownloadUsageClient);
    Download occurrenceDownload = OccurrenceDownloadIT.getTestInstancePredicateDownload();
    final Dataset testDataset1 = createTestDataset();
    final Dataset testDataset2 = createTestDataset();
    final Dataset testDataset3 = createTestDataset();

    occurrenceDownloadService.create(occurrenceDownload);

    Map<UUID, Long> datasetCitation = new HashMap<>();
    datasetCitation.put(testDataset1.getKey(), 1000L);
    datasetCitation.put(testDataset2.getKey(), 10000L);
    datasetCitation.put(testDataset3.getKey(), 100000L);
    occurrenceDownloadService.createUsages(occurrenceDownload.getKey(), datasetCitation);

    assertEquals(
        1,
        datasetOccurrenceDownloadUsageService
            .listByDataset(testDataset1.getKey(), true, new PagingRequest(0, 3))
            .getResults()
            .size(),
        "List operation should return 1 record");
    assertEquals(
        1,
        datasetOccurrenceDownloadUsageService
            .listByDataset(testDataset2.getKey(), true, new PagingRequest(0, 3))
            .getResults()
            .size(),
        "List operation should return 1 record");
    assertEquals(
        1,
        datasetOccurrenceDownloadUsageService
            .listByDataset(testDataset3.getKey(), true, new PagingRequest(0, 3))
            .getResults()
            .size(),
        "List operation should return 1 record");
    Download occDownload2 = occurrenceDownloadService.get(occurrenceDownload.getKey());
    assertEquals(3, occDownload2.getNumberDatasets());
  }
}
