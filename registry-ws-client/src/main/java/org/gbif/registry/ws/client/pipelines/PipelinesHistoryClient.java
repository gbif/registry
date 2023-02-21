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

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.pipelines.PipelineExecution;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.RunPipelineResponse;
import org.gbif.api.model.pipelines.ws.PipelineProcessParameters;
import org.gbif.api.model.pipelines.ws.RunAllParams;
import org.gbif.api.service.pipelines.PipelinesHistoryService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping("pipelines/history")
public interface PipelinesHistoryClient extends PipelinesHistoryService {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<PipelineProcess> history(@SpringQueryMap Pageable pageable);

  @GetMapping(
      value = "{datasetKey}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<PipelineProcess> history(
      @PathVariable("datasetKey") UUID datasetKey, @SpringQueryMap Pageable pageable);

  @GetMapping(
      value = "{datasetKey}/{attempt}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PipelineProcess getPipelineProcess(
      @PathVariable("datasetKey") UUID datasetKey, @PathVariable("attempt") int attempt);

  @PostMapping(
      value = "process",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  long createPipelineProcess(@RequestBody PipelineProcessParameters params);

  @PostMapping(
      value = "process/{processKey}",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  long addPipelineExecution(
      @PathVariable("processKey") long processKey,
      @RequestBody PipelineExecution pipelineExecution);

  @GetMapping("execution/running/{datasetKey}")
  @Override
  Long getRunningExecutionKey(@PathVariable("datasetKey") UUID datasetKey);

  @GetMapping("execution/{executionKey}/step")
  @Override
  List<PipelineStep> getPipelineStepsByExecutionKey(
    @PathVariable("executionKey") long executionKey);

  @GetMapping("process/running")
  @Override
  PagingResponse<PipelineProcess> getRunningPipelineProcess(Pageable pageable);

  @PostMapping("execution/finished")
  @Override
  void markAllPipelineExecutionAsFinished();

  @PostMapping("execution/{executionKey}/finished")
  @Override
  void markPipelineExecutionIfFinished(@PathVariable("executionKey") long executionKey);

  @PostMapping("execution/{executionKey}/abort")
  @Override
  void markPipelineStatusAsAborted(@PathVariable("executionKey") long executionKey);

  @PostMapping(
      value = "step",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  long updatePipelineStep(
      @RequestBody PipelineStep pipelineStep);

  @GetMapping(
      value = "step/{stepKey}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PipelineStep getPipelineStep(
      @PathVariable("stepKey") long stepKey);

  @PostMapping(value = "run")
  @ResponseBody
  @Override
  RunPipelineResponse runAll(
      @RequestParam(value = "steps", required = false) String steps,
      @RequestParam(value = "reason", required = false) String reason,
      @RequestParam(value = "useLastSuccessful", defaultValue = "false") boolean useLastSuccessful,
      @RequestParam(value = "markPreviousAttemptAsFailed", defaultValue = "false")
          boolean markPreviousAttemptAsFailed,
      @RequestBody(required = false) RunAllParams runAllParams,
      @RequestParam(value = "interpretTypes", defaultValue = "false") Set<String> interpretTypes);

  @PostMapping(value = "run/{datasetKey}")
  @ResponseBody
  @Override
  RunPipelineResponse runPipelineAttempt(
      @PathVariable("datasetKey") UUID datasetKey,
      @RequestParam(value = "steps", required = false) String steps,
      @RequestParam(value = "reason", required = false) String reason,
      @RequestParam(value = "useLastSuccessful", defaultValue = "false") boolean useLastSuccessful,
      @RequestParam(value = "markPreviousAttemptAsFailed", defaultValue = "false")
          boolean markPreviousAttemptAsFailed,
      @RequestParam(value = "interpretTypes", defaultValue = "false") Set<String> interpretTypes);

  @PostMapping(value = "run/{datasetKey}/{attempt}")
  @ResponseBody
  @Override
  RunPipelineResponse runPipelineAttempt(
      @PathVariable("datasetKey") UUID datasetKey,
      @PathVariable("attempt") int attempt,
      @RequestParam(value = "steps", required = false) String steps,
      @RequestParam(value = "reason", required = false) String reason,
      @RequestParam(value = "markPreviousAttemptAsFailed", defaultValue = "false")
          boolean markPreviousAttemptAsFailed,
      @RequestParam(value = "interpretTypes", defaultValue = "false") Set<String> interpretTypes);

  @PostMapping(
      value = "identifier/{datasetKey}/{attempt}/email",
      consumes = MediaType.TEXT_PLAIN_VALUE)
  @Override
  void sendAbsentIndentifiersEmail(
      @PathVariable("datasetKey") UUID datasetKey,
      @PathVariable("attempt") int attempt,
      @RequestBody String message);

  @PostMapping("identifier/{datasetKey}/{attempt}/allow")
  @Override
  void allowAbsentIndentifiers(
      @PathVariable("datasetKey") UUID datasetKey, @PathVariable("attempt") int attempt);

  @PostMapping("identifier/{datasetKey}/allow")
  @Override
  void allowAbsentIndentifiers(@PathVariable("datasetKey") UUID datasetKey);
}
