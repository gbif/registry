package org.gbif.registry.ws.client.pipelines;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.pipelines.PipelineExecution;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.ws.PipelineProcessParameters;
import org.gbif.api.model.pipelines.ws.PipelineStepParameters;
import org.gbif.registry.ws.client.guice.RegistryWs;
import org.gbif.ws.client.BaseWsClient;

import java.util.UUID;
import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;

/** WS client for the pipelines history tracking service. */
public class PipelinesHistoryWsClient extends BaseWsClient {

  private static final String PROCESS_PATH = "process";
  private static final String RUN_PATH = "run";

  private static final GenericType<PagingResponse<PipelineProcess>> PAGING_PIPELINE_PROCESS =
      new GenericType<PagingResponse<PipelineProcess>>() {};
  private static final GenericType<PipelineProcess> PIPELINE_PROCESS_TYPE =
      new GenericType<PipelineProcess>() {};
  private static final GenericType<PipelineStep> PIPELINE_STEP_TYPE =
      new GenericType<PipelineStep>() {};

  @Inject
  public PipelinesHistoryWsClient(
      @RegistryWs WebResource resource, @Nullable ClientFilter authFilter) {
    super(resource.path("pipelines/history"));
    if (authFilter != null) {
      this.resource.addFilter(authFilter);
    }
  }

  public PagingResponse<PipelineProcess> history(@Nullable Pageable pageable) {
    return get(PAGING_PIPELINE_PROCESS, pageable);
  }

  public PagingResponse<PipelineProcess> history(UUID datasetKey, Pageable pageable) {
    return get(PAGING_PIPELINE_PROCESS, pageable, datasetKey.toString());
  }

  public PipelineProcess getPipelineProcess(UUID datasetKey, int attempt) {
    return get(PIPELINE_PROCESS_TYPE, datasetKey.toString(), String.valueOf(attempt));
  }

  public long createOrGetPipelineProcess(UUID datasetKey, int attempt) {
    return post(Long.class, new PipelineProcessParameters(datasetKey, attempt), PROCESS_PATH);
  }

  public long addPipelineExecution(long processKey, PipelineExecution pipelineExecution) {
    return post(Long.class, pipelineExecution, PROCESS_PATH, String.valueOf(processKey));
  }

  public long addPipelineStep(long processKey, long executionKey, PipelineStep pipelineStep) {
    return post(
        Long.class,
        pipelineStep,
        PROCESS_PATH,
        String.valueOf(processKey),
        String.valueOf(executionKey));
  }

  public PipelineStep getPipelineStep(long processkey, long executionKey, long stepKey) {
    return get(
        PIPELINE_STEP_TYPE,
        PROCESS_PATH,
        String.valueOf(processkey),
        String.valueOf(executionKey),
        String.valueOf(stepKey));
  }

  public void updatePipelineStepStatusAndMetrics(
      long processKey, long executionKey, long stepKey, PipelineStepParameters stepParams) {
    put(
        ClientResponse.class,
        stepParams,
        PROCESS_PATH,
        String.valueOf(processKey),
        String.valueOf(executionKey),
        String.valueOf(stepKey));
  }

  public ClientResponse runAll(String steps, String reason) {
    return getResource(RUN_PATH)
        .queryParam("steps", steps)
        .queryParam("reason", reason)
        .type("application/json")
        .post(ClientResponse.class);
  }

  public ClientResponse runPipelineAttempt(UUID datasetKey, String steps, String reason) {
    return getResource(RUN_PATH, datasetKey.toString())
        .queryParam("steps", steps)
        .queryParam("reason", reason)
        .type("application/json")
        .post(ClientResponse.class);
  }

  public ClientResponse runPipelineAttempt(
      UUID datasetKey, int attempt, String steps, String reason) {
    return getResource(RUN_PATH, datasetKey.toString(), String.valueOf(attempt))
        .queryParam("steps", steps)
        .queryParam("reason", reason)
        .type("application/json")
        .post(ClientResponse.class);
  }
}
