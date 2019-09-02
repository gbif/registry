package org.gbif.registry.pipelines;

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.StepRunner;
import org.gbif.api.model.pipelines.StepType;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.*;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.database.LiquibaseModules;
import org.gbif.registry.grizzly.RegistryServer;
import org.gbif.registry.ws.client.pipelines.PipelinesHistoryWsClient;
import org.gbif.registry.ws.fixtures.TestConstants;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.UUID;

import com.google.inject.Injector;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import sun.security.ssl.HandshakeInStream;

import static org.gbif.registry.guice.RegistryTestModules.webservice;
import static org.gbif.registry.guice.RegistryTestModules.webserviceClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PipelinesHistoryResourceIT {

  private final DatasetService datasetService;
  private final OrganizationService organizationService;
  private final NodeService nodeService;
  private final InstallationService installationService;
  private final PipelinesHistoryWsClient historyWsClient;
  private final SimplePrincipalProvider principalProvider;

  @ClassRule
  public static final LiquibaseInitializer liquibaseRule =
      new LiquibaseInitializer(LiquibaseModules.database());

  @ClassRule public static final RegistryServer registryServer = RegistryServer.INSTANCE;

  @Rule
  public final DatabaseInitializer databaseRule =
      new DatabaseInitializer(LiquibaseModules.database());

  public PipelinesHistoryResourceIT() {
    final Injector serviceInjector = webservice();
    datasetService = serviceInjector.getInstance(DatasetService.class);
    organizationService = serviceInjector.getInstance(OrganizationService.class);
    nodeService = serviceInjector.getInstance(NodeService.class);
    installationService = serviceInjector.getInstance(InstallationService.class);

    final Injector clientInjector = webserviceClient();
    historyWsClient = clientInjector.getInstance(PipelinesHistoryWsClient.class);
    principalProvider = clientInjector.getInstance(SimplePrincipalProvider.class);
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

    long key = historyWsClient.createPipelineProcess(datasetKey, attempt);
    assertTrue(key > 0);

    PipelineProcess processCreated = historyWsClient.getPipelineProcess(datasetKey, attempt);
    assertEquals(datasetKey, processCreated.getDatasetKey());
    assertEquals(attempt, processCreated.getAttempt());
    assertNotNull(processCreated.getCreated());
    assertEquals(TestConstants.TEST_ADMIN, processCreated.getCreatedBy());
  }

  @Test
  public void historyTest() {
    final UUID datasetKey1 = createDataset();
    final UUID datasetKey2 = createDataset();

    historyWsClient.createPipelineProcess(datasetKey1, 1);
    historyWsClient.createPipelineProcess(datasetKey1, 2);
    historyWsClient.createPipelineProcess(datasetKey1, 3);
    historyWsClient.createPipelineProcess(datasetKey2, 1);
    historyWsClient.createPipelineProcess(datasetKey2, 2);

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
    long processKey = historyWsClient.createPipelineProcess(datasetKey1, 1);

    PipelineStep step =
        new PipelineStep()
            .setMessage("message")
            .setRunner(StepRunner.STANDALONE)
            .setType(StepType.ABCD_TO_VERBATIM)
            .setState(PipelineStep.Status.RUNNING);
    long stepKey = historyWsClient.addPipelineStep(processKey, step);
    assertTrue(stepKey > 0);

    PipelineStep stepCreated = historyWsClient.getPipelineStep(processKey, stepKey);
    assertNull(stepCreated.getFinished());
    assertTrue(stepCreated.lenientEquals(step));
  }

  @Test
  public void updatePipelineStepStatusTest() {
    final UUID datasetKey1 = createDataset();
    long processKey = historyWsClient.createPipelineProcess(datasetKey1, 1);

    PipelineStep step =
      new PipelineStep()
        .setMessage("message")
        .setRunner(StepRunner.STANDALONE)
        .setType(StepType.ABCD_TO_VERBATIM)
        .setState(PipelineStep.Status.RUNNING);
    long stepKey = historyWsClient.addPipelineStep(processKey, step);

    historyWsClient.updatePipelineStep(processKey, stepKey, PipelineStep.Status.COMPLETED);

    PipelineStep stepCreated = historyWsClient.getPipelineStep(processKey, stepKey);
    assertEquals(PipelineStep.Status.COMPLETED, stepCreated.getState());
    assertNotNull(stepCreated.getFinished());
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
    UUID datasetKey = datasetService.create(dataset);

    return datasetKey;
  }
}
