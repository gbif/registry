package org.gbif.registry.ws.resources.pipelines;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.registry.pipelines.IngestionHistoryService;
import org.gbif.registry.pipelines.model.IngestionProcess;

import java.util.UUID;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Produces(MediaType.APPLICATION_JSON)
@Path("ingestion/history")
public class IngestionHistoryResource {

  private final IngestionHistoryService ingestionHistoryService;

  @Inject
  public IngestionHistoryResource(IngestionHistoryService ingestionHistoryService) {
    this.ingestionHistoryService = ingestionHistoryService;
  }

  @GET
  public PagingResponse<IngestionProcess> history(@Context Pageable pageable) {
    return ingestionHistoryService.ingestionHistory(pageable);
  }

  @GET
  @Path("{datasetKey}")
  public PagingResponse<IngestionProcess> history(
      @PathParam("datasetKey") UUID datasetKey, @Context Pageable pageable) {
    return ingestionHistoryService.ingestionHistory(datasetKey, pageable);
  }

  @GET
  @Path("{datasetKey}/{attempt}")
  public IngestionProcess getIngestion(
      @PathParam("datasetKey") UUID datasetKey, @PathParam("attempt") int attempt) {
    return ingestionHistoryService.getIngestionProcess(datasetKey, attempt);
  }
}
