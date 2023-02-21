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

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.crawler.DwcaValidationReport;
import org.gbif.api.model.crawler.GenericValidationReport;
import org.gbif.api.model.crawler.OccurrenceValidationReport;
import org.gbif.api.model.pipelines.PipelineExecution;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.PipelineStep.MetricInfo;
import org.gbif.api.model.pipelines.PipelineStep.Status;
import org.gbif.api.model.pipelines.StepRunner;
import org.gbif.api.model.pipelines.StepType;
import org.gbif.api.model.pipelines.ws.PipelineProcessParameters;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.pipelines.PipelinesHistoryService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.*;
import org.gbif.common.messaging.api.messages.PipelinesDwcaMessage;
import org.gbif.common.messaging.api.messages.Platform;
import org.gbif.registry.database.DatabaseCleaner;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.client.pipelines.PipelinesHistoryClient;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.registry.ws.it.fixtures.TestConstants;
import org.gbif.registry.ws.it.fixtures.UserTestFixture;
import org.gbif.registry.ws.resources.pipelines.PipelinesHistoryResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.security.access.AccessDeniedException;

import static org.gbif.api.model.pipelines.StepType.DWCA_TO_VERBATIM;
import static org.gbif.api.model.pipelines.StepType.HDFS_VIEW;
import static org.gbif.api.model.pipelines.StepType.INTERPRETED_TO_INDEX;
import static org.gbif.api.model.pipelines.StepType.VERBATIM_TO_INTERPRETED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/** Tests the {@link PipelinesHistoryResource} and {@link PipelinesHistoryClient}. */
@Disabled
class PipelinesHistoryIT extends BaseItTest {

  @RegisterExtension
  protected static DatabaseCleaner databaseCleaner = new DatabaseCleaner(PG_CONTAINER);

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

  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  void createPipelineProcessTest(ServiceType serviceType) {
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

  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  void createPipelineProcessWithoutPrivilegesTest(ServiceType serviceType) {
    PipelinesHistoryService service =
        getService(serviceType, pipelinesHistoryResource, getPipelinesHistoryClientUserCredentials);
    resetSecurityContext(TestConstants.TEST_USER, UserRole.USER);
    assertThrows(
        AccessDeniedException.class,
        () -> service.createPipelineProcess(new PipelineProcessParameters(UUID.randomUUID(), 1)));
  }

  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  void getNonExistentPipelineProcessTest(ServiceType serviceType) {
    PipelinesHistoryService service =
        getService(serviceType, pipelinesHistoryResource, pipelinesHistoryClient);
    assertNull(service.getPipelineProcess(UUID.randomUUID(), 0));
  }

  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  void historyTestAndSearchUpdate(ServiceType serviceType) {
    PipelinesHistoryService service =
        getService(serviceType, pipelinesHistoryResource, pipelinesHistoryClient);
    final UUID datasetKey1 = createDataset();
    final UUID datasetKey2 = createDataset();

    service.createPipelineProcess(new PipelineProcessParameters(datasetKey1, 1));
    service.createPipelineProcess(new PipelineProcessParameters(datasetKey1, 2));
    service.createPipelineProcess(new PipelineProcessParameters(datasetKey1, 3));
    service.createPipelineProcess(new PipelineProcessParameters(datasetKey2, 1));
    service.createPipelineProcess(new PipelineProcessParameters(datasetKey2, 2));

    PagingResponse<PipelineProcess> processes = service.history(datasetKey1, new PagingRequest());
    assertEquals(3, processes.getCount());

    processes = service.history(datasetKey2, new PagingRequest());
    assertEquals(2, processes.getCount());
  }

  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  void addPipelineStepTestAndSearchUpdate(ServiceType serviceType) {
    PipelinesHistoryService service =
        getService(serviceType, pipelinesHistoryResource, pipelinesHistoryClient);
    final UUID datasetKey1 = createDataset();
    long processKey = service.createPipelineProcess(new PipelineProcessParameters(datasetKey1, 1));

    PipelineExecution execution =
        new PipelineExecution()
            .setStepsToRun(Collections.singleton(DWCA_TO_VERBATIM))
            .setRerunReason("rerun")
            .setRemarks("remarks");
    long executionKey = service.addPipelineExecution(processKey, execution);

    PipelineStep step = service.getPipelineStepsByExecutionKey(executionKey)
      .stream()
      .filter(x -> x.getType() == DWCA_TO_VERBATIM)
      .findAny()
      .orElseThrow(()->new IllegalArgumentException("Oops!"));

    step.setMessage("message");
    step.setRunner(StepRunner.STANDALONE);
    step.setState(PipelineStep.Status.RUNNING);

    long updatedStepKey = service.updatePipelineStep(step);
    assertEquals(step.getKey(), updatedStepKey);

    PipelineStep stepCreated = service.getPipelineStep(updatedStepKey);
    assertNull(stepCreated.getFinished());
    assertTrue(stepCreated.lenientEquals(step));
  }

  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  void updatePipelineStepWithoutPrivilegesTest(ServiceType serviceType) {
    PipelinesHistoryService service =
        getService(serviceType, pipelinesHistoryResource, pipelinesHistoryClient);
    final UUID datasetKey1 = createDataset();
    long processKey = service.createPipelineProcess(new PipelineProcessParameters(datasetKey1, 1));
    assertTrue(processKey > 0);

    service.addPipelineExecution(processKey, new PipelineExecution());

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
        () -> serviceUserCredentials.updatePipelineStep(step));
  }

  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  void updatePipelineStepStatusAndMetricsTest(ServiceType serviceType) {
    PipelinesHistoryService service =
        getService(serviceType, pipelinesHistoryResource, pipelinesHistoryClient);
    final UUID datasetKey1 = createDataset();
    long processKey = service.createPipelineProcess(new PipelineProcessParameters(datasetKey1, 1));

    PipelineExecution execution =
        new PipelineExecution()
            .setStepsToRun(Collections.singleton(DWCA_TO_VERBATIM))
            .setRerunReason("rerun")
            .setRemarks("remarks");
    long executionKey = service.addPipelineExecution(processKey, execution);

    PipelineStep step = service.getPipelineStepsByExecutionKey(executionKey)
      .stream()
      .filter(x -> x.getType() == DWCA_TO_VERBATIM)
      .findAny()
      .orElseThrow(()->new IllegalArgumentException("Oops!"));

    step.setState(PipelineStep.Status.COMPLETED);
    step.setMetrics(Collections.singleton(new MetricInfo("name", "value")));

    long updatedStepKey = service.updatePipelineStep(step);
    assertEquals(step.getKey(), updatedStepKey);

    PipelineStep stepCreated = service.getPipelineStep(updatedStepKey);

    assertEquals(PipelineStep.Status.COMPLETED, stepCreated.getState());
    assertEquals(1, stepCreated.getMetrics().size());
    assertEquals("value", stepCreated.getMetrics().iterator().next().getValue());
  }

  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  void getPipelineWorkflowNonExistentProcessTest(ServiceType serviceType) {
    PipelinesHistoryService service =
        getService(serviceType, pipelinesHistoryResource, pipelinesHistoryClient);
    assertNull(service.getPipelineProcess(UUID.randomUUID(), 1));
  }


  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  void runPipelineAttemptTest(ServiceType serviceType) {
    PipelinesHistoryService service =
        getService(serviceType, pipelinesHistoryResource, pipelinesHistoryClient);
    // create one process with one step
    final UUID datasetKey1 = createDataset();
    final int attempt = 1;
    long processKey =
        service.createPipelineProcess(new PipelineProcessParameters(datasetKey1, attempt));

    PipelineExecution execution =
        new PipelineExecution()
            .setStepsToRun(Collections.singleton(DWCA_TO_VERBATIM))
            .setRerunReason("rerun")
            .setRemarks("remarks");
    long executionKey = service.addPipelineExecution(processKey, execution);

    UUID uuid = UUID.fromString("418a6571-b6c1-4db0-b90e-8f36bde4c80e");
    PipelinesDwcaMessage message = new PipelinesDwcaMessage();
    message.setDatasetUuid(uuid);
    message.setAttempt(109);
    message.setDatasetType(DatasetType.SAMPLING_EVENT);
    message.setSource(URI.create("http://gbif.vm.ntnu.no/ipt/archive.do?r=setesdal_veg_data"));
    message.setPlatform(Platform.ALL);
    message.setEndpointType(EndpointType.DWC_ARCHIVE);
    message.setPipelineSteps(Collections.singleton("DWCA_TO_VERBATIM"));

    OccurrenceValidationReport ovr = new OccurrenceValidationReport(11961,0,11961,11961, 0, true);
    GenericValidationReport gvr = new GenericValidationReport(1630, true, Collections.emptyList(), Collections.emptyList());

    DwcaValidationReport report = new DwcaValidationReport(uuid, ovr, gvr, "");
    message.setValidationReport(report);

    PipelineStep pipelineStep = new PipelineStep()
      .setMessage(message.toString())
      .setRunner(StepRunner.STANDALONE)
      .setType(DWCA_TO_VERBATIM)
      .setState(Status.COMPLETED);

    pipelineStep.setKey(executionKey);

    service.updatePipelineStep(pipelineStep);

    // run the process
    final String rerunReason = "test reason";
    service.runPipelineAttempt(
        datasetKey1, attempt, DWCA_TO_VERBATIM.name(), rerunReason, false, null);

    // check that the DB was updated
    PipelineProcess process = service.getPipelineProcess(datasetKey1, attempt);
    assertEquals(rerunReason, process.getExecutions().iterator().next().getRerunReason());

    // run the process without attempt now
    final String rerunReason2 = "test reason 2";
    service.runPipelineAttempt(
        datasetKey1, DWCA_TO_VERBATIM.name(), rerunReason2, false, false, null);

    // check that the DB was updated again
    process = service.getPipelineProcess(datasetKey1, attempt);
    assertEquals(3, process.getExecutions().size());
    process.getExecutions().forEach(e -> assertNotNull(e.getRerunReason()));
  }

  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(
      value = ServiceType.class,
      names = {"CLIENT"})
  void runPipelineAttemptInRunningStateTest(ServiceType serviceType) {
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
            .setStepsToRun(Collections.singleton(DWCA_TO_VERBATIM))
            .setRerunReason("rerun")
            .setRemarks("remarks");
    pipelinesHistoryClient.addPipelineExecution(processKey, execution);

    service.updatePipelineStep(
        new PipelineStep()
            .setMessage("message")
            .setRunner(StepRunner.STANDALONE)
            .setType(StepType.DWCA_TO_VERBATIM)
            .setState(PipelineStep.Status.RUNNING));

    // run process and expect a bad request since the step is in running state
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.runPipelineAttempt(
                datasetKey1, attempt, StepType.DWCA_TO_VERBATIM.name(), "test", false, null));

    // run process without attempt and expect a bad request since the step is in running state
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.runPipelineAttempt(
                datasetKey1, StepType.DWCA_TO_VERBATIM.name(), "test", false, false, null));
  }

  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  void runPipelineAttemptInRunningStateMarkPreviousAsFailedTest(ServiceType serviceType) {
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
            .setStepsToRun(Collections.singleton(DWCA_TO_VERBATIM))
            .setRerunReason("rerun")
            .setRemarks("remarks");
    long executionKey = pipelinesHistoryClient.addPipelineExecution(processKey, execution);

    PipelinesDwcaMessage message = new PipelinesDwcaMessage();
    message.setDatasetUuid(UUID.fromString("418a6571-b6c1-4db0-b90e-8f36bde4c80e"));
    message.setDatasetType(DatasetType.SAMPLING_EVENT);
    message.setSource(URI.create("http://gbif.vm.ntnu.no/ipt/archive.do?r=setesdal_veg_data"));
    message.setAttempt(109);
    message.setEndpointType(EndpointType.DWC_ARCHIVE);
    message.setPlatform(Platform.ALL);
    message.setPipelineSteps(new HashSet<>(Arrays.asList(DWCA_TO_VERBATIM.name(),VERBATIM_TO_INTERPRETED.name(),INTERPRETED_TO_INDEX.name(),HDFS_VIEW.name())));

    PipelineStep pipelineStep = new PipelineStep()
      .setMessage(message.toString())
      .setRunner(StepRunner.STANDALONE)
      .setType(StepType.DWCA_TO_VERBATIM)
      .setState(Status.RUNNING);

    pipelineStep.setKey(executionKey);

    long stepKey = service.updatePipelineStep(pipelineStep);

    service.runPipelineAttempt(
        datasetKey1, StepType.DWCA_TO_VERBATIM.name(), "test", false, true, null);

    PipelineStep stepCreated = service.getPipelineStep(stepKey);
    assertEquals(Status.ABORTED, stepCreated.getState());
  }

  private UUID createDataset() {
    Node node = new Node();
    node.setTitle("node");
    node.setType(NodeType.COUNTRY);
    node.setParticipationStatus(ParticipationStatus.AFFILIATE);
    nodeService.create(node);

    Organization org = new Organization();
    org.setEndorsingNodeKey(node.getKey());
    org.setTitle("organization");
    org.setDescription("Description organization");
    org.setLanguage(Language.ABKHAZIAN);
    org.setPassword("testtttt");
    org.setEmail(Collections.singletonList("aa@aa.com"));
    org.setPhone(Collections.singletonList("123"));
    org.setCountry(Country.AFGHANISTAN);
    organizationService.create(org);

    Installation installation = new Installation();
    installation.setTitle("title");
    installation.setOrganizationKey(org.getKey());
    installation.setType(InstallationType.IPT_INSTALLATION);
    installationService.create(installation);

    Dataset dataset = new Dataset();
    dataset.setDoi(new DOI("10.1594/pangaea.94668"));
    dataset.setTitle("title");
    dataset.setDescription("description dataset");
    dataset.setInstallationKey(installation.getKey());
    dataset.setPublishingOrganizationKey(org.getKey());
    dataset.setType(DatasetType.OCCURRENCE);
    dataset.setLanguage(Language.ABKHAZIAN);
    dataset.setLicense(License.CC0_1_0);

    return datasetService.create(dataset);
  }
}
