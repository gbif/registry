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
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.fixtures.TestConstants;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;

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
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = RegistryIntegrationTestsConfiguration.class)
@ContextConfiguration(initializers = {DatasetProcessStatusIT.ContextInitializer.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class DatasetProcessStatusIT {

  @RegisterExtension
  static PreparedDbExtension database =
      EmbeddedPostgresExtension.preparedDatabase(
          LiquibasePreparer.forClasspathLocation("liquibase/master.xml"));

  @RegisterExtension
  public final DatabaseInitializer databaseRule =
      new DatabaseInitializer(database.getTestDatabase());

  static class ContextInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      TestPropertyValues.of(dbTestPropertyPairs())
          .applyTo(configurableApplicationContext.getEnvironment());
      withSearchEnabled(false, configurableApplicationContext.getEnvironment());
    }

    protected static void withSearchEnabled(
        boolean enabled, ConfigurableEnvironment configurableEnvironment) {
      TestPropertyValues.of("searchEnabled=" + enabled).applyTo(configurableEnvironment);
    }

    protected String[] dbTestPropertyPairs() {
      return new String[] {
        "registry.datasource.url=jdbc:postgresql://localhost:"
            + database.getConnectionInfo().getPort()
            + "/"
            + database.getConnectionInfo().getDbName(),
        "registry.datasource.username=" + database.getConnectionInfo().getUser(),
        "registry.datasource.password="
      };
    }
  }

  private final TestDataFactory testDataFactory;

  private final DatasetProcessStatusService datasetProcessStatusService;
  private final SimplePrincipalProvider simplePrincipalProvider;
  // The following services are required to create dataset instances
  private final DatasetService datasetService;
  private final OrganizationService organizationService;
  private final NodeService nodeService;
  private final InstallationService installationService;

  @Autowired
  public DatasetProcessStatusIT(
      DatasetProcessStatusService datasetProcessStatusService,
      DatasetService datasetService,
      OrganizationService organizationService,
      NodeService nodeService,
      InstallationService installationService,
      SimplePrincipalProvider simplePrincipalProvider,
      TestDataFactory testDataFactory) {
    this.datasetProcessStatusService = datasetProcessStatusService;
    this.datasetService = datasetService;
    this.organizationService = organizationService;
    this.nodeService = nodeService;
    this.installationService = installationService;
    this.simplePrincipalProvider = simplePrincipalProvider;
    this.testDataFactory = testDataFactory;
  }

  @BeforeEach
  public void setup() {
    // reset SimplePrincipleProvider, configured for web service client tests only
    if (simplePrincipalProvider != null) {
      simplePrincipalProvider.setPrincipal(TestConstants.TEST_ADMIN);
      SecurityContext ctx = SecurityContextHolder.createEmptyContext();
      SecurityContextHolder.setContext(ctx);
      ctx.setAuthentication(
          new UsernamePasswordAuthenticationToken(
              simplePrincipalProvider.get().getName(),
              "",
              Collections.singleton(new SimpleGrantedAuthority(UserRole.REGISTRY_ADMIN.name()))));
    }
  }

  /** Tests the create and get of a {@link DatasetProcessStatus}. */
  @Test
  public void testCreateAndGet() {
    DatasetProcessStatus expected = buildProcessStatus(createTestDataset(), 1);
    datasetProcessStatusService.createDatasetProcessStatus(expected);
    DatasetProcessStatus actual =
        datasetProcessStatusService.getDatasetProcessStatus(
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
  @Test
  public void testListAndListByDataset() {
    Dataset dataset = createTestDataset();
    Dataset dataset2 = createTestDataset();

    datasetProcessStatusService.createDatasetProcessStatus(buildProcessStatus(dataset, 1));
    datasetProcessStatusService.createDatasetProcessStatus(buildProcessStatus(dataset, 2));
    datasetProcessStatusService.createDatasetProcessStatus(buildProcessStatus(dataset2, 1));

    PagingResponse<DatasetProcessStatus> statuses =
        datasetProcessStatusService.listDatasetProcessStatus(new PagingRequest(0, 10));
    assertEquals(Long.valueOf(3), statuses.getCount(), "There have been 3 crawl attempts");
    assertEquals(3, statuses.getResults().size(), "There have been 3 crawl attempts");

    PagingResponse<DatasetProcessStatus> statusesByDataset =
        datasetProcessStatusService.listDatasetProcessStatus(dataset.getKey(), new PagingRequest());
    assertEquals(
        Long.valueOf(2), statusesByDataset.getCount(), "The dataset has had 2 crawl attempts");
    assertEquals(2, statusesByDataset.getResults().size(), "The dataset has had 2 crawl attempts");
  }

  @Test
  public void testIllegalCreate() {
    Dataset dataset = createTestDataset();

    DatasetProcessStatus status = buildProcessStatus(dataset, 1);
    datasetProcessStatusService.createDatasetProcessStatus(status);
    // valid to update
    status.setFragmentsEmitted(1000);
    datasetProcessStatusService.updateDatasetProcessStatus(status);
    // illegal to create the same attempt ID
    assertThrows(
        IllegalArgumentException.class,
        () ->
            datasetProcessStatusService.createDatasetProcessStatus(buildProcessStatus(dataset, 1)));
  }

  /** Tests a create, get, update */
  @Test
  public void testUpdate() {
    DatasetProcessStatus orig = buildProcessStatus(createTestDataset(), 1);
    datasetProcessStatusService.createDatasetProcessStatus(orig);
    DatasetProcessStatus written =
        datasetProcessStatusService.getDatasetProcessStatus(
            orig.getDatasetKey(), orig.getCrawlJob().getAttempt());
    assertEquals(orig.getDatasetKey(), written.getDatasetKey());
    assertEquals(orig.getCrawlJob().getAttempt(), written.getCrawlJob().getAttempt());
    written.setFinishReason(FinishReason.ABORT);
    datasetProcessStatusService.updateDatasetProcessStatus(written);
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
