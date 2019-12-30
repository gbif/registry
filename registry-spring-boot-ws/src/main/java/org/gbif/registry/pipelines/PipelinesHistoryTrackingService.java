package org.gbif.registry.pipelines;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.pipelines.PipelineExecution;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.StepType;
import org.gbif.registry.domain.pipelines.RunPipelineResponse;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service to provide the history and re-execute previous attempts of Pipelines.
 */
public interface PipelinesHistoryTrackingService {

  /**
   * Executes the last crawl/pipeline attempt executed on a dataset.
   *
   * @param datasetKey dataset identifier
   * @param steps      steps to be executed
   * @param reason     textual justification of why it has to be re-executed
   * @param user       the user who is running the attempt
   * @param prefix     if triggered for all datasets
   * @return a response containing the request result
   */
  RunPipelineResponse runLastAttempt(
    UUID datasetKey, Set<StepType> steps, String reason, String user, String prefix);

  /**
   * Executes a previously run attempt.
   *
   * @param datasetKey dataset identifier
   * @param attempt    crawl attempt identifier
   * @param steps      steps to be executed
   * @param reason     textual justification of why it has to be re-executed
   * @param user       the user who is running the attempt
   * @param prefix     if triggered for all datasets
   * @return the response of the execution request
   */
  RunPipelineResponse runPipelineAttempt(
    UUID datasetKey, int attempt, Set<StepType> steps, String reason, String user, String prefix);

  /**
   * Executes the last crawl attempt for all datasets.
   *
   * @param steps             steps to be executed
   * @param reason            textual justification of why it has to be re-executed
   * @param user              the user who is running the attempt
   * @param datasetsToExclude excluded dataset keys
   * @return the response of the execution request
   */
  RunPipelineResponse runLastAttempt(
    Set<StepType> steps, String reason, String user, List<UUID> datasetsToExclude);

  /**
   * Lists the history of all {@link PipelineProcess}, sorted descending from the most recent one.
   *
   * @param pageable paging request
   * @return a paged response that contains a list of {@link PipelineProcess}
   */
  PagingResponse<PipelineProcess> history(Pageable pageable);

  /**
   * Lists the history of all {@link PipelineProcess} of a dataset, sorted descending from the most
   * recent one.
   *
   * @param datasetKey dataset identifier
   * @param pageable   paging request
   * @return a paged response that contains a list of {@link PipelineProcess}
   */
  PagingResponse<PipelineProcess> history(UUID datasetKey, Pageable pageable);

  /**
   * Gets the PipelineProcess identified by the the dataset and attempt identifiers.
   *
   * @param datasetKey dataset identifier
   * @param attempt    crawl attempt identifier
   * @return a instance of pipelines process if exists
   */
  PipelineProcess get(UUID datasetKey, int attempt);

  /**
   * Creates/persists a pipelines process of dataset for an attempt identifier. If the process
   * already exists it returns the existing one.
   *
   * @param datasetKey dataset identifier
   * @param attempt    attempt identifier
   * @param creator    user or process that created the pipeline
   * @return the key of the {@link PipelineProcess} created
   */
  long createOrGet(UUID datasetKey, int attempt, String creator);

  /**
   * Adds/persists the information of a pipeline execution.
   *
   * @param pipelineProcessKey sequential identifier of a pipeline process
   * @param pipelineExecution  pipeline execution data
   * @param creator            the user who is adding the step
   * @return the key of the PipelineExecution created
   */
  long addPipelineExecution(long pipelineProcessKey, PipelineExecution pipelineExecution, String creator);

  /**
   * Adds/persists the information of a pipeline step.
   *
   * @param pipelineProcessKey sequential identifier of a pipeline process
   * @param executionKey       key of the pipeline execution
   * @param pipelineStep       step to be added
   * @param creator            the user who is adding the step
   * @return the key of the PipelineStep created
   */
  long addPipelineStep(long pipelineProcessKey, long executionKey, PipelineStep pipelineStep, String creator);

  /**
   * Gets the PipelineStep of the specified key.
   *
   * @param key key of the pipeline step
   * @return {@link PipelineStep}
   */
  PipelineStep getPipelineStep(long key);

  /**
   * Updates the status of a pipeline step and retrieves the metrics from ES and inserts them in the
   * DB.
   *
   * @param processKey      key of the process of the step
   * @param executionKey    key of the execution
   * @param pipelineStepKey sequential identifier of a pipeline process step
   * @param status          new status for the pipeline step
   * @param metrics         metrics from pipelines
   * @param user            the user who is updating the status
   */
  void updatePipelineStepStatusAndMetrics(
    long processKey,
    long executionKey,
    long pipelineStepKey,
    PipelineStep.Status status,
    List<PipelineStep.MetricInfo> metrics,
    String user);
}
