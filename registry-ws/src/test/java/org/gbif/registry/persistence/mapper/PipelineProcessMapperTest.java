package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.StepRunner;
import org.gbif.api.model.pipelines.StepType;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.*;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.database.LiquibaseModules;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.persistence.mapper.pipelines.PipelineProcessMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import java.util.function.BiFunction;

import com.google.inject.Injector;
import org.apache.ibatis.exceptions.PersistenceException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.postgresql.util.PSQLException;

import static org.gbif.api.model.pipelines.PipelineStep.MetricInfo;
import static org.gbif.api.model.pipelines.PipelineStep.Status;

import static org.hamcrest.core.Is.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PipelineProcessMapperTest {

  private static final String TEST_USER = "test";
  private static final String UPDATER_USER = "updater";

  private static final BiFunction<Integer, Long, Pageable> PAGE_FN =
      (limit, offset) ->
          new Pageable() {
            @Override
            public int getLimit() {
              return limit;
            }

            @Override
            public long getOffset() {
              return offset;
            }
          };

  private static final Pageable DEFAULT_PAGE = PAGE_FN.apply(10, 0L);

  @Rule public ExpectedException expectedException = ExpectedException.none();

  private PipelineProcessMapper pipelineProcessMapper;
  private DatasetMapper datasetMapper;
  private InstallationMapper installationMapper;
  private OrganizationMapper organizationMapper;
  private NodeMapper nodeMapper;

  @ClassRule
  public static LiquibaseInitializer liquibase =
      new LiquibaseInitializer(LiquibaseModules.database());

  @Rule
  public final DatabaseInitializer databaseRule =
      new DatabaseInitializer(LiquibaseModules.database());

  @Before
  public void setup() {
    Injector inj = RegistryTestModules.mybatis();
    pipelineProcessMapper = inj.getInstance(PipelineProcessMapper.class);
    datasetMapper = inj.getInstance(DatasetMapper.class);
    installationMapper = inj.getInstance(InstallationMapper.class);
    organizationMapper = inj.getInstance(OrganizationMapper.class);
    nodeMapper = inj.getInstance(NodeMapper.class);
  }

  @Test
  public void getPipelinesProcessByKeyTest() {
    // create process
    PipelineProcess process =
        new PipelineProcess().setDatasetKey(insertDataset()).setAttempt(1).setCreatedBy(TEST_USER);

    // insert in the DB
    pipelineProcessMapper.create(process);
    assertTrue(process.getKey() > 0);

    // get by key
    PipelineProcess processRetrieved = pipelineProcessMapper.get(process.getKey());
    assertEquals(process.getAttempt(), processRetrieved.getAttempt());
    assertEquals(process.getDatasetKey(), processRetrieved.getDatasetKey());
  }

  @Test
  public void createPipelinesProcessTest() {
    // create process
    PipelineProcess process =
        new PipelineProcess().setDatasetKey(insertDataset()).setAttempt(1).setCreatedBy(TEST_USER);

    // insert in the DB
    pipelineProcessMapper.create(process);
    assertTrue(process.getKey() > 0);

    // get process inserted
    PipelineProcess processRetrieved =
        pipelineProcessMapper.getByDatasetAndAttempt(process.getDatasetKey(), process.getAttempt());
    assertEquals(process.getDatasetKey(), processRetrieved.getDatasetKey());
    assertEquals(process.getAttempt(), processRetrieved.getAttempt());
    assertTrue(process.getSteps().isEmpty());
  }

  @Test
  public void duplicatePipelinesProcessTest() {
    // insert one process
    final UUID datasetKey = insertDataset();
    final int attempt = 1;

    PipelineProcess process =
        new PipelineProcess().setDatasetKey(datasetKey).setAttempt(attempt).setCreatedBy(TEST_USER);
    pipelineProcessMapper.create(process);

    // insert another process with the same datasetKey and attempt
    PipelineProcess duplicate =
        new PipelineProcess()
            .setDatasetKey(datasetKey)
            .setAttempt(attempt)
            .setCreatedBy(TEST_USER);
    pipelineProcessMapper.create(duplicate);

    assertEquals(process.getKey(), duplicate.getKey());
    assertEquals(datasetKey, duplicate.getDatasetKey());
    assertEquals(attempt, duplicate.getAttempt());
  }

  @Test
  public void addStepTest() {
    // insert one process
    PipelineProcess process =
        new PipelineProcess().setDatasetKey(insertDataset()).setAttempt(1).setCreatedBy(TEST_USER);
    pipelineProcessMapper.create(process);

    // add a step
    PipelineStep step =
        new PipelineStep()
            .setType(StepType.ABCD_TO_VERBATIM)
            .setRunner(StepRunner.STANDALONE)
            .setState(Status.COMPLETED)
            .setStarted(LocalDateTime.now().minusMinutes(1))
            .setFinished(LocalDateTime.now())
            .setMessage("message")
            .setMetrics(Collections.singleton(new MetricInfo("n", "v")))
            .setCreatedBy(TEST_USER);
    pipelineProcessMapper.addPipelineStep(process.getKey(), step);
    assertTrue(step.getKey() > 0);

    // assert results
    PipelineProcess processRetrieved =
        pipelineProcessMapper.getByDatasetAndAttempt(process.getDatasetKey(), process.getAttempt());
    assertEquals(process.getDatasetKey(), processRetrieved.getDatasetKey());
    assertEquals(process.getAttempt(), processRetrieved.getAttempt());
    assertEquals(1, processRetrieved.getSteps().size());
    assertTrue(step.lenientEquals(processRetrieved.getSteps().iterator().next()));
  }

  @Test
  public void listAndCountTest() {
    // insert some processes
    final UUID uuid1 = insertDataset();
    final UUID uuid2 = insertDataset();
    pipelineProcessMapper.create(
        new PipelineProcess().setDatasetKey(uuid1).setAttempt(1).setCreatedBy(TEST_USER));
    pipelineProcessMapper.create(
        new PipelineProcess().setDatasetKey(uuid1).setAttempt(2).setCreatedBy(TEST_USER));
    pipelineProcessMapper.create(
        new PipelineProcess().setDatasetKey(uuid2).setAttempt(1).setCreatedBy(TEST_USER));

    // list processes
    assertListResult(null, null, 3);
    assertListResult(uuid1, null, 2);
    assertListResult(uuid2, null, 1);
    assertListResult(uuid2, 1, 1);
    assertListResult(uuid2, 10, 0);
    assertListResult(null, 1, 2);
    assertListResult(null, 10, 0);
  }

  private void assertListResult(UUID datasetKey, Integer attempt, int expectedResult) {
    assertEquals(expectedResult, pipelineProcessMapper.count(datasetKey, attempt));
    assertEquals(
        expectedResult, pipelineProcessMapper.list(datasetKey, attempt, DEFAULT_PAGE).size());
  }

  @Test
  public void getPipelineStepTest() {
    // insert one process
    PipelineProcess process =
        new PipelineProcess().setDatasetKey(insertDataset()).setAttempt(1).setCreatedBy(TEST_USER);
    pipelineProcessMapper.create(process);

    // add a step
    PipelineStep step =
        new PipelineStep()
            .setType(StepType.ABCD_TO_VERBATIM)
            .setState(Status.RUNNING)
            .setCreatedBy(TEST_USER);
    pipelineProcessMapper.addPipelineStep(process.getKey(), step);

    // get step
    PipelineStep stepRetrieved = pipelineProcessMapper.getPipelineStep(step.getKey());
    assertTrue(stepRetrieved.lenientEquals(step));
  }

  @Test
  public void updatePipelineStepTest() {
    // insert one process
    PipelineProcess process =
        new PipelineProcess().setDatasetKey(insertDataset()).setAttempt(1).setCreatedBy(TEST_USER);
    pipelineProcessMapper.create(process);

    // add a step
    PipelineStep step =
        new PipelineStep()
            .setType(StepType.ABCD_TO_VERBATIM)
            .setStarted(LocalDateTime.now())
            .setState(Status.RUNNING)
            .setCreatedBy(TEST_USER);
    pipelineProcessMapper.addPipelineStep(process.getKey(), step);
    assertEquals(Status.RUNNING, pipelineProcessMapper.getPipelineStep(step.getKey()).getState());

    // change step state
    step.setFinished(LocalDateTime.now().plusHours(1));
    step.setState(Status.COMPLETED);
    step.setModifiedBy(UPDATER_USER);
    step.setRerunReason("reason");
    step.setMetrics(Collections.singleton(new MetricInfo("name", "val")));

    pipelineProcessMapper.updatePipelineStep(step);
    assertTrue(pipelineProcessMapper.getPipelineStep(step.getKey()).lenientEquals(step));
  }

  @Test
  public void getLastAttemptTest() {
    final UUID uuid1 = insertDataset();

    // shouldn't find any attempt
    assertFalse(pipelineProcessMapper.getLastAttempt(uuid1).isPresent());

    // insert some processes
    pipelineProcessMapper.create(
        new PipelineProcess().setDatasetKey(uuid1).setAttempt(1).setCreatedBy(TEST_USER));
    pipelineProcessMapper.create(
        new PipelineProcess().setDatasetKey(uuid1).setAttempt(2).setCreatedBy(TEST_USER));

    // get last attempt
    assertEquals(2, pipelineProcessMapper.getLastAttempt(uuid1).get().intValue());

    // add new attempt
    pipelineProcessMapper.create(
        new PipelineProcess().setDatasetKey(uuid1).setAttempt(3).setCreatedBy(TEST_USER));
    assertEquals(3, pipelineProcessMapper.getLastAttempt(uuid1).get().intValue());
  }

  private UUID insertDataset() {
    Node node = new Node();
    node.setKey(UUID.randomUUID());
    node.setTitle("node");
    node.setType(NodeType.COUNTRY);
    node.setParticipationStatus(ParticipationStatus.AFFILIATE);
    node.setCreatedBy("test");
    nodeMapper.create(node);

    Organization org = new Organization();
    org.setKey(UUID.randomUUID());
    org.setEndorsingNodeKey(node.getKey());
    org.setTitle("organization");
    org.setLanguage(Language.ABKHAZIAN);
    org.setPassword("testtttt");
    org.setCreatedBy("test");
    organizationMapper.create(org);

    Installation installation = new Installation();
    installation.setKey(UUID.randomUUID());
    installation.setTitle("title");
    installation.setOrganizationKey(org.getKey());
    installation.setType(InstallationType.BIOCASE_INSTALLATION);
    installation.setCreatedBy("test");
    installationMapper.create(installation);

    Dataset dataset = new Dataset();
    dataset.setKey(UUID.randomUUID());
    dataset.setTitle("title");
    dataset.setInstallationKey(installation.getKey());
    dataset.setPublishingOrganizationKey(org.getKey());
    dataset.setType(DatasetType.CHECKLIST);
    dataset.setLanguage(Language.ABKHAZIAN);
    dataset.setLicense(License.CC0_1_0);
    dataset.setCreatedBy("test");
    datasetMapper.create(dataset);

    return dataset.getKey();
  }
}
