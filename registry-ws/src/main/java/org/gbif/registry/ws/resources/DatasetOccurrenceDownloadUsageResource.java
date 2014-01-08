package org.gbif.registry.ws.resources;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.service.registry.DatasetOccurrenceDownloadUsageService;
import org.gbif.registry.persistence.mapper.DatasetOccurrenceDownloadMapper;
import org.gbif.ws.server.interceptor.NullToNotFound;
import org.gbif.ws.util.ExtraMediaTypes;

import java.util.UUID;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.bval.guice.Validate;
import org.mybatis.guice.transactional.Transactional;
import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;

/**
 * Occurrence download resource/web service.
 */
@Singleton
@Path("occurrence/download/dataset")
@Produces({MediaType.APPLICATION_JSON, ExtraMediaTypes.APPLICATION_JAVASCRIPT})
@Consumes(MediaType.APPLICATION_JSON)
public class DatasetOccurrenceDownloadUsageResource implements DatasetOccurrenceDownloadUsageService {

  private final DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper;

  @Inject
  public DatasetOccurrenceDownloadUsageResource(DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper) {
    this.datasetOccurrenceDownloadMapper = datasetOccurrenceDownloadMapper;
  }


  @POST
  @Transactional
  @Validate(groups = {PrePersist.class, Default.class})
  @RolesAllowed(ADMIN_ROLE)
  @Override
  public void create(@Valid @NotNull DatasetOccurrenceDownloadUsage downloadDataset) {
    datasetOccurrenceDownloadMapper.create(downloadDataset);
  }

  @GET
  @Path("/{datasetKey}")
  @NullToNotFound
  @Override
  public PagingResponse<DatasetOccurrenceDownloadUsage> listByDataset(
    @PathParam("datasetKey") UUID datasetKey, @Context Pageable page) {
    return new PagingResponse<DatasetOccurrenceDownloadUsage>(page,
      (long) datasetOccurrenceDownloadMapper.countByDataset(datasetKey),
      datasetOccurrenceDownloadMapper.listByDataset(datasetKey, page));
  }

}
