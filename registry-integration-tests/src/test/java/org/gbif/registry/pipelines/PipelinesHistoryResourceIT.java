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
package org.gbif.registry.pipelines;

/** Tests the {@link org.gbif.registry.ws.resources.pipelines.PipelinesHistoryResource}. */
public class PipelinesHistoryResourceIT {

  public boolean makeSpotlessHappy() {
    return true;
  }

  /* private final DatasetService datasetService;
  private final OrganizationService organizationService;
  private final NodeService nodeService;
  private final InstallationService installationService;
  private final PipelinesHistoryWsClient historyWsClient;
  private final SimplePrincipalProvider principalProvider;

  @RegisterExtension
  static PreparedDbExtension database =
      EmbeddedPostgresExtension.preparedDatabase(
          LiquibasePreparer.forClasspathLocation("liquibase/master.xml"));

  @RegisterExtension
  public final DatabaseInitializer databaseRule =
      new DatabaseInitializer(database.getTestDatabase());

  public PipelinesHistoryResourceIT(
      DatasetService datasetService,
      OrganizationService organizationService,
      NodeService nodeService,
      InstallationService installationService,
      PipelinesHistoryWsClient pipelinesHistoryWsClient) {
    this.datasetService = datasetService;
    this.organizationService = organizationService;
    this.nodeService = nodeService;
    this.installationService = installationService;

    historyWsClient = pipelinesHistoryWsClient;
    principalProvider = new SimplePrincipalProvider();
  }

  @Before
  public void setup() {
    if (principalProvider != null) {
      principalProvider.setPrincipal(TestConstants.TEST_ADMIN);
    }
  }

  @Test
  public void createPipelineProcessTest() {
    final UUID datasetKey = createDataset();
    final int attempt = 1;

    long key = historyWsClient.createOrGetPipelineProcess(datasetKey, attempt);
    assertTrue(key > 0);

    PipelineProcess processCreated = historyWsClient.getPipelineProcess(datasetKey, attempt);
    assertEquals(datasetKey, processCreated.getDatasetKey());
    assertEquals(attempt, processCreated.getAttempt());
    assertNotNull(processCreated.getCreated());
    assertEquals(TestConstants.TEST_ADMIN, processCreated.getCreatedBy());
  }

  @Test(expected = AccessControlException.class)
  public void createPipelineProcessWithoutPrivilegesTest() {
    principalProvider.setPrincipal(TestConstants.TEST_USER);
    historyWsClient.createOrGetPipelineProcess(UUID.randomUUID(), 1);
  }

  @Test
  public void getNonExistentPipelineProcessTest() {
    assertNull(historyWsClient.getPipelineProcess(UUID.randomUUID(), 0));
  }

  @Test
  public void historyTest() {
    final UUID datasetKey1 = createDataset();
    final UUID datasetKey2 = createDataset();

    historyWsClient.createOrGetPipelineProcess(datasetKey1, 1);
    historyWsClient.createOrGetPipelineProcess(datasetKey1, 2);
    historyWsClient.createOrGetPipelineProcess(datasetKey1, 3);
    historyWsClient.createOrGetPipelineProcess(datasetKey2, 1);
    historyWsClient.createOrGetPipelineProcess(datasetKey2, 2);

    PagingResponse<PipelineProcess> processes = historyWsClient.history(null);
    assertEquals(5, processes.getCount().longValue());

    processes = historyWsClient.history(datasetKey1, null);
    assertEquals(3, processes.getCount().longValue());

    processes = historyWsClient.history(datasetKey2, null);
    assertEquals(2, processes.getCount().longValue());
  }

  @Test
  public void addPipelineStepTest() {
    final UUID datasetKey1 = createDataset();
    long processKey = historyWsClient.createOrGetPipelineProcess(datasetKey1, 1);

    PipelineExecution execution =
        new PipelineExecution()
            .setStepsToRun(Collections.singletonList(StepType.DWCA_TO_VERBATIM))
            .setRerunReason("rerun")
            .setRemarks("remarks");
    long executionKey = historyWsClient.addPipelineExecution(processKey, execution);

    PipelineStep step =
        new PipelineStep()
            .setMessage("message")
            .setRunner(StepRunner.STANDALONE)
            .setType(StepType.ABCD_TO_VERBATIM)
            .setState(PipelineStep.Status.RUNNING);
    long stepKey = historyWsClient.addPipelineStep(processKey, executionKey, step);
    assertTrue(stepKey > 0);

    PipelineStep stepCreated = historyWsClient.getPipelineStep(processKey, executionKey, stepKey);
    assertNull(stepCreated.getFinished());
    assertTrue(stepCreated.lenientEquals(step));
  }

  @Test(expected = AccessControlException.class)
  public void addPipelineStepWithoutPrivilegesTest() {
    final UUID datasetKey1 = createDataset();
    long processKey = historyWsClient.createOrGetPipelineProcess(datasetKey1, 1);
    assertTrue(processKey > 0);

    long executionKey = historyWsClient.addPipelineExecution(processKey, new PipelineExecution());

    principalProvider.setPrincipal(TestConstants.TEST_USER);
    PipelineStep step =
        new PipelineStep()
            .setMessage("message")
            .setRunner(StepRunner.STANDALONE)
            .setType(StepType.ABCD_TO_VERBATIM)
            .setState(PipelineStep.Status.RUNNING);
    historyWsClient.addPipelineStep(processKey, executionKey, step);
  }

  @Test
  public void updatePipelineStepStatusAndMetricsTest() {
    final UUID datasetKey1 = createDataset();
    long processKey = historyWsClient.createOrGetPipelineProcess(datasetKey1, 1);

    PipelineExecution execution =
        new PipelineExecution()
            .setStepsToRun(Collections.singletonList(StepType.DWCA_TO_VERBATIM))
            .setRerunReason("rerun")
            .setRemarks("remarks");
    long executionKey = historyWsClient.addPipelineExecution(processKey, execution);

    PipelineStep step =
        new PipelineStep()
            .setMessage("message")
            .setRunner(StepRunner.STANDALONE)
            .setType(StepType.ABCD_TO_VERBATIM)
            .setState(PipelineStep.Status.RUNNING);
    long stepKey = historyWsClient.addPipelineStep(processKey, executionKey, step);

    PipelineStepParameters stepParams =
        new PipelineStepParameters(
            PipelineStep.Status.COMPLETED,
            Collections.singletonList(new PipelineStep.MetricInfo("name", "value")));
    historyWsClient.updatePipelineStepStatusAndMetrics(
        processKey, executionKey, stepKey, stepParams);

    PipelineStep stepCreated = historyWsClient.getPipelineStep(processKey, executionKey, stepKey);
    assertEquals(PipelineStep.Status.COMPLETED, stepCreated.getState());
    assertNotNull(stepCreated.getFinished());
    assertEquals(1, stepCreated.getMetrics().size());
    assertEquals("value", stepCreated.getMetrics().iterator().next().getValue());
  }

  @Test
  public void getPipelineWorkflowNonExistentProcessTest() {
    assertNull(historyWsClient.getPipelineProcess(UUID.randomUUID(), 1));
  }

  @Test
  public void runPipelineAttemptTest() {
    // create one process with one step
    final UUID datasetKey1 = createDataset();
    final int attempt = 1;
    long processKey = historyWsClient.createOrGetPipelineProcess(datasetKey1, attempt);

    PipelineExecution execution =
        new PipelineExecution()
            .setStepsToRun(Collections.singletonList(StepType.DWCA_TO_VERBATIM))
            .setRerunReason("rerun")
            .setRemarks("remarks");
    long executionKey = historyWsClient.addPipelineExecution(processKey, execution);

    historyWsClient.addPipelineStep(
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
    historyWsClient.runPipelineAttempt(
        datasetKey1, attempt, StepType.DWCA_TO_VERBATIM.name(), rerunReason);

    // check that the DB was updated
    PipelineProcess process = historyWsClient.getPipelineProcess(datasetKey1, attempt);
    assertEquals(rerunReason, process.getExecutions().iterator().next().getRerunReason());

    // run the process without attempt now
    final String rerunReason2 = "test reason 2";
    historyWsClient.runPipelineAttempt(datasetKey1, StepType.DWCA_TO_VERBATIM.name(), rerunReason2);

    // check that the DB was updated again
    process = historyWsClient.getPipelineProcess(datasetKey1, attempt);
    assertEquals(3, process.getExecutions().size());
    process.getExecutions().forEach(e -> assertNotNull(e.getRerunReason()));
  }

  @Test
  public void runPipelineAttemptInRunningStateTest() {
    // create one process with one step
    final UUID datasetKey1 = createDataset();
    final int attempt = 1;
    long processKey = historyWsClient.createOrGetPipelineProcess(datasetKey1, attempt);

    PipelineExecution execution =
        new PipelineExecution()
            .setStepsToRun(Collections.singletonList(StepType.DWCA_TO_VERBATIM))
            .setRerunReason("rerun")
            .setRemarks("remarks");
    long executionKey = historyWsClient.addPipelineExecution(processKey, execution);

    historyWsClient.addPipelineStep(
        processKey,
        executionKey,
        new PipelineStep()
            .setMessage("message")
            .setRunner(StepRunner.STANDALONE)
            .setType(StepType.ABCD_TO_VERBATIM)
            .setState(PipelineStep.Status.RUNNING));

    // run process and expect a bad request since the step is in running state
    ClientResponse response =
        historyWsClient.runPipelineAttempt(
            datasetKey1, attempt, StepType.ABCD_TO_VERBATIM.name(), "test");
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

    // run process without attempt and expect a bad request since the step is in running state
    response =
        historyWsClient.runPipelineAttempt(datasetKey1, StepType.ABCD_TO_VERBATIM.name(), "test");
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
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
  }*/
}
