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
package org.gbif.registry.persistence.mapper.pipelines;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.pipelines.PipelineExecution;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.StepType;
import org.gbif.api.model.pipelines.ws.SearchResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/** Mapper for {@link PipelineProcess} entities. */
@Repository
public interface PipelineProcessMapper {

  /**
   * Inserts a new {@link PipelineProcess} if it doesn't exist. If it exists it handles the conflict
   * and doesn't break.
   *
   * <p>The id generated is set to the {@link PipelineProcess} received as parameter.
   *
   * @param process to insert
   */
  void createIfNotExists(PipelineProcess process);

  /**
   * Retrieves a {@link PipelineProcess} by dataset key and attempt.
   *
   * @param datasetKey registy dataset identifier
   * @param attempt numerical attempt
   * @return {@link PipelineProcess}
   */
  PipelineProcess getByDatasetAndAttempt(
      @Param("datasetKey") UUID datasetKey, @Param("attempt") int attempt);

  /**
   * Retrieves a {@link PipelineProcess} by key.
   *
   * @param key key of the process
   * @return {@link PipelineProcess}
   */
  PipelineProcess get(@Param("key") long key);

  List<PipelineProcess> getRunningPipelineProcess(@Param("page") Pageable page);

  long getRunningPipelineProcessCount();

  Optional<Integer> getLastAttempt(@Param("datasetKey") UUID datasetKey);

  Optional<Integer> getLastSuccessfulAttempt(
      @Param("datasetKey") UUID datasetKey, @Param("stepType") StepType stepType);

  Long getRunningExecutionKey(@Param("datasetKey") UUID datasetKey);

  /**
   * Adds a {@link PipelineExecution} to an existing {@link PipelineProcess}.
   *
   * @param pipelinesProcessKey key of the process where we want to add the step
   * @param execution execution to add
   */
  void addPipelineExecution(
      @Param("pipelinesProcessKey") long pipelinesProcessKey,
      @Param("execution") PipelineExecution execution);

  /**
   * Retrieves a {@link PipelineExecution} by key.
   *
   * @param pipelineExecutionKey key of the pipeline execution
   * @return {@link PipelineExecution}
   */
  PipelineExecution getPipelineExecution(@Param("key") long pipelineExecutionKey);

  /**
   * Adds a {@link PipelineStep} to an existing {@link PipelineExecution}.
   *
   * @param pipelineExecutionKey key of the process where we want to add the step
   * @param step step to add
   */
  void addPipelineStep(
    @Param("pipelineExecutionKey") long pipelineExecutionKey, @Param("step") PipelineStep step);

  /**
   * Adds a {@link PipelineStep} to an existing {@link PipelineExecution}.
   *
   * @param step step to add
   */
  void updatePipelineStep(@Param("step") PipelineStep step);

  /**
   * Lists {@link PipelineProcess} based in the search parameters.
   *
   * <p>It supports paging.
   *
   * @param datasetKey dataset key
   * @param attempt attempt
   * @param page page to specify the offset and the limit
   * @return list of {@link PipelineProcess}
   */
  List<PipelineProcess> list(
      @Nullable @Param("datasetKey") UUID datasetKey,
      @Nullable @Param("attempt") Integer attempt,
      @Nullable @Param("page") Pageable page);

  /** Counts the number of {@link PipelineProcess} based in the search parameters. */
  long count(
      @Nullable @Param("datasetKey") UUID datasetKey, @Nullable @Param("attempt") Integer attempt);

  List<PipelineStep> getPipelineStepsByExecutionKey(@Param("key") long pipelineExecutionKey);

  PipelineStep getPipelineStep(@Param("key") long key);

  List<PipelineProcess> getPipelineProcessesByDatasetAndAttempts(
      @Nullable @Param("datasetKey") UUID datasetKey,
      @Nullable @Param("attempts") List<Integer> attempts);

  List<SearchResult> search(
      @Nullable @Param("datasetKey") UUID datasetKey,
      @Nullable @Param("state") PipelineStep.Status state,
      @Nullable @Param("stepType") StepType stepType,
      @Nullable @Param("startedMin") LocalDateTime startedMin,
      @Nullable @Param("startedMax") LocalDateTime startedMax,
      @Nullable @Param("finishedMin") LocalDateTime finishedMin,
      @Nullable @Param("finishedMax") LocalDateTime finishedMax,
      @Nullable @Param("rerunReason") String rerunReason,
      @Nullable @Param("pipelinesVersion") String pipelinesVersion,
      @Nullable @Param("page") Pageable page);

  long searchCount(
      @Nullable @Param("datasetKey") UUID datasetKey,
      @Nullable @Param("state") PipelineStep.Status state,
      @Nullable @Param("stepType") StepType stepType,
      @Nullable @Param("startedMin") LocalDateTime startedMin,
      @Nullable @Param("startedMax") LocalDateTime startedMax,
      @Nullable @Param("finishedMin") LocalDateTime finishedMin,
      @Nullable @Param("finishedMax") LocalDateTime finishedMax,
      @Nullable @Param("rerunReason") String rerunReason,
      @Nullable @Param("pipelinesVersion") String pipelinesVersion);

  /**
   * Mark all existing {@link PipelineExecution} as finished
   */
  void markAllPipelineExecutionAsFinished();

  /**
   * Abourt an existing {@link PipelineStep}.
   *
   * @param pipelineExecutionKey key of the process
   */
  void markPipelineStatusAsAborted(@Param("pipelineExecutionKey") long pipelineExecutionKey);

  /**
   * Mark an existing {@link PipelineExecution} as finished when all pipelin steps are finished
   *
   * @param pipelineExecutionKey key of the process
   */
  void markPipelineExecutionIfFinished(@Param("pipelineExecutionKey") long pipelineExecutionKey);
}
