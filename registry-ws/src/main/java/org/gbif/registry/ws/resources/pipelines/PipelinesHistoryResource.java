package org.gbif.registry.ws.resources.pipelines;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.pipelines.PipelineExecution;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.StepType;
import org.gbif.api.model.pipelines.ws.PipelineProcessParameters;
import org.gbif.api.model.pipelines.ws.PipelineStepParameters;
import org.gbif.registry.pipelines.PipelinesHistoryTrackingService;
import org.gbif.registry.pipelines.model.RunPipelineResponse;

import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Inject;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.EDITOR_ROLE;

/** Pipelines History service. */
@Produces(MediaType.APPLICATION_JSON)
@Path("pipelines/history")
public class PipelinesHistoryResource {

  private static final String PROCESS_PATH = "process/";
  private static final String RUN_PATH = "run/";

  private final PipelinesHistoryTrackingService historyTrackingService;

  @Inject
  public PipelinesHistoryResource(PipelinesHistoryTrackingService historyTrackingService) {
    this.historyTrackingService = historyTrackingService;
  }

  /** Lists the history of all pipelines. */
  @GET
  public PagingResponse<PipelineProcess> history(@Context Pageable pageable) {
    return historyTrackingService.history(pageable);
  }

  /** Lists teh history of a dataset. */
  @GET
  @Path("{datasetKey}")
  public PagingResponse<PipelineProcess> history(
      @PathParam("datasetKey") UUID datasetKey, @Context Pageable pageable) {
    return historyTrackingService.history(datasetKey, pageable);
  }

  /** Gets the data of a {@link PipelineProcess}. */
  @GET
  @Path("{datasetKey}/{attempt}")
  public PipelineProcess getPipelineProcess(
      @PathParam("datasetKey") UUID datasetKey, @PathParam("attempt") int attempt) {
    return historyTrackingService.get(datasetKey, attempt);
  }

  @POST
  @Path(PROCESS_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
  public long createPipelineProcess(
      PipelineProcessParameters params, @Context SecurityContext security) {
    return historyTrackingService.createOrGet(
        params.getDatasetKey(), params.getAttempt(), security.getUserPrincipal().getName());
  }

  /** Adds a new pipeline step. */
  @POST
  @Path(PROCESS_PATH + "{processKey}")
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
  public long addPipelineExecution(
      @PathParam("processKey") long processKey,
      PipelineExecution pipelineExecution,
      @Context SecurityContext security) {
    return historyTrackingService.addPipelineExecution(
        processKey, pipelineExecution, security.getUserPrincipal().getName());
  }

  /** Adds a new pipeline step. */
  @POST
  @Path(PROCESS_PATH + "{processKey}/{executionKey}")
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
  public long addPipelineStep(
      @PathParam("processKey") long processKey,
      @PathParam("executionKey") long executionKey,
      PipelineStep pipelineStep,
      @Context SecurityContext security) {
    return historyTrackingService.addPipelineStep(
        processKey, executionKey, pipelineStep, security.getUserPrincipal().getName());
  }

  @GET
  @Path(PROCESS_PATH + "{processKey}/{executionKey}/{stepKey}")
  public PipelineStep getPipelineStep(
      @PathParam("processKey") long processKey, @PathParam("executionKey") long executionKey, @PathParam("stepKey") long stepKey) {
    return historyTrackingService.getPipelineStep(stepKey);
  }

  /** Updates the step status. */
  @PUT
  @Path(PROCESS_PATH + "{processKey}/{executionKey}/{stepKey}")
  @Consumes({MediaType.APPLICATION_JSON})
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
  public void updatePipelineStepStatusAndMetrics(
      @PathParam("processKey") long processKey,
      @PathParam("executionKey") long executionKey,
      @PathParam("stepKey") long stepKey,
      PipelineStepParameters stepParams,
      @Context SecurityContext security) {
    Objects.requireNonNull(stepParams, "Pipeline Step parameters are required");

    historyTrackingService.updatePipelineStepStatusAndMetrics(
        processKey,
        executionKey,
        stepKey,
        stepParams.getStatus(),
        stepParams.getMetrics(),
        security.getUserPrincipal().getName());
  }

  /** Runs the last attempt for all datasets. */
  @POST
  @Path(RUN_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
  public Response runAll(
      @QueryParam("steps") String steps,
      @QueryParam("reason") String reason,
      @Context SecurityContext security,
      @Nullable RunAllParams runAllParams) {
    return checkRunInputParams(steps, reason)
        .orElseGet(
            () ->
                toHttpResponse(
                    historyTrackingService.runLastAttempt(
                        parseSteps(steps),
                        reason,
                        security.getUserPrincipal().getName(),
                        runAllParams != null ? runAllParams.datasetsToExclude : null)));
  }

  /** Restart last failed pipelines step for a dataset. */
  @POST
  @Path(RUN_PATH + "{datasetKey}")
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
  public Response runPipelineAttempt(
      @PathParam("datasetKey") UUID datasetKey,
      @QueryParam("steps") String steps,
      @QueryParam("reason") String reason,
      @Context SecurityContext security) {
    return checkRunInputParams(steps, reason)
        .orElseGet(
            () ->
                toHttpResponse(
                    historyTrackingService.runLastAttempt(
                        datasetKey,
                        parseSteps(steps),
                        reason,
                        security.getUserPrincipal().getName(),
                        null)));
  }

  /** Re-run a pipeline step. */
  @POST
  @Path(RUN_PATH + "{datasetKey}/{attempt}")
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
  public Response runPipelineAttempt(
      @PathParam("datasetKey") UUID datasetKey,
      @PathParam("attempt") int attempt,
      @QueryParam("steps") String steps,
      @QueryParam("reason") String reason,
      @Context SecurityContext security) {
    return checkRunInputParams(steps, reason)
        .orElseGet(
            () ->
                toHttpResponse(
                    historyTrackingService.runPipelineAttempt(
                        datasetKey,
                        attempt,
                        parseSteps(steps),
                        reason,
                        security.getUserPrincipal().getName(),
                        null)));
  }

  /** Transforms a {@link RunPipelineResponse} into a {@link Response}. */
  private static Response toHttpResponse(RunPipelineResponse runPipelineResponse) {
    if (runPipelineResponse.getResponseStatus()
        == RunPipelineResponse.ResponseStatus.PIPELINE_IN_SUBMITTED) {
      return Response.status(Response.Status.BAD_REQUEST).entity(runPipelineResponse).build();
    } else if (runPipelineResponse.getResponseStatus()
        == RunPipelineResponse.ResponseStatus.UNSUPPORTED_STEP) {
      return Response.status(Response.Status.NOT_ACCEPTABLE).entity(runPipelineResponse).build();
    } else if (runPipelineResponse.getResponseStatus()
        == RunPipelineResponse.ResponseStatus.ERROR) {
      return Response.serverError().entity(runPipelineResponse).build();
    }

    return Response.ok().entity(runPipelineResponse).build();
  }

  private Optional<Response> checkRunInputParams(String steps, String reason) {
    if (Strings.isNullOrEmpty(steps) || Strings.isNullOrEmpty(reason)) {
      return Optional.of(
          Response.status(Response.Status.BAD_REQUEST)
              .entity(
                  RunPipelineResponse.builder()
                      .setMessage("Steps and reason parameters are required")
                      .build())
              .build());
    }

    return Optional.empty();
  }

  /** Parse steps argument. */
  private Set<StepType> parseSteps(String steps) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(steps));
    return Arrays.stream(steps.split(","))
        .map(s -> StepType.valueOf(s.toUpperCase()))
        .collect(Collectors.toSet());
  }

  /** Encapsulates the params to pass in the body for the runAll method. */
  private static class RunAllParams {
    List<UUID> datasetsToExclude = new ArrayList<>();

    // getters and setters needed for jackson

    public List<UUID> getDatasetsToExclude() {
      return datasetsToExclude;
    }

    public void setDatasetsToExclude(List<UUID> datasetsToExclude) {
      this.datasetsToExclude = datasetsToExclude;
    }
  }
}
