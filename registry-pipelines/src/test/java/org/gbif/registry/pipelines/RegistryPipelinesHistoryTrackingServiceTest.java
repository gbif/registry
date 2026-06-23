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
package org.gbif.registry.pipelines;

import org.gbif.api.model.pipelines.PipelineExecution;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.StepType;

import java.time.OffsetDateTime;
import java.util.Set;

import org.gbif.registry.pipelines.service.PipelineWorkflowResolver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class RegistryPipelinesHistoryTrackingServiceTest {

  // Workflow resolution logic is tested independently in PipelineWorkflowResolverTest.
  // These tests cover the service's own concerns: getLatestSuccessfulStep and the
  // delegation from getStepTypes to PipelineWorkflowResolver.
  @Spy private PipelineWorkflowResolver workflowResolver =
    new PipelineWorkflowResolver(new com.fasterxml.jackson.databind.ObjectMapper());
  @InjectMocks private DefaultRegistryPipelinesHistoryTrackingService trackingService;

  @Test
  void getLatestSuccessfulStep_returnsStepWithMessage() {
    PipelineExecution execution = new PipelineExecution().setCreated(OffsetDateTime.now());

    PipelineStep s1 =
        new PipelineStep()
            .setType(StepType.ABCD_TO_VERBATIM)
            .setStarted(OffsetDateTime.now().minusMinutes(100))
            .setMessage("adwadawd");

    PipelineStep s2 =
        new PipelineStep()
            .setType(StepType.ABCD_TO_VERBATIM)
            .setStarted(OffsetDateTime.now().minusMinutes(80));

    PipelineStep s3 =
        new PipelineStep()
            .setType(StepType.ABCD_TO_VERBATIM)
            .setStarted(OffsetDateTime.now().minusMinutes(60));

    execution.addStep(s1);
    execution.addStep(s2);
    execution.addStep(s3);
    execution.setStepsToRun(Set.of(StepType.ABCD_TO_VERBATIM));

    PipelineProcess process = new PipelineProcess();
    process.addExecution(execution);

    PipelineStep step1 = trackingService.getLatestSuccessfulStep(process, s1.getType()).orElseThrow();
    assertEquals(s1, step1);
  }

  @Test
  void getStepTypes_verbatimStep_includesFragmenter() {
    Set<StepType> result =
        trackingService.getStepTypes(Set.of(StepType.ABCD_TO_VERBATIM), null, null, false, false);

    assertTrue(result.contains(StepType.FRAGMENTER));
  }

  @Test
  void getStepTypes_nonVerbatimStep_excludesFragmenter() {
    Set<StepType> result =
        trackingService.getStepTypes(Set.of(StepType.VERBATIM_TO_INTERPRETED), null, null, false, false);

    assertFalse(result.contains(StepType.FRAGMENTER));
  }

  @Test
  void getStepTypes_eventStep_returnsEventSteps() {
    Set<StepType> result =
        trackingService.getStepTypes(Set.of(StepType.EVENTS_VERBATIM_TO_INTERPRETED), null, null, false, false);

    assertTrue(result.contains(StepType.EVENTS_VERBATIM_TO_INTERPRETED));
    assertTrue(result.contains(StepType.EVENTS_HDFS_VIEW));
    assertTrue(result.contains(StepType.EVENTS_INTERPRETED_TO_INDEX));
    assertFalse(result.contains(StepType.FRAGMENTER));
  }
}
