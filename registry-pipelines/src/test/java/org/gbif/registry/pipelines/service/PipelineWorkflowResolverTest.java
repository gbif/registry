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
package org.gbif.registry.pipelines.service;

import org.gbif.api.model.pipelines.PipelineExecution;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.StepType;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.common.messaging.api.messages.DwcDpStageMessage;
import org.gbif.common.messaging.api.messages.DwcDpToVerbatimMessage;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineWorkflowResolverTest {

  private PipelineWorkflowResolver resolver;
  private final ObjectMapper mapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    resolver = new PipelineWorkflowResolver(mapper);
  }

  // ---- resolveStepTypes — standard workflows ----

  @Test
  void resolveStepTypes_verbatimStep_includesFragmenter() {
    Set<StepType> result =
      resolver.resolveStepTypes(Set.of(StepType.ABCD_TO_VERBATIM), null, null, false, false);

    assertTrue(result.contains(StepType.FRAGMENTER));
  }

  @Test
  void resolveStepTypes_nonVerbatimStep_excludesFragmenter() {
    Set<StepType> result =
      resolver.resolveStepTypes(
        Set.of(StepType.VERBATIM_TO_INTERPRETED), null, null, false, false);

    assertFalse(result.contains(StepType.FRAGMENTER));
  }

  @Test
  void resolveStepTypes_eventStep_returnsEventWorkflow() {
    Set<StepType> result =
      resolver.resolveStepTypes(
        Set.of(StepType.EVENTS_VERBATIM_TO_INTERPRETED), null, null, false, false);

    assertTrue(result.contains(StepType.EVENTS_VERBATIM_TO_INTERPRETED));
    assertTrue(result.contains(StepType.EVENTS_HDFS_VIEW));
    assertTrue(result.contains(StepType.EVENTS_INTERPRETED_TO_INDEX));
    assertFalse(result.contains(StepType.FRAGMENTER));
  }

  @Test
  void resolveStepTypes_samplingEventDataset_returnsEventOccurrenceWorkflow() {
    Dataset dataset = new Dataset();
    dataset.setType(DatasetType.SAMPLING_EVENT);

    Set<StepType> result =
      resolver.resolveStepTypes(
        Set.of(StepType.VERBATIM_TO_INTERPRETED), dataset, null, false, false);

    assertTrue(result.contains(StepType.VERBATIM_TO_INTERPRETED));
    assertTrue(result.contains(StepType.EVENTS_VERBATIM_TO_INTERPRETED));
  }

  @Test
  void resolveStepTypes_onlyIncludeRequestedStep_returnsUnchanged() {
    Set<StepType> steps = Set.of(StepType.VERBATIM_TO_INTERPRETED);
    Set<StepType> result =
      resolver.resolveStepTypes(steps, null, null, false, true);

    assertTrue(result.containsAll(steps));
    assertFalse(result.contains(StepType.INTERPRETED_TO_INDEX));
  }

  // ---- resolveStepTypes — DwC-DP ----

  @Test
  void resolveStepTypes_dwcDp_occurrenceAndEvents_returnsEventOccurrenceWorkflow()
    throws Exception {
    PipelineProcess process = processWith(
      StepType.DWCDP_TO_VERBATIM, dwcDpToVerbatimJson(true, true));

    Set<StepType> result =
      resolver.resolveStepTypes(
        Set.of(StepType.DWCDP_TO_VERBATIM), null, process, false, false);

    assertTrue(result.contains(StepType.DWCDP_TO_VERBATIM));
    assertTrue(result.contains(StepType.VERBATIM_TO_IDENTIFIER));
    assertTrue(result.contains(StepType.VERBATIM_TO_INTERPRETED));
    assertTrue(result.contains(StepType.INTERPRETED_TO_INDEX));
    assertTrue(result.contains(StepType.HDFS_VIEW));
    assertTrue(result.contains(StepType.EVENTS_VERBATIM_TO_INTERPRETED));
    assertTrue(result.contains(StepType.EVENTS_INTERPRETED_TO_INDEX));
    assertTrue(result.contains(StepType.EVENTS_HDFS_VIEW));
  }

  @Test
  void resolveStepTypes_dwcDp_occurrenceOnly_returnsOccurrenceWorkflow() throws Exception {
    PipelineProcess process = processWith(
      StepType.DWCDP_TO_VERBATIM, dwcDpToVerbatimJson(true, false));

    Set<StepType> result =
      resolver.resolveStepTypes(
        Set.of(StepType.DWCDP_TO_VERBATIM), null, process, false, false);

    assertTrue(result.contains(StepType.VERBATIM_TO_IDENTIFIER));
    assertTrue(result.contains(StepType.VERBATIM_TO_INTERPRETED));
    assertFalse(result.contains(StepType.EVENTS_VERBATIM_TO_INTERPRETED));
    assertFalse(result.contains(StepType.EVENTS_INTERPRETED_TO_INDEX));
    assertFalse(result.contains(StepType.EVENTS_HDFS_VIEW));
  }

  @Test
  void resolveStepTypes_dwcDp_eventsOnly_returnsEventWorkflow() throws Exception {
    PipelineProcess process = processWith(
      StepType.DWCDP_TO_VERBATIM, dwcDpToVerbatimJson(false, true));

    Set<StepType> result =
      resolver.resolveStepTypes(
        Set.of(StepType.DWCDP_TO_VERBATIM), null, process, false, false);

    assertTrue(result.contains(StepType.EVENTS_VERBATIM_TO_INTERPRETED));
    assertTrue(result.contains(StepType.EVENTS_INTERPRETED_TO_INDEX));
    assertTrue(result.contains(StepType.EVENTS_HDFS_VIEW));
    assertFalse(result.contains(StepType.VERBATIM_TO_IDENTIFIER));
    assertFalse(result.contains(StepType.VERBATIM_TO_INTERPRETED));
  }

  @Test
  void resolveStepTypes_dwcdpStage_readsNfsMessageFlags() throws Exception {
    PipelineProcess process = processWith(
        StepType.DWCDP_STAGE, dwcdpStageJson(true, true));

    Set<StepType> result =
      resolver.resolveStepTypes(
        Set.of(StepType.DWCDP_STAGE), null, process, false, false);

    assertTrue(result.contains(StepType.DWCDP_STAGE));
    assertTrue(result.contains(StepType.DWCDP_TO_VERBATIM));
    assertTrue(result.contains(StepType.VERBATIM_TO_IDENTIFIER));
    assertTrue(result.contains(StepType.EVENTS_VERBATIM_TO_INTERPRETED));
  }

  @Test
  void resolveStepTypes_dwcDp_noStoredMessage_defaultsToEventOccurrenceSuperset() {
    Set<StepType> result =
      resolver.resolveStepTypes(
        Set.of(StepType.DWCDP_TO_VERBATIM), null, new PipelineProcess(), false, false);

    assertTrue(result.contains(StepType.VERBATIM_TO_IDENTIFIER));
    assertTrue(result.contains(StepType.EVENTS_VERBATIM_TO_INTERPRETED));
  }

  @Test
  void resolveStepTypes_dwcDp_nullProcess_defaultsToEventOccurrenceSuperset() {
    Set<StepType> result =
      resolver.resolveStepTypes(
        Set.of(StepType.DWCDP_TO_VERBATIM), null, null, false, false);

    assertTrue(result.contains(StepType.VERBATIM_TO_IDENTIFIER));
    assertTrue(result.contains(StepType.EVENTS_VERBATIM_TO_INTERPRETED));
  }

  @Test
  void resolveStepTypes_dwcDp_rerunFromPreviousExecution_findsMessageAcrossExecutions()
    throws Exception {
    PipelineExecution firstExecution = executionWith(
      StepType.DWCDP_TO_VERBATIM,
      dwcDpToVerbatimJson(true, true),
      OffsetDateTime.now().minusHours(1));

    PipelineExecution secondExecution =
      new PipelineExecution()
        .setCreated(OffsetDateTime.now())
        .setStepsToRun(Set.of(StepType.DWCDP_TO_VERBATIM));

    PipelineProcess process = new PipelineProcess();
    process.addExecution(firstExecution);
    process.addExecution(secondExecution);

    Set<StepType> result =
      resolver.resolveStepTypes(
        Set.of(StepType.DWCDP_TO_VERBATIM), null, process, false, false);

    assertTrue(result.contains(StepType.VERBATIM_TO_IDENTIFIER));
    assertTrue(result.contains(StepType.EVENTS_VERBATIM_TO_INTERPRETED));
  }

  // ---- deserializeDwcDpMessage ----

  @Test
  void deserializeDwcDpMessage_verbatimMessage_parsedCorrectly() throws Exception {
    String json = dwcDpToVerbatimJson(true, false);
    DwcDpToVerbatimMessage result = resolver.deserializeDwcDpMessage(json).orElseThrow();
    assertTrue(result.isContainsOccurrences());
    assertFalse(result.isContainsEvents());
  }

  @Test
  void deserializeDwcDpMessage_nfsMessage_adaptedCorrectly() throws Exception {
    String json = dwcdpStageJson(false, true);
    DwcDpToVerbatimMessage result = resolver.deserializeDwcDpMessage(json).orElseThrow();
    assertFalse(result.isContainsOccurrences());
    assertTrue(result.isContainsEvents());
  }

  @Test
  void deserializeDwcDpMessage_invalidJson_returnsEmpty() {
    assertTrue(resolver.deserializeDwcDpMessage("not-valid-json").isEmpty());
  }

  // ---- helpers ----

  private String dwcDpToVerbatimJson(boolean containsOccurrences, boolean containsEvents)
    throws Exception {
    return mapper.writeValueAsString(
      new DwcDpToVerbatimMessage(
        UUID.randomUUID(), 1, Set.of(), null,
        containsOccurrences, containsEvents, false));
  }

  private String dwcdpStageJson(boolean containsOccurrences, boolean containsEvents)
    throws Exception {
    return mapper.writeValueAsString(
      new DwcDpStageMessage(
        UUID.randomUUID(), 1, Set.of(), null,
        containsOccurrences, containsEvents));
  }

  private PipelineProcess processWith(StepType stepType, String message) {
    PipelineExecution execution =
      executionWith(stepType, message, OffsetDateTime.now().minusMinutes(10));
    PipelineProcess process = new PipelineProcess();
    process.addExecution(execution);
    return process;
  }

  private PipelineExecution executionWith(
    StepType stepType, String message, OffsetDateTime created) {
    PipelineStep step =
      new PipelineStep()
        .setType(stepType)
        .setStarted(created)
        .setMessage(message);
    PipelineExecution execution =
      new PipelineExecution()
        .setCreated(created)
        .setStepsToRun(Set.of(stepType));
    execution.addStep(step);
    return execution;
  }
}
