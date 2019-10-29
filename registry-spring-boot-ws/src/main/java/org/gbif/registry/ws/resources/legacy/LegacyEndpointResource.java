package org.gbif.registry.ws.resources.legacy;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.registry.ws.model.ErrorResponse;
import org.gbif.registry.ws.model.LegacyEndpoint;
import org.gbif.registry.ws.model.LegacyEndpointResponse;
import org.gbif.registry.ws.util.LegacyResourceUtils;
import org.gbif.ws.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Handle all legacy web service Endpoint requests (excluding IPT requests), previously handled by the GBRDS.
 */
@RestController
@RequestMapping("registry/service")
public class LegacyEndpointResource {

  private static final Logger LOG = LoggerFactory.getLogger(LegacyEndpointResource.class);

  private final DatasetService datasetService;

  public LegacyEndpointResource(DatasetService datasetService) {
    this.datasetService = datasetService;
  }

  /**
   * Register Dataset Endpoint, handling incoming request with path /resource/service. The access point URL, type, and
   * dataset key are mandatory. Only after both the endpoint has been persisted is a ResponseEntity with
   * HttpStatus.CREATED returned.
   *
   * @param endpoint LegacyEndpoint with HTTP form parameters
   * @return ResponseEntity with HttpStatus.CREATED if successful
   */
  @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.APPLICATION_XML_VALUE)
  public ResponseEntity registerEndpoint(@RequestParam LegacyEndpoint endpoint) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (endpoint != null) {
      // set required fields
      endpoint.setCreatedBy(authentication.getName());
      endpoint.setModifiedBy(authentication.getName());

      // required fields present, and corresponding dataset exists?
      if (LegacyResourceUtils.isValid(endpoint, datasetService)) {

        // persist endpoint
        int key = datasetService.addEndpoint(endpoint.getDatasetKey(), endpoint);
        LOG.info("Endpoint created successfully, key={}", key);

        // generate response
        return ResponseEntity
          .status(HttpStatus.CREATED)
          .cacheControl(CacheControl.noCache())
          .body(endpoint);
      } else {
        LOG.error("Mandatory parameter(s) missing or invalid!");
      }
    }
    LOG.error("Endpoint creation failed");
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .cacheControl(CacheControl.noCache())
      .build();
  }

  /**
   * Delete all Endpoints for a Dataset, handling incoming request with path /resource/service and query parameter
   * resourceKey. Only credentials are mandatory. If deletion is successful, returns ResponseEntity with HttpStatus.OK.
   *
   * @param datasetKey dataset key (UUID) coming in as query param
   * @return ResponseEntity with HttpStatus.OK if successful
   */
  @DeleteMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public ResponseEntity deleteAllDatasetEndpoints(@RequestParam("resourceKey") UUID datasetKey) {
    if (datasetKey != null) {
      // retrieve existing dataset
      Dataset existing = datasetService.get(datasetKey);
      if (existing != null) {

        // delete dataset's endpoints
        List<Endpoint> endpointList = existing.getEndpoints();
        for (Endpoint endpoint : endpointList) {
          datasetService.deleteEndpoint(datasetKey, endpoint.getKey());
        }

        LOG.info("Dataset's endpoints deleted successfully, key={}", datasetKey);
        return ResponseEntity
          .status(HttpStatus.OK)
          .cacheControl(CacheControl.noCache())
          .build();

      } else {
        LOG.error("Request invalid. Dataset (whose endpoints are to be deleted) no longer exists!");
      }
    }
    LOG.error("Endpoint deletion failed");
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .cacheControl(CacheControl.noCache())
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
   * @return ResponseEntity with list of Endpoints or empty list with error message if none found
   */
  @GetMapping(consumes = MediaType.TEXT_PLAIN_VALUE,
    produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity endpointsForDataset(@RequestParam("resourceKey") UUID datasetKey, @RequestParam("op") String op) {

    // get all service types?
    if (op != null && op.equalsIgnoreCase("types")) {
      // TODO: replace static list http://dev.gbif.org/issues/browse/REG-394
      try {
        String content = Resources.toString(Resources.getResource("legacy/service_types.json"), Charsets.UTF_8);
        LOG.debug("Get service types finished");
        return ResponseEntity
          .status(HttpStatus.OK)
          .cacheControl(CacheControl.noCache())
          .body(content);
      } catch (IOException e) {
        LOG.error("An error occurred retrieving service types");
        return ResponseEntity
          .status(HttpStatus.INTERNAL_SERVER_ERROR)
          .cacheControl(CacheControl.noCache())
          .build();
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
        // return array, required for serialization otherwise might get an exception
        // writer for Java class java.util.ArrayList
        LegacyEndpointResponse[] array = endpoints.toArray(new LegacyEndpointResponse[endpoints.size()]);
        return ResponseEntity
          .status(HttpStatus.OK)
          .cacheControl(CacheControl.noCache())
          .body(array);
      } catch (NotFoundException e) {
        LOG.error("The dataset with key {} specified by query parameter does not exist", datasetKey);
        // the dataset didn't exist, and expected response is "{Error: "No services associated to the organisation}"
        return ResponseEntity
          .status(HttpStatus.OK)
          .cacheControl(CacheControl.noCache())
          .body(new ErrorResponse("No dataset matches the key provided"));
      }
    }
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .cacheControl(CacheControl.noCache())
      .build();
  }
}
