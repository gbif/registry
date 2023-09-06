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

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.pipelines.PipelineExecution;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.RunPipelineResponse;
import org.gbif.api.model.pipelines.StepType;
import org.gbif.api.model.pipelines.ws.SearchResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/** Service to provide the history and re-execute previous attempts of Pipelines. */
public interface RegistryPipelinesHistoryTrackingService {

  /**
   * Executes the last crawl/pipeline attempt executed on a dataset.
   *
   * @param datasetKey dataset identifier
   * @param steps steps to be executed
   * @param reason textual justification of why it has to be re-executed
   * @param user the user who is running the attempt
   * @param prefix if triggered for all datasets
   * @param useLastSuccessful if true it uses the latest successful attempt. Otherwise, it uses the
   *     latest.
   * @param markPreviousAttemptAsFailed previous status can't be wrong, when CLI restarted during
   *     processing a dataset
   * @param interpretTypes is used for partial interpretation such as only TAXONOMY, METADATA and
   *     etc
   * @return a response containing the request result
   */
  RunPipelineResponse runLastAttempt(
      UUID datasetKey,
      Set<StepType> steps,
      String reason,
      String user,
      String prefix,
      boolean useLastSuccessful,
      boolean markPreviousAttemptAsFailed,
      Set<String> interpretTypes);

  /**
   * Executes the last crawl attempt for all datasets.
   *
   * @param steps steps to be executed
   * @param reason textual justification of why it has to be re-executed
   * @param user the user who is running the attempt
   * @param datasetsToExclude excluded dataset keys
   * @param datasetsToInclude included dataset keys
   * @param useLastSuccessful if true it uses the latest successful attempt. Otherwise, it uses the
   *     latest.
   * @param markPreviousAttemptAsFailed previous status can't be wrong, when CLI restarted during
   *     processing a dataset
   * @param interpretTypes is used for partial interpretation such as only TAXONOMY, METADATA and
   *     etc
   * @return the response of the execution request
   */
  RunPipelineResponse runLastAttempt(
      Set<StepType> steps,
      String reason,
      String user,
      List<UUID> datasetsToExclude,
      List<UUID> datasetsToInclude,
      boolean useLastSuccessful,
      boolean markPreviousAttemptAsFailed,
      Set<String> interpretTypes);

  /**
   * Executes a previously run attempt.
   *
   * @param datasetKey dataset identifier
   * @param attempt crawl attempt identifier
   * @param steps steps to be executed
   * @param reason textual justification of why it has to be re-executed
   * @param user the user who is running the attempt
   * @param prefix if triggered for all datasets
   * @param markPreviousAttemptAsFailed previous status can't be wrong, when CLI restarted during
   *     processing a dataset
   * @param interpretTypes is used for partial interpretation such as only TAXONOMY, METADATA and
   *     etc.
   * @return the response of the execution request
   */
  RunPipelineResponse runPipelineAttempt(
      UUID datasetKey,
      int attempt,
      Set<StepType> steps,
      String reason,
      String user,
      String prefix,
      boolean markPreviousAttemptAsFailed,
      Set<String> interpretTypes);

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
   * @param pageable paging request
   * @return a paged response that contains a list of {@link PipelineProcess}
   */
  PagingResponse<PipelineProcess> history(UUID datasetKey, Pageable pageable);

  /**
   * Gets the PipelineProcess identified by the dataset and attempt identifiers.
   *
   * @param datasetKey dataset identifier
   * @param attempt crawl attempt identifier
   * @return an instance of pipelines process if exists
   */
  PipelineProcess get(UUID datasetKey, int attempt);

  /**
   * Gets running PipelineProcess
   *
   * @param pageable paging request
   */
  PagingResponse<PipelineProcess> getRunningPipelineProcess(Pageable pageable);

  /**
   * Creates/persists a pipelines process of dataset for an attempt identifier. If the process
   * already exists it returns the existing one.
   *
   * @param datasetKey dataset identifier
   * @param attempt attempt identifier
   * @param creator user or process that created the pipeline
   * @return the key of the {@link PipelineProcess} created
   */
  long createOrGet(UUID datasetKey, int attempt, String creator);

  /**
   * Gets execution key for running dataset
   *
   * @param datasetKey dataset identifier
   * @return running execution key
   */
  Long getRunningExecutionKey(UUID datasetKey);

  /**
   * Adds/persists the information of a pipeline execution.
   *
   * @param pipelineProcessKey sequential identifier of a pipeline process
   * @param pipelineExecution pipeline execution data
   * @param creator the user who is adding the step
   * @return the key of the PipelineExecution created
   */
  long addPipelineExecution(
      long pipelineProcessKey, PipelineExecution pipelineExecution, String creator);

  /**
   * Persists the information of a pipeline step.
   *
   * @param pipelineStep step to be added
   * @param creator the user who is adding the step
   * @return the key of the PipelineStep created
   */
  long updatePipelineStep(PipelineStep pipelineStep, String creator);

  /**
   * Gets the PipelineSteps list of the execution key.
   *
   * @param executionKey key of the execution
   * @return {@link PipelineStep}
   */
  List<PipelineStep> getPipelineStepsByExecutionKey(long executionKey);

  /** Marks all pipeline execution as finished */
  void markAllPipelineExecutionAsFinished();

  /**
   * Marks pipeline execution as finished when all pipeline steps are finished
   *
   * @param executionKey key of the pipeline execution
   */
  void markPipelineExecutionIfFinished(long executionKey);

  /**
   * Changes status to ABORTED and set finished date if state is RUNNING, QUEUED or SUBMITTED
   *
   * @param executionKey key of the pipeline execution
   */
  void markPipelineStatusAsAborted(long executionKey);

  /**
   * Gets the PipelineStep of the specified key.
   *
   * @param key key of the pipeline step
   * @return {@link PipelineStep}
   */
  PipelineStep getPipelineStep(long key);

  PagingResponse<SearchResult> search(
      @Nullable UUID datasetKey,
      @Nullable PipelineStep.Status state,
      @Nullable StepType stepType,
      @Nullable LocalDateTime startedMin,
      @Nullable LocalDateTime startedMax,
      @Nullable LocalDateTime finishedMin,
      @Nullable LocalDateTime finishedMax,
      @Nullable String rerunReason,
      @Nullable String pipelinesVersion,
      @Nullable Pageable page);

  /**
   * Sends email to data administrator about absent identifiers issue with a dataset
   *
   * <p>Deprecated: use {@link #notifyAbsentIdentifiers(UUID, int, long, String)} instead.
   *
   * @param datasetKey dataset key
   * @param attempt attempt to run
   * @param message with failed metrics and etc info
   */
  @Deprecated
  void sendAbsentIndentifiersEmail(@NotNull UUID datasetKey, int attempt, @NotNull String message);

  /**
   * Mark failed identifier stage as finished and continue interpretation process for datasets were
   * identifier stage failed because of a threshold limit
   *
   * @param datasetKey dataset key
   * @param attempt attempt to run
   */
  void allowAbsentIndentifiers(@NotNull UUID datasetKey, int attempt);

  /**
   * Mark latest failed identifier stage as finished and continue interpretation process for
   * datasets were identifier stage failed because of a threshold limit
   *
   * @param datasetKey dataset key
   */
  void allowAbsentIndentifiers(@NotNull UUID datasetKey);

  /**
   * Sends a notification to the data administrators about absent identifiers issues with the
   * dataset.
   *
   * @param datasetKey key of the dataset
   * @param attempt crawling attempt
   * @param executionKey key of the pipelines execution
   * @param message cause of the issue
   */
  void notifyAbsentIdentifiers(UUID datasetKey, int attempt, long executionKey, String message);
}
