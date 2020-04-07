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

import org.gbif.api.model.pipelines.PipelineExecution;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.StepType;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.registry.persistence.mapper.pipelines.PipelineProcessMapper;

import java.time.LocalDateTime;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class PipelinesHistoryTrackingServiceTest {

  @Mock private PipelineProcessMapper pipelineProcessMapper;

  @Mock private DatasetService datasetService;

  @Mock private MessagePublisher messagePublisher;

  @Test
  public void getLatestSuccesfulStepTest() {
    DefaultRegistryPipelinesHistoryTrackingService trackingService =
        new DefaultRegistryPipelinesHistoryTrackingService(
            new ObjectMapper(), messagePublisher, pipelineProcessMapper, datasetService, 1);

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

    assertEquals(
        s1, trackingService.getLatestSuccessfulStep(process, StepType.ABCD_TO_VERBATIM).get());

    // add newer step
    PipelineStep s3 =
        new PipelineStep()
            .setType(StepType.ABCD_TO_VERBATIM)
            .setStarted(LocalDateTime.now().minusMinutes(40));
    execution.addStep(s3);
    assertEquals(
        s3, trackingService.getLatestSuccessfulStep(process, StepType.ABCD_TO_VERBATIM).get());

    // add older execution with newer step
    PipelineExecution execution2 =
        new PipelineExecution().setCreated(LocalDateTime.now().minusHours(1));
    process.addExecution(execution2);

    PipelineStep s4 =
        new PipelineStep()
            .setType(StepType.ABCD_TO_VERBATIM)
            .setStarted(LocalDateTime.now().minusMinutes(30));
    execution2.addStep(s4);
    assertEquals(
        s4, trackingService.getLatestSuccessfulStep(process, StepType.ABCD_TO_VERBATIM).get());
  }
}
