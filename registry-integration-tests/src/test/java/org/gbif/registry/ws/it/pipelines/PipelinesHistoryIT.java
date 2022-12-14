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
package org.gbif.registry.ws.it.pipelines;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.pipelines.PipelineExecution;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.StepRunner;
import org.gbif.api.model.pipelines.StepType;
import org.gbif.api.model.pipelines.ws.PipelineProcessParameters;
import org.gbif.api.model.pipelines.ws.PipelineStepParameters;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.pipelines.PipelinesHistoryService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.InstallationType;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.NodeType;
import org.gbif.api.vocabulary.ParticipationStatus;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.client.pipelines.PipelinesHistoryClient;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.registry.ws.it.fixtures.TestConstants;
import org.gbif.registry.ws.it.fixtures.UserTestFixture;
import org.gbif.registry.ws.resources.pipelines.PipelinesHistoryResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the {@link PipelinesHistoryResource} and {@link PipelinesHistoryClient}. */
public class PipelinesHistoryIT extends BaseItTest {

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = new TestCaseDatabaseInitializer(database.getPostgresContainer());

  private final DatasetService datasetService;
  private final OrganizationService organizationService;
  private final NodeService nodeService;
  private final InstallationService installationService;
  private final PipelinesHistoryService pipelinesHistoryResource;
  private final PipelinesHistoryService pipelinesHistoryClient;
  private final PipelinesHistoryService getPipelinesHistoryClientUserCredentials;
  private final UserTestFixture userTestFixture;

  @Autowired
  public PipelinesHistoryIT(
      PipelinesHistoryService pipelinesHistoryResource,
      DatasetService datasetService,
      OrganizationService organizationService,
      NodeService nodeService,
      InstallationService installationService,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer,
      @LocalServerPort int localServerPort,
      KeyStore keyStore,
      UserTestFixture userTestFixture) {
    super(principalProvider, esServer);
    this.pipelinesHistoryResource = pipelinesHistoryResource;
    this.datasetService = datasetService;
    this.organizationService = organizationService;
    this.nodeService = nodeService;
    this.installationService = installationService;
    this.pipelinesHistoryClient =
        prepareClient(
            TestConstants.TEST_ADMIN, localServerPort, keyStore, PipelinesHistoryClient.class);
    this.getPipelinesHistoryClientUserCredentials =
        prepareClient(
            TestConstants.TEST_USER, localServerPort, keyStore, PipelinesHistoryClient.class);
    this.userTestFixture = userTestFixture;
  }

  @BeforeEach
  public void beforeEach() {
    userTestFixture.prepareAdminUser();
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void createPipelineProcessTest(ServiceType serviceType) {
    PipelinesHistoryService service =
        getService(serviceType, pipelinesHistoryResource, pipelinesHistoryClient);
    final UUID datasetKey = createDataset();
    final int attempt = 1;

    long key = service.createPipelineProcess(new PipelineProcessParameters(datasetKey, attempt));
    assertTrue(key > 0);

    PipelineProcess processCreated = service.getPipelineProcess(datasetKey, attempt);
    assertEquals(datasetKey, processCreated.getDatasetKey());
    assertEquals(attempt, processCreated.getAttempt());
    assertNotNull(processCreated.getCreated());
    assertEquals(TestConstants.TEST_ADMIN, processCreated.getCreatedBy());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void createPipelineProcessWithoutPrivilegesTest(ServiceType serviceType) {
    PipelinesHistoryService service =
        getService(serviceType, pipelinesHistoryResource, getPipelinesHistoryClientUserCredentials);
    resetSecurityContext(TestConstants.TEST_USER, UserRole.USER);
    assertThrows(
        AccessDeniedException.class,
        () -> service.createPipelineProcess(new PipelineProcessParameters(UUID.randomUUID(), 1)));
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void getNonExistentPipelineProcessTest(ServiceType serviceType) {
    PipelinesHistoryService service =
        getService(serviceType, pipelinesHistoryResource, pipelinesHistoryClient);
    assertNull(service.getPipelineProcess(UUID.randomUUID(), 0));
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void historyTestAndSearchUpdate(ServiceType serviceType) {
    PipelinesHistoryService service =
        getService(serviceType, pipelinesHistoryResource, pipelinesHistoryClient);
    final UUID datasetKey1 = createDataset();
    final UUID datasetKey2 = createDataset();

    service.createPipelineProcess(new PipelineProcessParameters(datasetKey1, 1));
    service.createPipelineProcess(new PipelineProcessParameters(datasetKey1, 2));
    service.createPipelineProcess(new PipelineProcessParameters(datasetKey1, 3));
    service.createPipelineProcess(new PipelineProcessParameters(datasetKey2, 1));
    service.createPipelineProcess(new PipelineProcessParameters(datasetKey2, 2));

    PagingResponse<PipelineProcess> processes = service.history(new PagingRequest());
    assertEquals(5, processes.getCount());

    processes = service.history(datasetKey1, new PagingRequest());
    assertEquals(3, processes.getCount());

    processes = service.history(datasetKey2, new PagingRequest());
    assertEquals(2, processes.getCount());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void addPipelineStepTestAndSearchUpdate(ServiceType serviceType) {
    PipelinesHistoryService service =
        getService(serviceType, pipelinesHistoryResource, pipelinesHistoryClient);
    final UUID datasetKey1 = createDataset();
    long processKey = service.createPipelineProcess(new PipelineProcessParameters(datasetKey1, 1));

    PipelineExecution execution =
        new PipelineExecution()
            .setStepsToRun(Collections.singletonList(StepType.DWCA_TO_VERBATIM))
            .setRerunReason("rerun")
            .setRemarks("remarks");
    long executionKey = service.addPipelineExecution(processKey, execution);

    PipelineStep step =
        new PipelineStep()
            .setMessage("message")
            .setRunner(StepRunner.STANDALONE)
            .setType(StepType.ABCD_TO_VERBATIM)
            .setState(PipelineStep.Status.RUNNING);
    long stepKey = service.addPipelineStep(processKey, executionKey, step);
    assertTrue(stepKey > 0);

    PipelineStep stepCreated = service.getPipelineStep(processKey, executionKey, stepKey);
    assertNull(stepCreated.getFinished());
    assertTrue(stepCreated.lenientEquals(step));
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void addPipelineStepWithoutPrivilegesTest(ServiceType serviceType) {
    PipelinesHistoryService service =
        getService(serviceType, pipelinesHistoryResource, pipelinesHistoryClient);
    final UUID datasetKey1 = createDataset();
    long processKey = service.createPipelineProcess(new PipelineProcessParameters(datasetKey1, 1));
    assertTrue(processKey > 0);

    long executionKey = service.addPipelineExecution(processKey, new PipelineExecution());

    resetSecurityContext(TestConstants.TEST_USER, UserRole.USER);
    PipelinesHistoryService serviceUserCredentials =
        getService(serviceType, pipelinesHistoryResource, getPipelinesHistoryClientUserCredentials);

    PipelineStep step =
        new PipelineStep()
            .setMessage("message")
            .setRunner(StepRunner.STANDALONE)
            .setType(StepType.ABCD_TO_VERBATIM)
            .setState(PipelineStep.Status.RUNNING);

    assertThrows(
        AccessDeniedException.class,
        () -> serviceUserCredentials.addPipelineStep(processKey, executionKey, step));
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void updatePipelineStepStatusAndMetricsTest(ServiceType serviceType) {
    PipelinesHistoryService service =
        getService(serviceType, pipelinesHistoryResource, pipelinesHistoryClient);
    final UUID datasetKey1 = createDataset();
    long processKey = service.createPipelineProcess(new PipelineProcessParameters(datasetKey1, 1));

    PipelineExecution execution =
        new PipelineExecution()
            .setStepsToRun(Collections.singletonList(StepType.DWCA_TO_VERBATIM))
            .setRerunReason("rerun")
            .setRemarks("remarks");
    long executionKey = service.addPipelineExecution(processKey, execution);

    PipelineStep step =
        new PipelineStep()
            .setMessage("message")
            .setRunner(StepRunner.STANDALONE)
            .setType(StepType.ABCD_TO_VERBATIM)
            .setState(PipelineStep.Status.RUNNING);
    long stepKey = service.addPipelineStep(processKey, executionKey, step);

    PipelineStepParameters stepParams =
        new PipelineStepParameters(
            PipelineStep.Status.COMPLETED,
            Collections.singletonList(new PipelineStep.MetricInfo("name", "value")));
    service.updatePipelineStepStatusAndMetrics(processKey, executionKey, stepKey, stepParams);

    PipelineStep stepCreated = service.getPipelineStep(processKey, executionKey, stepKey);
    assertEquals(PipelineStep.Status.COMPLETED, stepCreated.getState());
    assertNotNull(stepCreated.getFinished());
    assertEquals(1, stepCreated.getMetrics().size());
    assertEquals("value", stepCreated.getMetrics().iterator().next().getValue());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void getPipelineWorkflowNonExistentProcessTest(ServiceType serviceType) {
    PipelinesHistoryService service =
        getService(serviceType, pipelinesHistoryResource, pipelinesHistoryClient);
    assertNull(service.getPipelineProcess(UUID.randomUUID(), 1));
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void runPipelineAttemptTest(ServiceType serviceType) {
    PipelinesHistoryService service =
        getService(serviceType, pipelinesHistoryResource, pipelinesHistoryClient);
    // create one process with one step
    final UUID datasetKey1 = createDataset();
    final int attempt = 1;
    long processKey =
        service.createPipelineProcess(new PipelineProcessParameters(datasetKey1, attempt));

    PipelineExecution execution =
        new PipelineExecution()
            .setStepsToRun(Collections.singletonList(StepType.DWCA_TO_VERBATIM))
            .setRerunReason("rerun")
            .setRemarks("remarks");
    long executionKey = service.addPipelineExecution(processKey, execution);

    service.addPipelineStep(
        processKey,
        executionKey,
        new PipelineStep()
            .setMessage(
                "{\"datasetUuid\":\"418a6571-b6c1-4db0-b90e-8f36bde4c80e\",\"datasetType\":\"SAMPLING_EVENT\",\"source\":"
                    + "\"http://gbif.vm.ntnu.no/ipt/archive.do?r=setesdal_veg_data\",\"attempt\":109,\"validationReport\":"
                    + "{\"datasetKey\":\"418a6571-b6c1-4db0-b90e-8f36bde4c80e\",\"occurrenceReport\":{\"checkedRecords\":11961,"
                    + "\"uniqueTriplets\":0,\"allRecordsChecked\":true,\"recordsWithInvalidTriplets\":11961,\"uniqueOccurrenceIds\":11961,"
                    + "\"recordsMissingOccurrenceId\":0,\"invalidationReason\":null,\"valid\":true},\"genericReport\":{\"checkedRecords\":1630,"
                    + "\"allRecordsChecked\":true,\"duplicateIds\":[],\"rowNumbersMissingId\":[],\"invalidationReason\":null,\"valid\":true},"
                    + "\"invalidationReason\":null,\"valid\":true},\"pipelineSteps\":[\"DWCA_TO_VERBATIM\",\"HDFS_VIEW\","
                    + "\"VERBATIM_TO_INTERPRETED\",\"INTERPRETED_TO_INDEX\"],\"endpointType\":\"DWC_ARCHIVE\",\"platform\":\"ALL\"}")
            .setRunner(StepRunner.STANDALONE)
            .setType(StepType.DWCA_TO_VERBATIM)
            .setState(PipelineStep.Status.COMPLETED));

    // run the process
    final String rerunReason = "test reason";
    service.runPipelineAttempt(
        datasetKey1, attempt, StepType.DWCA_TO_VERBATIM.name(), rerunReason, false, null);

    // check that the DB was updated
    PipelineProcess process = service.getPipelineProcess(datasetKey1, attempt);
    assertEquals(rerunReason, process.getExecutions().iterator().next().getRerunReason());

    // run the process without attempt now
    final String rerunReason2 = "test reason 2";
    service.runPipelineAttempt(
        datasetKey1, StepType.DWCA_TO_VERBATIM.name(), rerunReason2, false, false, null);

    // check that the DB was updated again
    process = service.getPipelineProcess(datasetKey1, attempt);
    assertEquals(3, process.getExecutions().size());
    process.getExecutions().forEach(e -> assertNotNull(e.getRerunReason()));
  }

  @ParameterizedTest
  @EnumSource(
      value = ServiceType.class,
      names = {"CLIENT"})
  public void runPipelineAttemptInRunningStateTest(ServiceType serviceType) {
    PipelinesHistoryService service =
        getService(serviceType, pipelinesHistoryResource, pipelinesHistoryClient);
    // create one process with one step
    final UUID datasetKey1 = createDataset();
    final int attempt = 1;
    long processKey =
        pipelinesHistoryClient.createPipelineProcess(
            new PipelineProcessParameters(datasetKey1, attempt));

    PipelineExecution execution =
        new PipelineExecution()
            .setStepsToRun(Collections.singletonList(StepType.DWCA_TO_VERBATIM))
            .setRerunReason("rerun")
            .setRemarks("remarks");
    long executionKey = pipelinesHistoryClient.addPipelineExecution(processKey, execution);

    service.addPipelineStep(
        processKey,
        executionKey,
        new PipelineStep()
            .setMessage("message")
            .setRunner(StepRunner.STANDALONE)
            .setType(StepType.ABCD_TO_VERBATIM)
            .setState(PipelineStep.Status.RUNNING));

    // run process and expect a bad request since the step is in running state
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.runPipelineAttempt(
                datasetKey1, attempt, StepType.ABCD_TO_VERBATIM.name(), "test", false, null));

    // run process without attempt and expect a bad request since the step is in running state
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.runPipelineAttempt(
                datasetKey1, StepType.ABCD_TO_VERBATIM.name(), "test", false, false, null));
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void runPipelineAttemptInRunningStateMarkPreviousAsFailedTest(ServiceType serviceType) {
    PipelinesHistoryService service =
        getService(serviceType, pipelinesHistoryResource, pipelinesHistoryClient);
    // create one process with one step
    final UUID datasetKey1 = createDataset();
    final int attempt = 1;
    long processKey =
        pipelinesHistoryClient.createPipelineProcess(
            new PipelineProcessParameters(datasetKey1, attempt));

    PipelineExecution execution =
        new PipelineExecution()
            .setStepsToRun(Collections.singletonList(StepType.DWCA_TO_VERBATIM))
            .setRerunReason("rerun")
            .setRemarks("remarks");
    long executionKey = pipelinesHistoryClient.addPipelineExecution(processKey, execution);

    long stepKey =
        service.addPipelineStep(
            processKey,
            executionKey,
            new PipelineStep()
                .setMessage(
                    "{\"datasetUuid\":\"418a6571-b6c1-4db0-b90e-8f36bde4c80e\",\"datasetType\":\"SAMPLING_EVENT\",\"source\":"
                        + "\"http://gbif.vm.ntnu.no/ipt/archive.do?r=setesdal_veg_data\",\"attempt\":109,\"validationReport\":"
                        + "{\"datasetKey\":\"418a6571-b6c1-4db0-b90e-8f36bde4c80e\",\"occurrenceReport\":{\"checkedRecords\":11961,"
                        + "\"uniqueTriplets\":0,\"allRecordsChecked\":true,\"recordsWithInvalidTriplets\":11961,\"uniqueOccurrenceIds\":11961,"
                        + "\"recordsMissingOccurrenceId\":0,\"invalidationReason\":null,\"valid\":true},\"genericReport\":{\"checkedRecords\":1630,"
                        + "\"allRecordsChecked\":true,\"duplicateIds\":[],\"rowNumbersMissingId\":[],\"invalidationReason\":null,\"valid\":true},"
                        + "\"invalidationReason\":null,\"valid\":true},\"pipelineSteps\":[\"DWCA_TO_VERBATIM\",\"HDFS_VIEW\","
                        + "\"VERBATIM_TO_INTERPRETED\",\"INTERPRETED_TO_INDEX\"],\"endpointType\":\"DWC_ARCHIVE\",\"platform\":\"ALL\"}")
                .setRunner(StepRunner.STANDALONE)
                .setType(StepType.ABCD_TO_VERBATIM)
                .setState(PipelineStep.Status.RUNNING));

    service.runPipelineAttempt(
        datasetKey1, StepType.ABCD_TO_VERBATIM.name(), "test", false, true, null);

    PipelineStep stepCreated = service.getPipelineStep(processKey, executionKey, stepKey);
    assertEquals(PipelineStep.Status.FAILED, stepCreated.getState());
  }

  private UUID createDataset() {
    Node node = new Node();
    node.setTitle("node");
    node.setType(NodeType.COUNTRY);
    node.setParticipationStatus(ParticipationStatus.AFFILIATE);
    node.setCreatedBy("test");
    UUID nodeKey = nodeService.create(node);

    Organization org = new Organization();
    org.setEndorsingNodeKey(nodeKey);
    org.setTitle("organization");
    org.setLanguage(Language.ABKHAZIAN);
    org.setPassword("testtttt");
    org.setCreatedBy("test");
    UUID orgKey = organizationService.create(org);

    Installation installation = new Installation();
    installation.setOrganizationKey(orgKey);
    installation.setType(InstallationType.BIOCASE_INSTALLATION);
    installation.setTitle("title");
    UUID installationKey = installationService.create(installation);

    Dataset dataset = new Dataset();
    dataset.setInstallationKey(installationKey);
    dataset.setPublishingOrganizationKey(orgKey);
    dataset.setType(DatasetType.CHECKLIST);
    dataset.setLanguage(Language.ABKHAZIAN);
    dataset.setLicense(License.CC0_1_0);
    dataset.setTitle("title");
    return datasetService.create(dataset);
  }
}
