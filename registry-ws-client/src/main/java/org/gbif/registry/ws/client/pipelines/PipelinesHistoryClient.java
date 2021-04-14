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
package org.gbif.registry.ws.client.pipelines;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.pipelines.PipelineExecution;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.RunPipelineResponse;
import org.gbif.api.model.pipelines.ws.PipelineProcessParameters;
import org.gbif.api.model.pipelines.ws.PipelineStepParameters;
import org.gbif.api.model.pipelines.ws.RunAllParams;
import org.gbif.api.service.pipelines.PipelinesHistoryService;

import java.util.Set;
import java.util.UUID;

import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping("pipelines/history")
public interface PipelinesHistoryClient extends PipelinesHistoryService {

  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<PipelineProcess> history(@SpringQueryMap Pageable pageable);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{datasetKey}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<PipelineProcess> history(
      @PathVariable("datasetKey") UUID datasetKey, @SpringQueryMap Pageable pageable);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{datasetKey}/{attempt}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PipelineProcess getPipelineProcess(
      @PathVariable("datasetKey") UUID datasetKey, @PathVariable("attempt") int attempt);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "process",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  long createPipelineProcess(@RequestBody PipelineProcessParameters params);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "process/{processKey}",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  long addPipelineExecution(
      @PathVariable("processKey") long processKey,
      @RequestBody PipelineExecution pipelineExecution);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "process/{processKey}/{executionKey}",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  long addPipelineStep(
      @PathVariable("processKey") long processKey,
      @PathVariable("executionKey") long executionKey,
      @RequestBody PipelineStep pipelineStep);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "process/{processKey}/{executionKey}/{stepKey}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PipelineStep getPipelineStep(
      @PathVariable("processKey") long processKey,
      @PathVariable("executionKey") long executionKey,
      @PathVariable("stepKey") long stepKey);

  @RequestMapping(
      method = RequestMethod.PUT,
      value = "process/{processKey}/{executionKey}/{stepKey}",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  void updatePipelineStepStatusAndMetrics(
      @PathVariable("processKey") long processKey,
      @PathVariable("executionKey") long executionKey,
      @PathVariable("stepKey") long stepKey,
      @RequestBody PipelineStepParameters stepParams);

  @RequestMapping(method = RequestMethod.POST, value = "run")
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

  @RequestMapping(method = RequestMethod.POST, value = "run/{datasetKey}")
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

  @RequestMapping(method = RequestMethod.POST, value = "run/{datasetKey}/{attempt}")
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
}
