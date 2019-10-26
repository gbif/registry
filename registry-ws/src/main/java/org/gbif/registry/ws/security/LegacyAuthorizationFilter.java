package org.gbif.registry.ws.security;

import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.ws.util.LegacyResourceConstants;

import java.util.UUID;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import com.google.inject.Inject;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A filter that will intercept legacy web service requests to /registry/* and perform authentication setting
 * a security context on the request.
 */
public class LegacyAuthorizationFilter implements ContainerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(LegacyAuthorizationFilter.class);

  private final InstallationService installationService;
  private final OrganizationService organizationService;
  private final DatasetService datasetService;

  // request HttpContext to access HTTP Headers during authorization
  @Context
  private HttpContext httpContext;
  @Context
  private UriInfo uriInfo;

  @Inject
  public LegacyAuthorizationFilter(InstallationService installationService, OrganizationService organizationService,
                                   DatasetService datasetService) {
    this.installationService = installationService;
    this.organizationService = organizationService;
    this.datasetService = datasetService;
  }

  @Override
  public ContainerRequest filter(ContainerRequest request) {
    String path = request.getPath().toLowerCase();
    // is it a legacy web service request?
    if (path.contains("registry/")) {
      // is it a GET request requiring authorization?
      if ("GET".equalsIgnoreCase(request.getMethod())) {

        // E.g. validate organization request, identified by param op=login
        if (request.getQueryParameters().getFirst("op") != null) {
          if (request.getQueryParameters().getFirst("op").equalsIgnoreCase("login")) {
            UUID organizationKey = retrieveKeyFromRequestPath(request);
            return authorizeOrganizationChange(organizationKey, request);
          }
        }
      }
      // is it a POST, PUT, DELETE request requiring authorization?
      else if ("POST".equalsIgnoreCase(request.getMethod())
        || "PUT".equalsIgnoreCase(request.getMethod())
        || "DELETE".equalsIgnoreCase(request.getMethod())) {
        // legacy installation request
        if (path.contains("/ipt")) {
          // register installation?
          if (path.endsWith("/register")) {
            return authorizeOrganizationChange(request);
          }
          // update installation?
          else if (path.contains("/update/")) {
            UUID installationKey = retrieveKeyFromRequestPath(request);
            return authorizeInstallationChange(request, installationKey);
          }
          // register dataset?
          else if (path.endsWith("/resource")) {
            return authorizeOrganizationChange(request);
          }
          // update dataset, delete dataset?
          else if (path.contains("/resource/")) {
            UUID datasetKey = retrieveKeyFromRequestPath(request);
            return authorizeOrganizationDatasetChange(request, datasetKey);
          }
        }
        // legacy dataset request
        else if (path.contains("/resource")) {
          // register dataset?
          if (path.endsWith("/resource")) {
            return authorizeOrganizationChange(request);
          }
          // update dataset, delete dataset?
          else if (path.contains("/resource/")) {
            UUID datasetKey = retrieveKeyFromRequestPath(request);
            return authorizeOrganizationDatasetChange(request, datasetKey);
          }
        }
        // legacy endpoint request
        else if (path.endsWith("/service")) {
          // add endpoint?
          if (request.getQueryParameters().isEmpty()) {
            UUID datasetKey = retrieveDatasetKeyFromFormParameters(request);
            return authorizeOrganizationDatasetChange(request, datasetKey);
          }
          // delete all dataset's enpoints?
          else if (uriInfo.getRequestUri().toString().contains("?resourceKey=")) {
            UUID datasetKey = retrieveDatasetKeyFromQueryParameters(request);
            return authorizeOrganizationDatasetChange(request, datasetKey);
          }
        }
      }
    }
    // otherwise return request unchanged
    return request;
  }

  private LegacyRequestAuthorization newAuthorization() {
    return new LegacyRequestAuthorization(httpContext, organizationService, datasetService, installationService);
  }

  /**
   * Authorize request can make a change to an organization, setting the request security context specifying the
   * principal provider. Called for example, when adding a new dataset.
   *
   * @param request request
   * @return request
   * @throws WebApplicationException if request isn't authorized
   */
  private ContainerRequest authorizeOrganizationChange(ContainerRequest request) throws WebApplicationException {
    LegacyRequestAuthorization authorization = newAuthorization();
    if (authorization.isAuthorizedToModifyOrganization()) {
      request.setSecurityContext(authorization);
    } else {
      LOG.error("Request to register not authorized!");
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    return request;
  }

  /**
   * Authorize request can make a change to an organization, first extracting the organization key from the
   * request path. If authorization is successful, the method sets the request security context specifying the
   * principal provider. Called for example, when verifying the credentials are correct for an organization.
   *
   * @param organizationKey organization key
   * @return request
   * @throws WebApplicationException if request isn't authorized
   */
  private ContainerRequest authorizeOrganizationChange(UUID organizationKey, ContainerRequest request)
    throws WebApplicationException {
    LegacyRequestAuthorization authorization = newAuthorization();
    if (authorization.isAuthorizedToModifyOrganization(organizationKey)) {
      request.setSecurityContext(authorization);
    } else {
      LOG.error("Request to register not authorized!");
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    return request;
  }

  /**
   * Authorize request can make a change to an organization's dataset, setting the request security context specifying
   * the principal provider. Called for example, when updating or deleting a dataset.
   *
   * @param request    request
   * @param datasetKey dataset key
   * @return request
   * @throws WebApplicationException if request isn't authorized
   */
  private ContainerRequest authorizeOrganizationDatasetChange(ContainerRequest request, UUID datasetKey)
    throws WebApplicationException {
    LegacyRequestAuthorization authorization = newAuthorization();
    if (authorization.isAuthorizedToModifyOrganizationsDataset(datasetKey)) {
      request.setSecurityContext(authorization);
    } else {
      LOG.error("Request to update Dataset not authorized!");
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    return request;
  }

  /**
   * Authorize request can make a change to an installation, setting the request security context specifying the
   * principal provider. Called for example, when adding a new dataset.
   *
   * @param request         request
   * @param installationKey installation key
   * @return request
   * @throws WebApplicationException if request isn't authorized
   */
  private ContainerRequest authorizeInstallationChange(ContainerRequest request, UUID installationKey) throws WebApplicationException {
    LegacyRequestAuthorization authorization = newAuthorization();
    if (authorization.isAuthorizedToModifyInstallation(installationKey)) {
      request.setSecurityContext(authorization);
    } else {
      LOG.error("Request to update IPT not authorized!");
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    return request;
  }

  /**
   * Retrieve key from request path, where the key is the last path segment, e.g. /registry/resource/{key}
   * Ensure any trailing .json for example is removed.
   *
   * @param request request
   * @return dataset key
   * @throws WebApplicationException if incoming string key isn't a valid UUID
   */
  private UUID retrieveKeyFromRequestPath(ContainerRequest request) throws WebApplicationException {
    String path = request.getPath();
    String key = path.substring(path.lastIndexOf("/") + 1);
    if (key.contains(".")) {
      key = key.substring(0, key.lastIndexOf("."));
    }
    try {
      return UUID.fromString(key);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
  }

  /**
   * Retrieve dataset key from form parameters.
   *
   * @param request request
   * @return dataset key
   * @throws WebApplicationException if incoming string key isn't a valid UUID
   */
  private UUID retrieveDatasetKeyFromFormParameters(ContainerRequest request) throws WebApplicationException {
    MultivaluedMap<String, String> params = request.getFormParameters();
    String key = params.getFirst(LegacyResourceConstants.RESOURCE_KEY_PARAM);
    try {
      return UUID.fromString(key);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
  }

  /**
   * Retrieve dataset key from query parameters.
   *
   * @param request request
   * @return dataset key
   * @throws WebApplicationException if incoming string key isn't a valid UUID
   */
  private UUID retrieveDatasetKeyFromQueryParameters(ContainerRequest request) throws WebApplicationException {
    MultivaluedMap<String, String> params = request.getQueryParameters();
    String key = params.getFirst(LegacyResourceConstants.RESOURCE_KEY_PARAM);
    try {
      return UUID.fromString(key);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
  }

  public static UUID extractOrgKeyFromSecurity(SecurityContext sec) {
    if (sec instanceof ContainerRequest) {
      // not sure why jersey does this, but its nested apparently
      sec = ((ContainerRequest) sec).getSecurityContext();
    }

    if (sec instanceof LegacyRequestAuthorization) {
      return ((LegacyRequestAuthorization) sec).getUserKey();
    }

    return null;
  }

}
