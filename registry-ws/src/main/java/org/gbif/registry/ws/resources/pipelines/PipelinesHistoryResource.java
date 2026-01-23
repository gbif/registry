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
package org.gbif.registry.ws.resources.pipelines;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.pipelines.PipelineExecution;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.RunPipelineResponse;
import org.gbif.api.model.pipelines.StepRunner;
import org.gbif.api.model.pipelines.StepType;
import org.gbif.api.model.pipelines.ws.PipelineProcessParameters;
import org.gbif.api.model.pipelines.ws.RunAllParams;
import org.gbif.api.model.pipelines.ws.SearchResult;
import org.gbif.api.service.pipelines.PipelinesHistoryService;
import org.gbif.registry.pipelines.RegistryPipelinesHistoryTrackingService;
import org.gbif.registry.ws.util.DateUtils;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Preconditions;

import io.swagger.v3.oas.annotations.Hidden;

import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.EDITOR_ROLE;

/** Pipelines History service. */
@Hidden // TODO: Document?
@RestController
@RequestMapping(value = "pipelines/history", produces = MediaType.APPLICATION_JSON_VALUE)
public class PipelinesHistoryResource implements PipelinesHistoryService {

  private final RegistryPipelinesHistoryTrackingService historyTrackingService;

  public PipelinesHistoryResource(RegistryPipelinesHistoryTrackingService historyTrackingService) {
    this.historyTrackingService = historyTrackingService;
  }

  /** Lists the history of all pipelines. */
  @GetMapping
  @Override
  public PagingResponse<PipelineProcess> history(Pageable pageable) {
    return historyTrackingService.history(pageable);
  }

  /** Lists the history of a dataset. */
  @GetMapping("{datasetKey}")
  @Override
  public PagingResponse<PipelineProcess> history(
      @PathVariable("datasetKey") UUID datasetKey, Pageable pageable) {
    return historyTrackingService.history(datasetKey, pageable);
  }

  /** Gets the data of a {@link PipelineProcess}. */
  @GetMapping("{datasetKey}/{attempt}")
  @Override
  public PipelineProcess getPipelineProcess(
      @PathVariable("datasetKey") UUID datasetKey, @PathVariable("attempt") int attempt) {
    return historyTrackingService.get(datasetKey, attempt);
  }

  @GetMapping("process/running")
  @Override
  public PagingResponse<PipelineProcess> getRunningPipelineProcess(
      @RequestParam(value = "stepType", required = false) StepType stepType,
      @RequestParam(value = "stepRunner", required = false) StepRunner stepRunner,
      Pageable pageable) {
    return historyTrackingService.getRunningPipelineProcess(stepType, stepRunner, pageable);
  }

  @PostMapping(value = "process", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Secured(ADMIN_ROLE)
  @Override
  public long createPipelineProcess(@RequestBody PipelineProcessParameters params) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return historyTrackingService.createOrGet(
        params.getDatasetKey(), params.getAttempt(), authentication.getName());
  }

  /** Adds a new pipeline execution. */
  @PostMapping(value = "process/{processKey}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Secured(ADMIN_ROLE)
  @Override
  public long addPipelineExecution(
      @PathVariable("processKey") long processKey,
      @RequestBody PipelineExecution pipelineExecution) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return historyTrackingService.addPipelineExecution(
        processKey, pipelineExecution, authentication.getName());
  }

  @GetMapping("execution/running/{datasetKey}")
  @Override
  public Long getRunningExecutionKey(@PathVariable("datasetKey") UUID datasetKey) {
    return historyTrackingService.getRunningExecutionKey(datasetKey);
  }

  @GetMapping("execution/{executionKey}/step")
  @Override
  public List<PipelineStep> getPipelineStepsByExecutionKey(
      @PathVariable("executionKey") long executionKey) {
    return historyTrackingService.getPipelineStepsByExecutionKey(executionKey);
  }

  @PostMapping("execution/finished")
  @Secured(ADMIN_ROLE)
  @Override
  public void markAllPipelineExecutionAsFinished() {
    historyTrackingService.markAllPipelineExecutionAsFinished();
  }

  @PostMapping("execution/{executionKey}/finished")
  @Secured(ADMIN_ROLE)
  @Override
  public void markPipelineExecutionIfFinished(@PathVariable("executionKey") long executionKey) {
    historyTrackingService.markPipelineExecutionIfFinished(executionKey);
  }

  @PostMapping("execution/{executionKey}/abort")
  @Secured(ADMIN_ROLE)
  @Override
  public void markPipelineStatusAsAborted(@PathVariable("executionKey") long executionKey) {
    historyTrackingService.markPipelineStatusAsAborted(executionKey);
  }

  @GetMapping("step/{stepKey}")
  @Override
  public PipelineStep getPipelineStep(@PathVariable("stepKey") long stepKey) {
    return historyTrackingService.getPipelineStep(stepKey);
  }

  /** Update pipeline step */
  @PutMapping(value = "step/{stepKey}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Secured(ADMIN_ROLE)
  public long updatePipelineStep(@PathVariable("stepKey") long stepKey, @RequestBody PipelineStep pipelineStep) {
    Preconditions.checkArgument(pipelineStep.getKey() == stepKey);
    return updatePipelineStep(pipelineStep);
  }

  @Override
  @Validated
  public long updatePipelineStep(@NotNull PipelineStep pipelineStep) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return historyTrackingService.updatePipelineStep(pipelineStep, authentication.getName());
  }

  @PutMapping(
      value = "step/{stepKey}/submittedToQueued",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Secured(ADMIN_ROLE)
  @Override
  public void setSubmittedPipelineStepToQueued(@PathVariable("stepKey") long stepKey) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    historyTrackingService.setSubmittedPipelineStepToQueued(stepKey, authentication.getName());
  }

  /**
   * Runs the last attempt for all datasets. Parameters 'steps' and 'reason' are required, but they
   * will be validated in PipelinesHistoryResource#checkRunInputParams so here they are specified as
   * optional fields.
   */
  @PostMapping(value = "run", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Secured(ADMIN_ROLE)
  @Override
  public RunPipelineResponse runAll(
      @RequestParam("steps") String steps,
      @RequestParam("reason") String reason,
      @RequestParam(value = "useLastSuccessful", defaultValue = "false") boolean useLastSuccessful,
      @RequestParam(value = "markPreviousAttemptAsFailed", defaultValue = "false")
          boolean markPreviousAttemptAsFailed,
      @RequestBody(required = false) RunAllParams runAllParams,
      @RequestParam("interpretTypes") Set<String> interpretTypes,
      @RequestParam(value = "excludeEventSteps", required = false, defaultValue = "false")
          boolean excludeEventSteps) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    return historyTrackingService.runLastAttempt(
        parseSteps(steps),
        reason,
        authentication.getName(),
        runAllParams != null ? runAllParams.getDatasetsToExclude() : Collections.emptyList(),
        runAllParams != null ? runAllParams.getDatasetsToInclude() : Collections.emptyList(),
        useLastSuccessful,
        markPreviousAttemptAsFailed,
        interpretTypes,
        excludeEventSteps);
  }

  /**
   * Restart last failed pipelines step for a dataset. Parameters 'steps' and 'reason' are required,
   * but they will be validated in PipelinesHistoryResource#checkRunInputParams so here they are
   * specified as optional fields.
   */
  @PostMapping("run/{datasetKey}")
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public RunPipelineResponse runPipelineAttempt(
      @PathVariable("datasetKey") UUID datasetKey,
      @RequestParam("steps") String steps,
      @RequestParam("reason") String reason,
      @RequestParam(value = "useLastSuccessful", defaultValue = "false") boolean useLastSuccessful,
      @RequestParam(value = "markPreviousAttemptAsFailed", defaultValue = "false")
          boolean markPreviousAttemptAsFailed,
      @RequestParam("interpretTypes") Set<String> interpretTypes,
      @RequestParam(value = "excludeEventSteps", required = false, defaultValue = "false")
          boolean excludeEventSteps) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return historyTrackingService.runLastAttempt(
        datasetKey,
        parseSteps(steps),
        reason,
        authentication.getName(),
        null,
        useLastSuccessful,
        markPreviousAttemptAsFailed,
        interpretTypes,
        excludeEventSteps);
  }

  /**
   * Re-run a pipeline step. Parameters 'steps' and 'reason' are required, but they will be
   * validated in PipelinesHistoryResource#checkRunInputParams so here they are specified as
   * optional fields.
   */
  @PostMapping("run/{datasetKey}/{attempt}")
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public RunPipelineResponse runPipelineAttempt(
      @PathVariable("datasetKey") UUID datasetKey,
      @PathVariable("attempt") int attempt,
      @RequestParam("steps") String steps,
      @RequestParam("reason") String reason,
      @RequestParam(value = "markPreviousAttemptAsFailed", defaultValue = "false")
          boolean markPreviousAttemptAsFailed,
      @RequestParam("interpretTypes") Set<String> interpretTypes,
      @RequestParam(value = "excludeEventSteps", required = false, defaultValue = "false")
          boolean excludeEventSteps) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return historyTrackingService.runPipelineAttempt(
        datasetKey,
        attempt,
        parseSteps(steps),
        reason,
        authentication.getName(),
        null,
        markPreviousAttemptAsFailed,
        interpretTypes,
        excludeEventSteps);
  }

  @GetMapping("search")
  public PagingResponse<SearchResult> search(
      @Nullable @RequestParam(value = "datasetKey", required = false) UUID datasetKey,
      @Nullable @RequestParam(value = "state", required = false) PipelineStep.Status state,
      @Nullable @RequestParam(value = "stepType", required = false) StepType stepType,
      @Nullable @RequestParam(value = "startedMin", required = false) String startedMinAsString,
      @Nullable @RequestParam(value = "startedMax", required = false) String startedMaxAsString,
      @Nullable @RequestParam(value = "finishedMin", required = false) String finishedMinAsString,
      @Nullable @RequestParam(value = "finishedMax", required = false) String finishedMaxAsString,
      @Nullable @RequestParam(value = "rerunReason", required = false) String rerunReason,
      @Nullable @RequestParam(value = "pipelinesVersion", required = false) String pipelinesVersion,
      Pageable page) {

    OffsetDateTime startedMin = DateUtils.LOWER_BOUND_RANGE_PARSER_OFFSET.apply(startedMinAsString);
    OffsetDateTime startedMax = DateUtils.UPPER_BOUND_RANGE_PARSER_OFFSET.apply(startedMaxAsString);
    OffsetDateTime finishedMin = DateUtils.LOWER_BOUND_RANGE_PARSER_OFFSET.apply(finishedMinAsString);
    OffsetDateTime finishedMax = DateUtils.UPPER_BOUND_RANGE_PARSER_OFFSET.apply(finishedMaxAsString);

    return historyTrackingService.search(
        datasetKey,
        state,
        stepType,
        startedMin,
        startedMax,
        finishedMin,
        finishedMax,
        rerunReason,
        pipelinesVersion,
        page);
  }

  @PostMapping(
      value = "identifier/{datasetKey}/{attempt}/email",
      consumes = MediaType.TEXT_PLAIN_VALUE)
  @Secured(ADMIN_ROLE)
  @Deprecated
  @Override
  public void sendAbsentIndentifiersEmail(
      @PathVariable("datasetKey") UUID datasetKey,
      @PathVariable("attempt") int attempt,
      @RequestBody String message) {
    historyTrackingService.sendAbsentIndentifiersEmail(datasetKey, attempt, message);
  }

  @PostMapping("identifier/{datasetKey}/{attempt}/allow")
  @Secured(ADMIN_ROLE)
  @Override
  public void allowAbsentIndentifiers(
      @PathVariable("datasetKey") UUID datasetKey, @PathVariable("attempt") int attempt) {
    historyTrackingService.allowAbsentIndentifiers(datasetKey, attempt);
  }

  @PostMapping("identifier/{datasetKey}/allow")
  @Secured(ADMIN_ROLE)
  @Override
  public void allowAbsentIndentifiers(@PathVariable("datasetKey") UUID datasetKey) {
    historyTrackingService.allowAbsentIndentifiers(datasetKey);
  }

  @PostMapping(
      value = "identifier/{datasetKey}/{attempt}/{executionKey}/notify",
      consumes = MediaType.TEXT_PLAIN_VALUE)
  @Secured(ADMIN_ROLE)
  @Override
  public void notifyAbsentIdentifiers(
      @PathVariable("datasetKey") UUID datasetKey,
      @PathVariable("attempt") int attempt,
      @PathVariable("executionKey") long executionKey,
      @RequestBody String message) {
    historyTrackingService.notifyAbsentIdentifiers(datasetKey, attempt, executionKey, message);
  }

  @ExceptionHandler({
    ConstraintViolationException.class,
    MissingServletRequestParameterException.class
  })
  private ResponseEntity<RunPipelineResponse> validationExceptionMapper() {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            RunPipelineResponse.builder()
                .setMessage("Steps and reason parameters are required")
                .build());
  }

  /** Parse steps argument. */
  private Set<StepType> parseSteps(String steps) {
    return Arrays.stream(steps.split(","))
        .map(s -> StepType.valueOf(s.toUpperCase()))
        .collect(Collectors.toSet());
  }
}
