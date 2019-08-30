package org.gbif.registry.ws.client.pipelines;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.crawler.pipelines.PipelineProcess;
import org.gbif.api.model.crawler.pipelines.PipelineStep;
import org.gbif.api.model.crawler.pipelines.PipelineWorkflow;
import org.gbif.ws.client.BaseWsClient;

import javax.annotation.Nullable;
import javax.ws.rs.core.Response;

import com.google.inject.Inject;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;

/** WS client for the pipelines history tracking service. */
public class PipelinesHistoryWsClient extends BaseWsClient {

  private static final GenericType<PagingResponse<PipelineProcess>> PAGING_PIPELINE_PROCESS =
      new GenericType<PagingResponse<PipelineProcess>>() {};
  private static final GenericType<PipelineProcess> PIPELINE_PROCESS_TYPE =
      new GenericType<PipelineProcess>() {};
  private static final GenericType<PipelineStep> PIPELINE_STEP_TYPE =
      new GenericType<PipelineStep>() {};
  private static final GenericType<PipelineWorkflow> PIPELINE_WORKFLOW_TYPE =
      new GenericType<PipelineWorkflow>() {};
  private static final GenericType<Response> RESPONSE_TYPE = new GenericType<Response>() {};

  @Inject
  public PipelinesHistoryWsClient(WebResource resource, @Nullable ClientFilter authFilter) {
    super(resource.path("pipelines/history"));
    if (authFilter != null) {
      this.resource.addFilter(authFilter);
    }
  }

  public PagingResponse<PipelineProcess> history(@Nullable Pageable pageable) {
    return get(PAGING_PIPELINE_PROCESS, pageable);
  }

  public Response crawlAll() {
    return get(RESPONSE_TYPE, "crawlall");
  }

  public Response all(String steps, String reason) {
    return getResource()
        .queryParam("steps", steps)
        .queryParam("reason", reason)
        .type("application/json")
        .post(RESPONSE_TYPE);
  }

  public PagingResponse<PipelineProcess> history(String datasetKey, Pageable pageable) {
    return get(PAGING_PIPELINE_PROCESS, pageable, datasetKey);
  }

  public PipelineProcess getPipelineProcess(String datasetKey, String attempt) {
    return get(PIPELINE_PROCESS_TYPE, datasetKey, attempt);
  }

  public PipelineWorkflow getPipelineWorkflow(String datasetKey, String attempt) {
    return get(PIPELINE_WORKFLOW_TYPE, "workflow", datasetKey, attempt);
  }

  public Response runPipelineAttempt(
      String datasetKey, String attempt, String steps, String reason) {

    return getResource(datasetKey, attempt)
        .queryParam("steps", steps)
        .queryParam("reason", reason)
        .type("application/json")
        .post(RESPONSE_TYPE);
  }

  public PipelineStep addPipelineStep(String processKey, PipelineStep pipelineStep) {
    return post(PIPELINE_STEP_TYPE, pipelineStep, "process", processKey);
  }

  public void updatePipelineStep(String processKey, String status) {
    put(status, "process", processKey);
  }

  public Response runPipelineAttempt(String datasetKey, String steps, String reason) {
    return getResource(datasetKey)
        .queryParam("steps", steps)
        .queryParam("reason", reason)
        .type("application/json")
        .post(RESPONSE_TYPE);
  }
}
