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

import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.PipelinesWorkflow;
import org.gbif.api.model.pipelines.StepType;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.common.messaging.api.messages.DwcDpNfsToHdfsMessage;
import org.gbif.common.messaging.api.messages.DwcDpToVerbatimMessage;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Resolves the pipeline workflow graph and full step set for a given set of requested steps.
 *
 * <p>Extracted from {@link DefaultRegistryPipelinesHistoryTrackingService} to keep workflow
 * selection logic cohesive and independently testable.
 *
 * <p>DwC-DP datasets ({@link StepType#NFS_TO_HDFS}, {@link StepType#DWCDP_TO_VERBATIM}) require
 * special handling because their registered {@link DatasetType} may be {@code UNDEFINED}. Instead
 * of relying on the dataset type, the resolver reads {@code containsOccurrences} /
 * {@code containsEvents} flags from the latest successful step message stored in the registry,
 * and selects the appropriate workflow graph from those flags.
 */
@Component
public class PipelineWorkflowResolver {

  private static final Logger LOG = LoggerFactory.getLogger(PipelineWorkflowResolver.class);

  private final ObjectMapper objectMapper;

  public PipelineWorkflowResolver(
    @Qualifier("registryObjectMapper") ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Resolves the full set of steps an execution should track, expanding the requested steps
   * to the full reachable set in the appropriate workflow graph.
   *
   * <p>If {@code onlyIncludeRequestedStep} is true, returns {@code stepsToSend} unchanged.
   * Otherwise, {@link StepType#FRAGMENTER} is removed unless a verbatim step is in the requested set.
   */
  public Set<StepType> resolveStepTypes(
    Set<StepType> stepsToSend,
    Dataset dataset,
    PipelineProcess process,
    boolean excludeEventSteps,
    boolean onlyIncludeRequestedStep) {

    if (onlyIncludeRequestedStep) {
      LOG.warn("Only include requested steps {}", stepsToSend);
      return stepsToSend;
    }

    Set<StepType> finalSteps = new HashSet<>(
      resolveWorkflow(stepsToSend, dataset, process, excludeEventSteps)
        .getAllNodesFor(stepsToSend));

    if (stepsToSend.stream().noneMatch(StepType::isVerbatimType)) {
      finalSteps.remove(StepType.FRAGMENTER);
    }

    LOG.info("Resolved finalSteps {} for requested {}", finalSteps, stepsToSend);
    return finalSteps;
  }

  /**
   * Selects the workflow graph for the given steps.
   *
   * <p>DwC-DP steps are handled first since the dataset registered type may be UNDEFINED;
   * the flags are read from the stored step message instead. All other paths fall through
   * to type-based selection.
   */
  PipelinesWorkflow.Graph<StepType> resolveWorkflow(
    Set<StepType> stepsToSend,
    Dataset dataset,
    PipelineProcess process,
    boolean excludeEventSteps) {

    if (isDwcDpSteps(stepsToSend)) {
      return resolveDwcDpWorkflow(process);
    }

    boolean hasEventSteps = stepsToSend.stream().anyMatch(StepType::isEventType);
    boolean hasOccurrenceOrVerbatimSteps =
      stepsToSend.stream().anyMatch(StepType::isOccurrenceType)
        || stepsToSend.stream().anyMatch(StepType::isVerbatimType);

    if (hasEventSteps && hasOccurrenceOrVerbatimSteps) {
      return PipelinesWorkflow.getEventOccurrenceWorkflow();
    }
    if (hasEventSteps) {
      return PipelinesWorkflow.getEventWorkflow();
    }
    if (!excludeEventSteps && dataset != null && dataset.getType() == DatasetType.SAMPLING_EVENT) {
      return PipelinesWorkflow.getEventOccurrenceWorkflow();
    }
    return PipelinesWorkflow.getOccurrenceWorkflow();
  }

  boolean isDwcDpSteps(Set<StepType> steps) {
    return steps.contains(StepType.NFS_TO_HDFS)
      || steps.contains(StepType.DWCDP_TO_VERBATIM);
  }

  /**
   * Resolves the workflow graph for DwC-DP steps by reading containsOccurrences/containsEvents
   * from the latest successful DWCDP_TO_VERBATIM or NFS_TO_HDFS step message stored in the
   * registry. Falls back to the EVENT_OCCURRENCE superset if no message can be found or parsed,
   * since that graph covers all possible DwC-DP step combinations.
   */
  PipelinesWorkflow.Graph<StepType> resolveDwcDpWorkflow(PipelineProcess process) {
    return findDwcDpMessage(process)
      .map(msg -> PipelinesWorkflow.getWorkflow(
        msg.isContainsOccurrences(), msg.isContainsEvents()))
      .orElseGet(() -> {
        LOG.warn(
          "Could not find DwC-DP step message for process {}, "
            + "defaulting to EVENT_OCCURRENCE superset",
          process != null ? process.getDatasetKey() : "null");
        return PipelinesWorkflow.getEventOccurrenceWorkflow();
      });
  }

  /**
   * Finds the latest successful DwC-DP step message from the process history, trying
   * DWCDP_TO_VERBATIM first then NFS_TO_HDFS as fallback. Deserializes to
   * DwcDpToVerbatimMessage in both cases — NFS_TO_HDFS messages are adapted since they
   * carry the same containsOccurrences/containsEvents flags.
   */
  Optional<DwcDpToVerbatimMessage> findDwcDpMessage(PipelineProcess process) {
    if (process == null) {
      return Optional.empty();
    }

    return getLatestSuccessfulStep(process, StepType.DWCDP_TO_VERBATIM)
      .or(() -> getLatestSuccessfulStep(process, StepType.NFS_TO_HDFS))
      .flatMap(step -> deserializeDwcDpMessage(step.getMessage()));
  }

  Optional<DwcDpToVerbatimMessage> deserializeDwcDpMessage(String json) {
    try {
      return Optional.of(objectMapper.readValue(json, DwcDpToVerbatimMessage.class));
    } catch (JsonProcessingException e) {
      LOG.debug("Could not deserialize as DwcDpToVerbatimMessage, trying NFS message");
    }

    try {
      DwcDpNfsToHdfsMessage nfsMsg = objectMapper.readValue(json, DwcDpNfsToHdfsMessage.class);
      return Optional.of(new DwcDpToVerbatimMessage(
        nfsMsg.getDatasetUuid(),
        nfsMsg.getAttempt(),
        nfsMsg.getPipelineSteps(),
        nfsMsg.getExecutionId(),
        nfsMsg.isContainsOccurrences(),
        nfsMsg.isContainsEvents(),
        false));
    } catch (JsonProcessingException ex) {
      LOG.warn("Could not deserialize DwC-DP step message: {}", ex.getMessage());
      return Optional.empty();
    }
  }

  private Optional<PipelineStep> getLatestSuccessfulStep(
    PipelineProcess process, StepType stepType) {
    return process.getExecutions().stream()
      .filter(ex -> !ex.getStepsToRun().isEmpty())
      .sorted(Comparator.comparing(
        org.gbif.api.model.pipelines.PipelineExecution::getCreated).reversed())
      .flatMap(ex -> ex.getSteps().stream())
      .filter(s -> stepType.equals(s.getType()))
      .filter(s -> s.getMessage() != null && !s.getMessage().isEmpty())
      .max(Comparator.comparing(PipelineStep::getStarted));
  }
}
