package org.gbif.registry.ws.resources.legacy;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.registry.ws.model.ErrorResponse;
import org.gbif.registry.ws.model.LegacyEndpoint;
import org.gbif.registry.ws.model.LegacyEndpointResponse;
import org.gbif.registry.ws.util.LegacyResourceConstants;
import org.gbif.registry.ws.util.LegacyResourceUtils;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.core.InjectParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle all legacy web service Endpoint requests (excluding IPT requests), previously handled by the GBRDS.
 */
@Singleton
@Path("registry/service")
public class LegacyEndpointResource {

  private static final Logger LOG = LoggerFactory.getLogger(LegacyEndpointResource.class);

  private final DatasetService datasetService;

  @Inject
  public LegacyEndpointResource(DatasetService datasetService) {
    this.datasetService = datasetService;
  }

  /**
   * Register Dataset Endpoint, handling incoming request with path /resource/service. The access point URL, type, and
   * dataset key are mandatory. Only after both the endpoint has been persisted is a Response with Status.CREATED
   * returned.
   *
   * @param endpoint LegacyEndpoint with HTTP form parameters having been injected from Jersey
   * @param security SecurityContext (security related information)
   *
   * @return Response with Status.CREATED if successful
   */
  @POST
  @Produces(MediaType.APPLICATION_XML)
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response registerEndpoint(@InjectParam LegacyEndpoint endpoint, @Context SecurityContext security) {
    if (endpoint != null) {
      // set required fields
      String user = security.getUserPrincipal().getName();
      endpoint.setCreatedBy(user);
      endpoint.setModifiedBy(user);

      // required fields present, and corresponding dataset exists?
      if (LegacyResourceUtils.isValid(endpoint, datasetService)) {

        // persist endpoint
        int key = datasetService.addEndpoint(endpoint.getDatasetKey(), endpoint);
        LOG.info("Endpoint created successfully, key=%s", String.valueOf(key));

        // generate response
        return Response.status(Response.Status.CREATED).cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED)
          .entity(endpoint).build();
      } else {
        LOG.error("Mandatory parameter(s) missing or invalid!");
      }
    }
    LOG.error("Endpoint creation failed");
    return Response.status(Response.Status.BAD_REQUEST).cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED)
      .build();
  }

  /**
   * Delete all Endpoints for a Dataset, handling incoming request with path /resource/service and query parameter
   * resourceKey. Only credentials are mandatory. If deletion is successful, returns Response with Status.OK.
   *
   * @param datasetKey dataset key (UUID) coming in as query param
   *
   * @return Response with Status.OK if successful
   */
  @DELETE
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response deleteAllDatasetEndpoints(@QueryParam("resourceKey") UUID datasetKey) {
    if (datasetKey != null) {
      // retrieve existing dataset
      Dataset existing = datasetService.get(datasetKey);
      if (existing != null) {

        // delete dataset's endpoints
        List<Endpoint> endpointList = existing.getEndpoints();
        for (Endpoint endpoint : endpointList) {
          datasetService.deleteEndpoint(datasetKey, endpoint.getKey());
        }

        LOG.info("Dataset's endpoints deleted successfully, key=%s", datasetKey.toString());
        return Response.status(Response.Status.OK).cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED).build();

      } else {
        LOG.error("Request invalid. Dataset (whose endpoints are to be deleted) no longer exists!");
      }
    }
    LOG.error("Endpoint deletion failed");
    return Response.status(Response.Status.BAD_REQUEST).cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED)
      .build();
  }

  /**
   * Retrieve all Endpoints associated to a Dataset, handling incoming request with path /service and query parameter
   * resourceKey. The dataset key query parameter is mandatory. Only after both the datasetKey is verified to
   * correspond to an existing Dataset, is a Response including the list of Endpoints returned.
   * </br>
   * Alternatively, get a list of all service types, handling incoming request with path /service.json and query
   * parameter op=types
   *
   * @param datasetKey dataset key (UUID) coming in as query param
   *
   * @return Response with list of Endpoints or empty list with error message if none found
   */
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @Consumes(MediaType.TEXT_PLAIN)
  public Response endpointsForDataset(@QueryParam("resourceKey") UUID datasetKey, @QueryParam("op") String op) {

    // get all service types?
    if (op != null && op.equalsIgnoreCase("types")) {
      // TODO: replace static list http://dev.gbif.org/issues/browse/REG-394
      try {
        String content = Resources.toString(Resources.getResource("legacy/service_types.json"), Charsets.UTF_8);
        LOG.debug("Get service types finished");
        return Response.status(Response.Status.OK).entity(content)
          .cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED).build();
      } catch (IOException e) {
        LOG.error("An error occurred retrieving service types");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED).build();
      }
    } else if (datasetKey != null) {
      try {
        // verify Dataset with key exists, otherwise NotFoundException gets thrown
        datasetService.get(datasetKey);

        LOG.debug("Get all Endpoints for Dataset, key={}", datasetKey);
        List<LegacyEndpointResponse> endpoints = Lists.newArrayList();

        LOG.debug("Requesting all endpoints for dataset, key={}", datasetKey);
        List<Endpoint> response = datasetService.listEndpoints(datasetKey);
        for (Endpoint e : response) {
          endpoints.add(new LegacyEndpointResponse(e, datasetKey));
        }

        LOG.debug("Get all Endpoints for Dataset finished");
        // return array, required for serialization otherwise get com.sun.jersey.api.MessageException: A message body
        // writer for Java class java.util.ArrayList
        LegacyEndpointResponse[] array = endpoints.toArray(new LegacyEndpointResponse[endpoints.size()]);
        return Response.status(Response.Status.OK).entity(array)
          .cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED).build();
      } catch (NotFoundException e) {
        LOG.error("The dataset with key {} specified by query parameter does not exist", datasetKey);
        // the dataset didn't exist, and expected response is "{Error: "No services associated to the organisation}"
        return Response.status(Response.Status.OK).entity(new ErrorResponse("No dataset matches the key provided"))
          .cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED).build();
      }
    }
    return Response.status(Response.Status.BAD_REQUEST).cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED)
      .build();
  }
}
