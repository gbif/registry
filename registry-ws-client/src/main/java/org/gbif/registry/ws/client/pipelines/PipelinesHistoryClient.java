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
package org.gbif.registry.ws.client.pipelines;
import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
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
import org.gbif.api.service.pipelines.PipelinesHistoryService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface PipelinesHistoryClient extends PipelinesHistoryService {

  @RequestLine("GET /pipelines/history")
  @Headers("Accept: application/json")
  PagingResponse<PipelineProcess> history(@QueryMap Pageable pageable);

  @RequestLine("GET /pipelines/history/{datasetKey}")
  @Headers("Accept: application/json")
  PagingResponse<PipelineProcess> history(@Param("datasetKey") UUID datasetKey, @QueryMap Pageable pageable);

  @RequestLine("GET /pipelines/history/{datasetKey}/{attempt}")
  @Headers("Accept: application/json")
  PipelineProcess getPipelineProcess(@Param("datasetKey") UUID datasetKey, @Param("attempt") int attempt);

  @RequestLine("POST /pipelines/history/process")
  @Headers("Content-Type: application/json")
  long createPipelineProcess(PipelineProcessParameters params);

  @RequestLine("POST /pipelines/history/process/{processKey}")
  @Headers("Content-Type: application/json")
  long addPipelineExecution(@Param("processKey") long processKey, PipelineExecution pipelineExecution);

  @RequestLine("GET /pipelines/history/execution/running/{datasetKey}")
  Long getRunningExecutionKey(@Param("datasetKey") UUID datasetKey);

  @RequestLine("GET /pipelines/history/execution/{executionKey}/step")
  List<PipelineStep> getPipelineStepsByExecutionKey(@Param("executionKey") long executionKey);

  @RequestLine("GET /pipelines/history/process/running?stepType={stepType}&stepRunner={stepRunner}")
  @Headers("Accept: application/json")
  PagingResponse<PipelineProcess> getRunningPipelineProcess(
    @Param("stepType") StepType stepType,
    @Param("stepRunner") StepRunner stepRunner,
    @QueryMap Pageable pageable
  );

  @RequestLine("POST /pipelines/history/execution/finished")
  void markAllPipelineExecutionAsFinished();

  @RequestLine("POST /pipelines/history/execution/{executionKey}/finished")
  void markPipelineExecutionIfFinished(@Param("executionKey") long executionKey);

  @RequestLine("POST /pipelines/history/execution/{executionKey}/abort")
  void markPipelineStatusAsAborted(@Param("executionKey") long executionKey);

  @RequestLine("PUT /pipelines/history/step/{stepKey}")
  @Headers("Content-Type: application/json")
  long updatePipelineStep(@Param("stepKey") long stepKey, PipelineStep pipelineStep);

  @RequestLine("GET /pipelines/history/step/{stepKey}")
  @Headers("Accept: application/json")
  PipelineStep getPipelineStep(@Param("stepKey") long stepKey);

  @RequestLine("POST /pipelines/history/run?steps={steps}&reason={reason}&useLastSuccessful={useLastSuccessful}&markPreviousAttemptAsFailed={markPreviousAttemptAsFailed}&interpretTypes={interpretTypes}")
  @Headers("Content-Type: application/json")
  RunPipelineResponse runAll(
    @Param("steps") String steps,
    @Param("reason") String reason,
    @Param("useLastSuccessful") boolean useLastSuccessful,
    @Param("markPreviousAttemptAsFailed") boolean markPreviousAttemptAsFailed,
    RunAllParams runAllParams,
    @Param("interpretTypes") Set<String> interpretTypes
  );

  @RequestLine("POST /pipelines/history/run/{datasetKey}?steps={steps}&reason={reason}&useLastSuccessful={useLastSuccessful}&markPreviousAttemptAsFailed={markPreviousAttemptAsFailed}&interpretTypes={interpretTypes}")
  RunPipelineResponse runPipelineAttempt(
    @Param("datasetKey") UUID datasetKey,
    @Param("steps") String steps,
    @Param("reason") String reason,
    @Param("useLastSuccessful") boolean useLastSuccessful,
    @Param("markPreviousAttemptAsFailed") boolean markPreviousAttemptAsFailed,
    @Param("interpretTypes") Set<String> interpretTypes
  );

  @RequestLine("POST /pipelines/history/run/{datasetKey}/{attempt}?steps={steps}&reason={reason}&markPreviousAttemptAsFailed={markPreviousAttemptAsFailed}&interpretTypes={interpretTypes}")
  RunPipelineResponse runPipelineAttempt(
    @Param("datasetKey") UUID datasetKey,
    @Param("attempt") int attempt,
    @Param("steps") String steps,
    @Param("reason") String reason,
    @Param("markPreviousAttemptAsFailed") boolean markPreviousAttemptAsFailed,
    @Param("interpretTypes") Set<String> interpretTypes
  );

  @RequestLine("POST /pipelines/history/identifier/{datasetKey}/{attempt}/email")
  @Headers("Content-Type: text/plain")
  @Deprecated
  void sendAbsentIdentifiersEmail(@Param("datasetKey") UUID datasetKey, @Param("attempt") int attempt, String message);

  @RequestLine("POST /pipelines/history/identifier/{datasetKey}/{attempt}/allow")
  void allowAbsentIdentifiers(@Param("datasetKey") UUID datasetKey, @Param("attempt") int attempt);

  @RequestLine("POST /pipelines/history/identifier/{datasetKey}/allow")
  void allowAbsentIdentifiers(@Param("datasetKey") UUID datasetKey);

  @RequestLine("POST /pipelines/history/identifier/{datasetKey}/{attempt}/{executionKey}/notify")
  @Headers("Content-Type: text/plain")
  void notifyAbsentIdentifiers(@Param("datasetKey") UUID datasetKey, @Param("attempt") int attempt, @Param("executionKey") long executionKey, String message);

  @RequestLine("PUT /pipelines/history/step/{stepKey}/submittedToQueued")
  @Headers("Content-Type: application/json")
  void setSubmittedPipelineStepToQueued(@Param("stepKey") long stepKey);
}
