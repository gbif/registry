package org.gbif.registry.ws.resources.pipelines;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.crawler.pipelines.PipelineProcess;
import org.gbif.api.model.crawler.pipelines.PipelineStep;
import org.gbif.api.model.crawler.pipelines.PipelineWorkflow;
import org.gbif.api.model.crawler.pipelines.StepType;
import org.gbif.registry.pipelines.PipelinesHistoryTrackingService;
import org.gbif.registry.pipelines.RunPipelineResponse;
import org.gbif.ws.util.ExtraMediaTypes;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.google.inject.Inject;

/**
 * Pipelines History service.
 */
@Produces({MediaType.APPLICATION_JSON, ExtraMediaTypes.APPLICATION_JAVASCRIPT})
@Path("pipelines/history")
public class PipelinesHistoryResource {

  private static final String REGISTRY_ADMIN = "REGISTRY_ADMIN";
  private static final String REGISTRY_EDITOR = "REGISTRY_EDITOR";

  private final PipelinesHistoryTrackingService historyTrackingService;

  @Inject
  public PipelinesHistoryResource(PipelinesHistoryTrackingService historyTrackingService) {
    this.historyTrackingService = historyTrackingService;
  }

  /**
   * Transforms a {@link RunPipelineResponse} into a {@link Response}.
   */
  private static Response toHttpResponse(RunPipelineResponse runPipelineResponse) {
    if (runPipelineResponse.getResponseStatus() == RunPipelineResponse.ResponseStatus.PIPELINE_IN_SUBMITTED) {
      return Response.status(Response.Status.BAD_REQUEST).entity(runPipelineResponse).build();
    } else if (runPipelineResponse.getResponseStatus() == RunPipelineResponse.ResponseStatus.UNSUPPORTED_STEP) {
      return Response.status(Response.Status.NOT_ACCEPTABLE).entity(runPipelineResponse).build();
    } else if (runPipelineResponse.getResponseStatus() == RunPipelineResponse.ResponseStatus.ERROR) {
      return Response.serverError().entity(runPipelineResponse).build();
    }

    return Response.ok().entity(runPipelineResponse).build();
  }

  /**
   * Lists the history of all pipelines.
   */
  @GET
  public PagingResponse<PipelineProcess> history(@Context Pageable pageable) {
    return historyTrackingService.history(pageable);
  }

  /**
   * Parse steps argument.
   */
  private Set<StepType> parseSteps(String steps) {
    Objects.requireNonNull(steps, "Steps can't be null");
    return Arrays.stream(steps.split(","))
                          .map(s -> StepType.valueOf(s.toUpperCase()))
                          .collect(Collectors.toSet());
  }

  @GET
  @Path("crawlall")
  public Response crawlAll() {
    return toHttpResponse(historyTrackingService.crawlAll());
  }

  /**
   * Lists the history of all pipelines.
   */
  @POST
  @RolesAllowed({REGISTRY_ADMIN, REGISTRY_EDITOR})
  public Response all(@QueryParam("steps") String steps, @QueryParam("reason") String reason, @Context SecurityContext security) {
    return toHttpResponse(historyTrackingService.runLastAttempt(parseSteps(steps), reason,
                                                                security.getUserPrincipal().getName()));
  }

  /**
   * Lists teh history of a dataset.
   */
  @GET
  @Path("{datasetKey}")
  public PagingResponse<PipelineProcess> history(@PathParam("datasetKey") String datasetKey, @Context Pageable pageable) {
    return historyTrackingService.history(UUID.fromString(datasetKey), pageable);
  }

  /**
   * Gets the data of a {@link PipelineProcess}.
   */
  @GET
  @Path("{datasetKey}/{attempt}")
  public PipelineProcess get(@PathParam("datasetKey") String datasetKey, @PathParam("attempt") String attempt) {
   return historyTrackingService.get(UUID.fromString(datasetKey), Integer.parseInt(attempt));
  }

  @GET
  @Path("workflow/{datasetKey}/{attempt}")
  public PipelineWorkflow getPipelineWorkflow(@PathParam("datasetKey") String datasetKey, @PathParam("attempt") String attempt) {
    return historyTrackingService.getPipelineWorkflow(UUID.fromString(datasetKey), Integer.parseInt(attempt));
  }

  /**
   * Re-run a pipeline step.
   */
  @POST
  @Path("{datasetKey}/{attempt}")
  @RolesAllowed({REGISTRY_ADMIN, REGISTRY_EDITOR})
  public Response runPipelineAttempt(@PathParam("datasetKey") String datasetKey, @PathParam("attempt") String attempt,
                                     @QueryParam("steps") String steps, @QueryParam("reason") String reason,
                                     @Context SecurityContext security) {

    return  toHttpResponse(historyTrackingService.runPipelineAttempt(UUID.fromString(datasetKey),
                                                                     Integer.parseInt(attempt),
                                                                     parseSteps(steps), reason,
                                                                     security.getUserPrincipal().getName()));
  }

  /**
   * Adds a new pipeline step.
   */
  @POST
  @Path("process/{processKey}")
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed({REGISTRY_ADMIN, REGISTRY_EDITOR})
  public PipelineStep addPipelineStep(@PathParam("processKey") String processKey, PipelineStep pipelineStep, @Context
    SecurityContext security) {
    return historyTrackingService.addPipelineStep(Long.parseLong(processKey), pipelineStep, security.getUserPrincipal().getName());
  }


  /**
   * Updates the step status.
   */
  @PUT
  @Path("process/{processKey}")
  @RolesAllowed({REGISTRY_ADMIN, REGISTRY_EDITOR})
  public void updatePipelineStep(@PathParam("processKey") String processKey, String status, @Context SecurityContext security) {
    historyTrackingService.updatePipelineStepStatus(Long.parseLong(processKey), PipelineStep.Status.valueOf(status.toUpperCase()),
                                                    security.getUserPrincipal().getName());
  }


  /**
   * Restart last failed pipelines step
   */
  @POST
  @Path("{datasetKey}")
  @RolesAllowed({REGISTRY_ADMIN, REGISTRY_EDITOR})
  public Response runPipelineAttempt(@PathParam("datasetKey") String datasetKey, @QueryParam("steps") String steps,
                                     @QueryParam("reason") String reason, @Context SecurityContext security) {
    return toHttpResponse(historyTrackingService.runLastAttempt(UUID.fromString(datasetKey),
                                                                parseSteps(steps), reason,
                                                                security.getUserPrincipal().getName()));
  }

}
