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
package org.gbif.registry.ws.resources.pipelines;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.pipelines.PipelineExecution;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.StepType;
import org.gbif.api.model.pipelines.ws.PipelineProcessParameters;
import org.gbif.api.model.pipelines.ws.PipelineStepParameters;
import org.gbif.registry.domain.pipelines.RunPipelineResponse;
import org.gbif.registry.pipelines.PipelinesHistoryTrackingService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Preconditions;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.EDITOR_ROLE;

/** Pipelines History service. */
@RestController
@RequestMapping(value = "pipelines/history", produces = MediaType.APPLICATION_JSON_VALUE)
public class PipelinesHistoryResource {

  private final PipelinesHistoryTrackingService historyTrackingService;

  public PipelinesHistoryResource(PipelinesHistoryTrackingService historyTrackingService) {
    this.historyTrackingService = historyTrackingService;
  }

  /** Lists the history of all pipelines. */
  @GetMapping
  public PagingResponse<PipelineProcess> history(Pageable pageable) {
    return historyTrackingService.history(pageable);
  }

  /** Lists the history of a dataset. */
  @GetMapping("{datasetKey}")
  public PagingResponse<PipelineProcess> history(
      @PathVariable("datasetKey") UUID datasetKey, Pageable pageable) {
    return historyTrackingService.history(datasetKey, pageable);
  }

  /** Gets the data of a {@link PipelineProcess}. */
  @GetMapping("{datasetKey}/{attempt}")
  public PipelineProcess getPipelineProcess(
      @PathVariable("datasetKey") UUID datasetKey, @PathVariable("attempt") int attempt) {
    return historyTrackingService.get(datasetKey, attempt);
  }

  @PostMapping(value = "process", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public long createPipelineProcess(
      @RequestBody @NotNull PipelineProcessParameters params, Authentication authentication) {
    return historyTrackingService.createOrGet(
        params.getDatasetKey(), params.getAttempt(), authentication.getName());
  }

  /** Adds a new pipeline execution. */
  @PostMapping(value = "process/{processKey}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public long addPipelineExecution(
      @PathVariable("processKey") long processKey,
      @RequestBody @NotNull PipelineExecution pipelineExecution,
      Authentication authentication) {
    return historyTrackingService.addPipelineExecution(
        processKey, pipelineExecution, authentication.getName());
  }

  /** Adds a new pipeline step. */
  @PostMapping(
      value = "process/{processKey}/{executionKey}",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public long addPipelineStep(
      @PathVariable("processKey") long processKey,
      @PathVariable("executionKey") long executionKey,
      @RequestBody @NotNull PipelineStep pipelineStep,
      Authentication authentication) {
    return historyTrackingService.addPipelineStep(
        processKey, executionKey, pipelineStep, authentication.getName());
  }

  @GetMapping("process/{processKey}/{executionKey}/{stepKey}")
  public PipelineStep getPipelineStep(
      @PathVariable("processKey") long processKey,
      @PathVariable("executionKey") long executionKey,
      @PathVariable("stepKey") long stepKey) {
    return historyTrackingService.getPipelineStep(stepKey);
  }

  /** Updates the step status. */
  @PutMapping(
      value = "process/{processKey}/{executionKey}/{stepKey}",
      consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE})
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public void updatePipelineStepStatusAndMetrics(
      @PathVariable("processKey") long processKey,
      @PathVariable("executionKey") long executionKey,
      @PathVariable("stepKey") long stepKey,
      @RequestBody @NotNull PipelineStepParameters stepParams,
      Authentication authentication) {
    historyTrackingService.updatePipelineStepStatusAndMetrics(
        processKey,
        executionKey,
        stepKey,
        stepParams.getStatus(),
        stepParams.getMetrics(),
        authentication.getName());
  }

  /**
   * Runs the last attempt for all datasets. Parameters 'steps' and 'reason' are required, but they
   * will be validated in PipelinesHistoryResource#checkRunInputParams so here they are specified as
   * optional fields.
   */
  @PostMapping(value = "run", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public ResponseEntity<RunPipelineResponse> runAll(
      @RequestParam(value = "steps", required = false) String steps,
      @RequestParam(value = "reason", required = false) String reason,
      Authentication authentication,
      @RequestBody(required = false) RunAllParams runAllParams) {
    return checkRunInputParams(steps, reason)
        .orElseGet(
            () ->
                toHttpResponse(
                    historyTrackingService.runLastAttempt(
                        parseSteps(steps),
                        reason,
                        authentication.getName(),
                        runAllParams != null
                            ? runAllParams.datasetsToExclude
                            : Collections.emptyList())));
  }

  /**
   * Restart last failed pipelines step for a dataset. Parameters 'steps' and 'reason' are required,
   * but they will be validated in PipelinesHistoryResource#checkRunInputParams so here they are
   * specified as optional fields.
   */
  @PostMapping(value = "run/{datasetKey}")
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public ResponseEntity<RunPipelineResponse> runPipelineAttempt(
      @PathVariable("datasetKey") UUID datasetKey,
      @RequestParam(value = "steps", required = false) String steps,
      @RequestParam(value = "reason", required = false) String reason,
      Authentication authentication) {
    return checkRunInputParams(steps, reason)
        .orElseGet(
            () ->
                toHttpResponse(
                    historyTrackingService.runLastAttempt(
                        datasetKey, parseSteps(steps), reason, authentication.getName(), null)));
  }

  /**
   * Re-run a pipeline step. Parameters 'steps' and 'reason' are required, but they will be
   * validated in PipelinesHistoryResource#checkRunInputParams so here they are specified as
   * optional fields.
   */
  @PostMapping(value = "run/{datasetKey}/{attempt}")
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public ResponseEntity<RunPipelineResponse> runPipelineAttempt(
      @PathVariable("datasetKey") UUID datasetKey,
      @PathVariable("attempt") int attempt,
      @RequestParam(value = "steps", required = false) String steps,
      @RequestParam(value = "reason", required = false) String reason,
      Authentication authentication) {
    return checkRunInputParams(steps, reason)
        .orElseGet(
            () ->
                toHttpResponse(
                    historyTrackingService.runPipelineAttempt(
                        datasetKey,
                        attempt,
                        parseSteps(steps),
                        reason,
                        authentication.getName(),
                        null)));
  }

  /** Transforms a {@link RunPipelineResponse} into a {@link ResponseEntity}. */
  private static ResponseEntity<RunPipelineResponse> toHttpResponse(
      RunPipelineResponse runPipelineResponse) {
    if (runPipelineResponse.getResponseStatus()
        == RunPipelineResponse.ResponseStatus.PIPELINE_IN_SUBMITTED) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(runPipelineResponse);
    } else if (runPipelineResponse.getResponseStatus()
        == RunPipelineResponse.ResponseStatus.UNSUPPORTED_STEP) {
      return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(runPipelineResponse);
    } else if (runPipelineResponse.getResponseStatus()
        == RunPipelineResponse.ResponseStatus.ERROR) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(runPipelineResponse);
    }

    return ResponseEntity.ok().body(runPipelineResponse);
  }

  private Optional<ResponseEntity<RunPipelineResponse>> checkRunInputParams(
      String steps, String reason) {
    if (StringUtils.isAnyEmpty(steps, reason)) {
      return Optional.of(
          ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body(
                  RunPipelineResponse.builder()
                      .setMessage("Steps and reason parameters are required")
                      .build()));
    }

    return Optional.empty();
  }

  /** Parse steps argument. */
  private Set<StepType> parseSteps(String steps) {
    Preconditions.checkArgument(StringUtils.isNotEmpty(steps));
    return Arrays.stream(steps.split(","))
        .map(s -> StepType.valueOf(s.toUpperCase()))
        .collect(Collectors.toSet());
  }

  /** Encapsulates the params to pass in the body for the runAll method. */
  private static class RunAllParams {
    List<UUID> datasetsToExclude = new ArrayList<>();

    // getters and setters needed for jackson

    public List<UUID> getDatasetsToExclude() {
      return datasetsToExclude;
    }

    public void setDatasetsToExclude(List<UUID> datasetsToExclude) {
      this.datasetsToExclude = datasetsToExclude;
    }
  }
}
