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

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.crawler.CrawlJob;
import org.gbif.api.model.crawler.DatasetProcessStatus;
import org.gbif.api.model.crawler.FinishReason;
import org.gbif.api.model.crawler.ProcessState;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetProcessStatusService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.EndpointType;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.client.DatasetProcessStatusClient;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Runs tests for the {@link DatasetProcessStatusService} implementations. This is parameterized to
 * run the same test routines for the following:
 *
 * <ol>
 *   <li>The persistence layer
 *   <li>The WS service layer
 *   <li>The WS service client layer
 * </ol>
 */
public class DatasetProcessStatusIT extends BaseItTest {

  private final TestDataFactory testDataFactory;

  private final DatasetProcessStatusService datasetProcessStatusResource;
  private final DatasetProcessStatusService datasetProcessStatusClient;
  // The following services are required to create dataset instances
  private final DatasetService datasetService;
  private final OrganizationService organizationService;
  private final NodeService nodeService;
  private final InstallationService installationService;

  @Autowired
  public DatasetProcessStatusIT(
      DatasetProcessStatusService datasetProcessStatusResource,
      DatasetService datasetService,
      OrganizationService organizationService,
      NodeService nodeService,
      InstallationService installationService,
      SimplePrincipalProvider simplePrincipalProvider,
      TestDataFactory testDataFactory,
      EsManageServer esServer,
      @LocalServerPort int localServerPort,
      KeyStore keyStore) {
    super(simplePrincipalProvider, esServer);
    this.datasetProcessStatusResource = datasetProcessStatusResource;
    this.datasetService = datasetService;
    this.organizationService = organizationService;
    this.nodeService = nodeService;
    this.installationService = installationService;
    this.testDataFactory = testDataFactory;
    this.datasetProcessStatusClient =
        prepareClient(localServerPort, keyStore, DatasetProcessStatusClient.class);
  }

  /** Tests the create and get of a {@link DatasetProcessStatus}. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testCreateAndGet(ServiceType serviceType) {
    DatasetProcessStatusService service =
        getService(serviceType, datasetProcessStatusResource, datasetProcessStatusClient);
    DatasetProcessStatus expected = buildProcessStatus(createTestDataset(), 1);
    service.createDatasetProcessStatus(expected);
    DatasetProcessStatus actual =
        service.getDatasetProcessStatus(
            expected.getDatasetKey(), expected.getCrawlJob().getAttempt());
    assertNotNull(actual);
    assertEquals(expected.getFinishedCrawling(), actual.getFinishedCrawling());
    assertEquals(expected.getStartedCrawling(), actual.getStartedCrawling());
    assertEquals(expected.getDatasetKey(), actual.getDatasetKey());
    assertEquals(expected.getCrawlJob(), actual.getCrawlJob());
    assertEquals(expected.getDeclaredCount(), actual.getDeclaredCount());
    assertEquals(expected.getFinishReason(), actual.getFinishReason());
    assertEquals(expected.getFragmentsEmitted(), actual.getFragmentsEmitted());
    assertEquals(expected.getFragmentsProcessed(), actual.getFragmentsProcessed());
    assertEquals(expected.getFragmentsReceived(), actual.getFragmentsReceived());
    assertEquals(
        expected.getInterpretedOccurrencesPersistedError(),
        actual.getInterpretedOccurrencesPersistedError());
    assertEquals(
        expected.getInterpretedOccurrencesPersistedSuccessful(),
        actual.getInterpretedOccurrencesPersistedSuccessful());
    assertEquals(expected.getPagesCrawled(), expected.getPagesCrawled());
    assertEquals(expected.getProcessStateChecklist(), actual.getProcessStateChecklist());
    assertEquals(expected.getProcessStateOccurrence(), actual.getProcessStateOccurrence());
    assertEquals(expected.getProcessStateSample(), actual.getProcessStateSample());
    assertEquals(
        expected.getVerbatimOccurrencesPersistedSuccessful(),
        actual.getVerbatimOccurrencesPersistedSuccessful());
    assertEquals(
        expected.getVerbatimOccurrencesPersistedError(),
        actual.getVerbatimOccurrencesPersistedError());
    assertEquals(
        expected.getRawOccurrencesPersistedError(), actual.getRawOccurrencesPersistedError());
    assertEquals(expected.getRawOccurrencesPersistedNew(), actual.getRawOccurrencesPersistedNew());
    assertEquals(
        expected.getRawOccurrencesPersistedUnchanged(),
        actual.getRawOccurrencesPersistedUnchanged());
    assertEquals(
        expected.getRawOccurrencesPersistedUpdated(), actual.getRawOccurrencesPersistedUpdated());
    assertEquals(expected.getPagesFragmentedSuccessful(), actual.getPagesFragmentedSuccessful());
    assertEquals(expected.getPagesFragmentedError(), actual.getPagesFragmentedError());
  }

  /**
   * Tests the {@link
   * DatasetProcessStatusService#listDatasetProcessStatus(org.gbif.api.model.common.paging.Pageable)}
   * operation.
   */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testListAndListByDataset(ServiceType serviceType) {
    DatasetProcessStatusService service =
        getService(serviceType, datasetProcessStatusResource, datasetProcessStatusClient);
    Dataset dataset = createTestDataset();
    Dataset dataset2 = createTestDataset();

    service.createDatasetProcessStatus(buildProcessStatus(dataset, 1));
    service.createDatasetProcessStatus(buildProcessStatus(dataset, 2));
    service.createDatasetProcessStatus(buildProcessStatus(dataset2, 1));

    PagingResponse<DatasetProcessStatus> statuses =
        service.listDatasetProcessStatus(new PagingRequest(0, 10));
    assertEquals(Long.valueOf(3), statuses.getCount(), "There have been 3 crawl attempts");
    assertEquals(3, statuses.getResults().size(), "There have been 3 crawl attempts");

    PagingResponse<DatasetProcessStatus> statusesByDataset =
        service.listDatasetProcessStatus(dataset.getKey(), new PagingRequest());
    assertEquals(
        Long.valueOf(2), statusesByDataset.getCount(), "The dataset has had 2 crawl attempts");
    assertEquals(2, statusesByDataset.getResults().size(), "The dataset has had 2 crawl attempts");
  }

  // TODO: 07/05/2020 client exception
  @ParameterizedTest
  @EnumSource(
      value = ServiceType.class,
      names = {"RESOURCE"})
  public void testIllegalCreate(ServiceType serviceType) {
    DatasetProcessStatusService service =
        getService(serviceType, datasetProcessStatusResource, datasetProcessStatusClient);
    Dataset dataset = createTestDataset();

    DatasetProcessStatus status = buildProcessStatus(dataset, 1);
    service.createDatasetProcessStatus(status);
    // valid to update
    status.setFragmentsEmitted(1000);
    service.updateDatasetProcessStatus(status);
    // illegal to create the same attempt ID
    assertThrows(
        IllegalArgumentException.class,
        () -> service.createDatasetProcessStatus(buildProcessStatus(dataset, 1)));
  }

  /** Tests a create, get, update */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testUpdate(ServiceType serviceType) {
    DatasetProcessStatusService service =
        getService(serviceType, datasetProcessStatusResource, datasetProcessStatusClient);
    DatasetProcessStatus orig = buildProcessStatus(createTestDataset(), 1);
    service.createDatasetProcessStatus(orig);
    DatasetProcessStatus written =
        service.getDatasetProcessStatus(orig.getDatasetKey(), orig.getCrawlJob().getAttempt());
    assertEquals(orig.getDatasetKey(), written.getDatasetKey());
    assertEquals(orig.getCrawlJob().getAttempt(), written.getCrawlJob().getAttempt());
    written.setFinishReason(FinishReason.ABORT);
    service.updateDatasetProcessStatus(written);
    assertEquals(orig.getCrawlJob().getAttempt(), written.getCrawlJob().getAttempt());
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

  /** Builds a new sample process status for the given dataset and attempt number. */
  private DatasetProcessStatus buildProcessStatus(Dataset dataset, int attempt) {
    DatasetProcessStatus.Builder builder = new DatasetProcessStatus.Builder();
    CrawlJob crawlJob =
        new CrawlJob(
            dataset.getKey(),
            EndpointType.DIGIR,
            URI.create("http://gbif.org.test"),
            attempt,
            null);
    builder.crawlJob(crawlJob);
    builder.crawlContext("testcontext");
    builder.startedCrawling(new Date());
    builder.finishedCrawling(new Date());
    builder.fragmentsEmitted(144);
    builder.fragmentsProcessed(144);
    builder.fragmentsReceived(144);
    builder.finishReason(FinishReason.NORMAL);
    builder.processStateOccurrence(ProcessState.FINISHED);
    builder.processStateChecklist(ProcessState.EMPTY);
    builder.interpretedOccurrencesPersistedError(0);
    builder.interpretedOccurrencesPersistedSuccessful(144);
    builder.pagesCrawled(2);
    builder.pagesFragmentedError(0);
    builder.pagesFragmentedSuccessful(2);
    builder.rawOccurrencesPersistedError(0);
    builder.rawOccurrencesPersistedNew(144);
    builder.rawOccurrencesPersistedUnchanged(0);
    builder.rawOccurrencesPersistedUpdated(0);
    builder.verbatimOccurrencesPersistedError(0);
    builder.verbatimOccurrencesPersistedSuccessful(144);
    builder.datasetKey(crawlJob.getDatasetKey());
    return builder.build();
  }
}
