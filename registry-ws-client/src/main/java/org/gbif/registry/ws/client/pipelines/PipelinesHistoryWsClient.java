package org.gbif.registry.ws.client.pipelines;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.PipelineWorkflow;
import org.gbif.api.model.pipelines.ws.PipelineProcessParameters;
import org.gbif.registry.ws.client.guice.RegistryWs;
import org.gbif.ws.client.BaseWsClient;

import java.util.UUID;
import javax.annotation.Nullable;

import com.google.inject.Inject;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;

/** WS client for the pipelines history tracking service. */
public class PipelinesHistoryWsClient extends BaseWsClient {

  private static final String PROCESS_PATH = "process";
  private static final String STEP_PATH = "step";
  private static final String RUN_PATH = "run";

  private static final GenericType<PagingResponse<PipelineProcess>> PAGING_PIPELINE_PROCESS =
      new GenericType<PagingResponse<PipelineProcess>>() {};
  private static final GenericType<PipelineProcess> PIPELINE_PROCESS_TYPE =
      new GenericType<PipelineProcess>() {};
  private static final GenericType<PipelineStep> PIPELINE_STEP_TYPE =
      new GenericType<PipelineStep>() {};
  private static final GenericType<PipelineWorkflow> PIPELINE_WORKFLOW_TYPE =
      new GenericType<PipelineWorkflow>() {};

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

  public long createPipelineProcess(UUID datasetKey, int attempt) {
    return post(Long.class, new PipelineProcessParameters(datasetKey, attempt), PROCESS_PATH);
  }

  public long addPipelineStep(long processKey, PipelineStep pipelineStep) {
    return post(Long.class, pipelineStep, PROCESS_PATH, String.valueOf(processKey));
  }

  public PipelineStep getPipelineStep(long processkey, long stepKey) {
    return get(
        PIPELINE_STEP_TYPE,
        PROCESS_PATH,
        String.valueOf(processkey),
        STEP_PATH,
        String.valueOf(stepKey));
  }

  public void updatePipelineStep(long processKey, long stepKey, PipelineStep.Status status) {
    put(status, PROCESS_PATH, String.valueOf(processKey), STEP_PATH, String.valueOf(stepKey));
  }

  public PipelineWorkflow getPipelineWorkflow(UUID datasetKey, int attempt) {
    return get(
        PIPELINE_WORKFLOW_TYPE, "workflow", String.valueOf(datasetKey), String.valueOf(attempt));
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

  public ClientResponse runPipelineAttempt(UUID datasetKey, int attempt, String steps, String reason) {
    return getResource(RUN_PATH, datasetKey.toString(), String.valueOf(attempt))
        .queryParam("steps", steps)
        .queryParam("reason", reason)
        .type("application/json")
        .post(ClientResponse.class);
  }
}
