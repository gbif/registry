package org.gbif.registry.pipelines;

import org.gbif.api.model.pipelines.PipelineExecution;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.StepType;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.registry.persistence.mapper.pipelines.PipelineProcessMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class PipelinesHistoryTrackingServiceTest {

  @Mock
  private PipelineProcessMapper pipelineProcessMapperMock;

  @Mock
  private DatasetService datasetServiceMock;

  @InjectMocks
  private DefaultPipelinesHistoryTrackingService trackingService;

  @Test
  public void getLatestSuccesfulStepTest() {
    PipelineProcess process = new PipelineProcess();
    PipelineExecution execution = new PipelineExecution().setCreated(LocalDateTime.now());
    process.addExecution(execution);

    PipelineStep s1 =
      new PipelineStep()
        .setType(StepType.ABCD_TO_VERBATIM)
        .setStarted(LocalDateTime.now().minusMinutes(50));
    execution.addStep(s1);

    PipelineStep s2 =
      new PipelineStep()
        .setType(StepType.ABCD_TO_VERBATIM)
        .setStarted(LocalDateTime.now().minusMinutes(60));
    execution.addStep(s1);

    assertEquals(s1, trackingService.getLatestSuccessfulStep(process, StepType.ABCD_TO_VERBATIM).get());

    // add newer step
    PipelineStep s3 =
      new PipelineStep()
        .setType(StepType.ABCD_TO_VERBATIM)
        .setStarted(LocalDateTime.now().minusMinutes(40));
    execution.addStep(s3);
    assertEquals(s3, trackingService.getLatestSuccessfulStep(process, StepType.ABCD_TO_VERBATIM).get());

    // add older execution with newer step
    PipelineExecution execution2 = new PipelineExecution().setCreated(LocalDateTime.now().minusHours(1));
    process.addExecution(execution2);

    PipelineStep s4 =
      new PipelineStep()
        .setType(StepType.ABCD_TO_VERBATIM)
        .setStarted(LocalDateTime.now().minusMinutes(30));
    execution2.addStep(s4);
    assertEquals(s4, trackingService.getLatestSuccessfulStep(process, StepType.ABCD_TO_VERBATIM).get());
  }
}
