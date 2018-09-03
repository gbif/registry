package org.gbif.registry.ws.resources;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.service.registry.DatasetOccurrenceDownloadUsageService;
import org.gbif.registry.persistence.mapper.DatasetOccurrenceDownloadMapper;
import org.gbif.ws.server.interceptor.NullToNotFound;
import org.gbif.ws.util.ExtraMediaTypes;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import static org.gbif.registry.ws.util.DownloadSecurityUtils.clearSensitiveData;

/**
 * Occurrence download resource/web service.
 */
@Singleton
@Path("occurrence/download/dataset")
@Produces({MediaType.APPLICATION_JSON, ExtraMediaTypes.APPLICATION_JAVASCRIPT})
@Consumes(MediaType.APPLICATION_JSON)
public class DatasetOccurrenceDownloadUsageResource implements DatasetOccurrenceDownloadUsageService {

  private final DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper;

  @Context
  private SecurityContext securityContext;

  @Inject
  public DatasetOccurrenceDownloadUsageResource(DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper) {
    this.datasetOccurrenceDownloadMapper = datasetOccurrenceDownloadMapper;
  }

  @GET
  @Path("/{datasetKey}")
  @NullToNotFound
  @Override
  public PagingResponse<DatasetOccurrenceDownloadUsage> listByDataset(
    @PathParam("datasetKey") UUID datasetKey, @Context Pageable page) {
    List<DatasetOccurrenceDownloadUsage> usages = datasetOccurrenceDownloadMapper.listByDataset(datasetKey, page);
    clearSensitiveData(securityContext, usages);
    return new PagingResponse<>(page,
      (long) datasetOccurrenceDownloadMapper.countByDataset(datasetKey), usages);
  }

}
