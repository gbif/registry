package org.gbif.registry.ws.resources;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.registry.doi.DoiGenerator;
import org.gbif.registry.persistence.mapper.DatasetOccurrenceDownloadMapper;
import org.gbif.registry.persistence.mapper.OccurrenceDownloadMapper;
import org.gbif.registry.ws.guice.Trim;
import org.gbif.registry.ws.util.DataCiteConverter;
import org.gbif.ws.server.interceptor.NullToNotFound;
import org.gbif.ws.util.ExtraMediaTypes;

import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.jersey.api.NotFoundException;
import org.apache.bval.guice.Validate;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.util.DownloadSecurityUtils.checkUserIsInSecurityContext;
import static org.gbif.registry.ws.util.DownloadSecurityUtils.clearSensitiveData;

/**
 * Occurrence download resource/web service.
 */
@Singleton
@Path("occurrence/download")
@Produces({MediaType.APPLICATION_JSON, ExtraMediaTypes.APPLICATION_JAVASCRIPT})
@Consumes(MediaType.APPLICATION_JSON)
public class OccurrenceDownloadResource implements OccurrenceDownloadService {

  private final OccurrenceDownloadMapper occurrenceDownloadMapper;

  private final DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper;

  private final DatasetService datasetService;

  private final DoiGenerator doiGenerator;

  //Download final/failed states
  private final EnumSet<Download.Status> FAILED_STATES = EnumSet.of(Download.Status.KILLED, Download.Status.CANCELLED,
                                                                    Download.Status.FAILED);

  //Page size to iterate over dataset usages
  private static final int USAGES_PAGE_SIZE = 400;


  //DOI logging marker
  private static Marker DOI_SMTP = MarkerFactory.getMarker("DOI_SMTP");
  private static Logger LOG = LoggerFactory.getLogger(DoiGenerator.class);

  @Context
  private SecurityContext securityContext;

  @Inject
  public OccurrenceDownloadResource(OccurrenceDownloadMapper occurrenceDownloadMapper, DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper, DoiGenerator doiGenerator, DatasetService datasetService) {
    this.occurrenceDownloadMapper = occurrenceDownloadMapper;
    this.datasetOccurrenceDownloadMapper = datasetOccurrenceDownloadMapper;
    this.doiGenerator = doiGenerator;
    this.datasetService = datasetService;
  }


  @POST
  @Trim
  @Transactional
  @Validate(groups = {PrePersist.class, Default.class})
  @RolesAllowed(ADMIN_ROLE)
  @Override
  public void create(@Valid @NotNull @Trim Download occurrenceDownload) {
    occurrenceDownload.setDoi(doiGenerator.newDownloadDOI());
    occurrenceDownloadMapper.create(occurrenceDownload);
  }

  @GET
  @Path("{key}")
  @Nullable
  @NullToNotFound
  @Override
  public Download get(@PathParam("key") String key) {
    Download download = occurrenceDownloadMapper.get(key);
    if (download != null) { // the user can request a non-existing download
      clearSensitiveData(securityContext, download);
    }
    return download;
  }

  /**
   * Lists all the downloads. This operation can be executed by role ADMIN only.
   */
  @GET
  @RolesAllowed(ADMIN_ROLE)
  @Override
  public PagingResponse<Download> list(@Context Pageable page) {
    return new PagingResponse<Download>(page, (long) occurrenceDownloadMapper.count(),
      occurrenceDownloadMapper.list(page));
  }

  @GET
  @Path("user/{user}")
  @NullToNotFound
  public PagingResponse<Download> listByUser(@PathParam("user") String user, @Context Pageable page) {
    checkUserIsInSecurityContext(user, securityContext);
    return new PagingResponse<Download>(page, (long) occurrenceDownloadMapper.countByUser(user),
      occurrenceDownloadMapper.listByUser(user, page));
  }

  @PUT
  @Path("{key}")
  @Transactional
  @Override
  public void update(Download download) {
    // The current download is retrieved because its user could be modified during the update
    Download currentDownload = get(download.getKey());
    Preconditions.checkNotNull(currentDownload);
    checkUserIsInSecurityContext(currentDownload.getRequest().getCreator(), securityContext);
    updateDownloadDOI(download, currentDownload);
    occurrenceDownloadMapper.update(download);
  }

  /**
   * Updates the download DOI according to the download status.
   * If the download succeeded its DOI is registered; if the download status is one the FAILED_STATES
   * the DOI is removed, otherwise doesn't nothing.
   */
  private void updateDownloadDOI(Download download, Download previousDownload){
    if(download.isAvailable() && previousDownload.getStatus() != Download.Status.SUCCEEDED){
      try {
        doiGenerator.registerDownload(download.getDoi(), buildMetadata(download), download.getKey());
      } catch(InvalidMetadataException error) {
        LOG.error(DOI_SMTP, "Invalid metadata for download {} with doi {} ", download.getKey(), download.getDoi(),  error);
      }

    } else if(FAILED_STATES.contains(download.getStatus())){
      doiGenerator.delete(download.getDoi());
    }
  }

  /**
   * Creates the DataCite metadata for a download object.
   */
  private DataCiteMetadata buildMetadata(Download download) {

    List<DatasetOccurrenceDownloadUsage> response = null;
    List<DatasetOccurrenceDownloadUsage> usages = Lists.newArrayList();
    PagingRequest pagingRequest = new PagingRequest(0, USAGES_PAGE_SIZE);

    while (response == null || !response.isEmpty()) {
      response = datasetOccurrenceDownloadMapper.listByDownload(download.getKey(), pagingRequest);
      usages.addAll(response);
      pagingRequest.nextPage();
    }

    return DataCiteConverter.convert(download, usages);
  }



  @GET
  @Path("{key}/datasets")
  @Override
  @NullToNotFound
  public PagingResponse<DatasetOccurrenceDownloadUsage> listDatasetUsages(@PathParam("key") String downloadKey,
                                                                   @Context Pageable page){
    Download download = get(downloadKey);
    if(download != null) {
      return new PagingResponse<DatasetOccurrenceDownloadUsage>(page,
                                   download.getNumberDatasets(),
                                   datasetOccurrenceDownloadMapper.listByDownload(downloadKey, page));
    }
    throw new NotFoundException();
  }
}
