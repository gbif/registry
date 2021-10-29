/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.security;

import org.gbif.registry.domain.ws.util.LegacyResourceConstants;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.security.LegacyRequestAuthorization;
import org.gbif.ws.util.CommonWsUtils;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A filter that will intercept legacy web service requests to /registry/* and perform
 * authentication setting a security context on the request.
 */
@Component
public class LegacyAuthorizationFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(LegacyAuthorizationFilter.class);

  private static final String DOT = ".";
  private static final String SLASH = "/";
  private static final String REGISTRY_END_SIDE_SLASH_MAPPING = "registry/";
  private static final String RESOURCE_MAPPING = "/resource";
  private static final String UPDATE_BOTH_SIDE_SLASH_MAPPING = "/update/";
  private static final String REGISTER_MAPPING = "/register";
  private static final String SERVICE_MAPPING = "/service";
  private static final String IPT_MAPPING = "/ipt";
  private static final String RESOURCE_BOTH_SIDE_SLASH_MAPPING = "/resource/";

  private final LegacyAuthorizationService legacyAuthorizationService;

  public LegacyAuthorizationFilter(LegacyAuthorizationService legacyAuthorizationService) {
    this.legacyAuthorizationService = legacyAuthorizationService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String path = request.getRequestURI().toLowerCase();

    // is it a legacy web service request?
    if (path.contains(REGISTRY_END_SIDE_SLASH_MAPPING)) {
      if ("GET".equalsIgnoreCase(request.getMethod())) {
        filterGetRequests(request);
      } else if ("POST".equalsIgnoreCase(request.getMethod())
          || "PUT".equalsIgnoreCase(request.getMethod())
          || "DELETE".equalsIgnoreCase(request.getMethod())) {
        filterPostPutDeleteRequests(request, path);
      }
    }
    // otherwise just do nothing (request unchanged)
    filterChain.doFilter(request, response);
  }

  private void filterPostPutDeleteRequests(HttpServletRequest httpRequest, String path) {
    // legacy installation request
    if (path.contains(IPT_MAPPING)) {
      // register installation?
      if (path.endsWith(REGISTER_MAPPING)) {
        authorizeOrganizationChange(httpRequest);
      }
      // update installation?
      else if (path.contains(UPDATE_BOTH_SIDE_SLASH_MAPPING)) {
        UUID installationKey = retrieveKeyFromRequestPath(httpRequest);
        authorizeInstallationChange(httpRequest, installationKey);
      }
      // register dataset?
      else if (path.endsWith(RESOURCE_MAPPING)) {
        authorizeOrganizationChange(httpRequest);
      }
      // update dataset, delete dataset?
      else if (path.contains(RESOURCE_BOTH_SIDE_SLASH_MAPPING)) {
        UUID datasetKey = retrieveKeyFromRequestPath(httpRequest);
        authorizeOrganizationDatasetChange(httpRequest, datasetKey);
      }
    }
    // legacy dataset request
    else if (path.contains(RESOURCE_MAPPING)) {
      // register dataset?
      if (path.endsWith(RESOURCE_MAPPING)) {
        authorizeOrganizationChange(httpRequest);
      }
      // update dataset, delete dataset?
      else if (path.contains(RESOURCE_BOTH_SIDE_SLASH_MAPPING)) {
        UUID datasetKey = retrieveKeyFromRequestPath(httpRequest);
        authorizeOrganizationDatasetChange(httpRequest, datasetKey);
      }
    }
    // legacy endpoint request, add endpoint?
    else if (path.endsWith(SERVICE_MAPPING)) {
      UUID datasetKey = retrieveDatasetKeyFromFormOrQueryParameters(httpRequest);
      authorizeOrganizationDatasetChange(httpRequest, datasetKey);
    }
  }

  private void filterGetRequests(HttpServletRequest httpRequest) {
    // E.g. validate organization request, identified by param op=login
    if (CommonWsUtils.getFirst(httpRequest.getParameterMap(), "op") != null
        && CommonWsUtils.getFirst(httpRequest.getParameterMap(), "op").equalsIgnoreCase("login")) {
      UUID organizationKey = retrieveKeyFromRequestPath(httpRequest);
      authorizeOrganizationChange(httpRequest, organizationKey);
    }
  }

  /**
   * Authorize request can make a change to an organization, setting the request security context
   * specifying the principal provider. Called for example, when adding a new dataset.
   *
   * @throws WebApplicationException if request isn't authorized
   */
  private void authorizeOrganizationChange(HttpServletRequest request) {
    LegacyRequestAuthorization authorization = legacyAuthorizationService.authenticate(request);
    if (legacyAuthorizationService.isAuthorizedToModifyOrganization(authorization)) {
      SecurityContextHolder.getContext().setAuthentication(authorization);
    } else {
      LOG.error("Request to register not authorized!");
      throw new WebApplicationException(
          "Request to register not authorized", HttpStatus.UNAUTHORIZED);
    }
  }

  /**
   * Authorize request can make a change to an organization, first extracting the organization key
   * from the request path. If authorization is successful, the method sets the request security
   * context specifying the principal provider. Called for example, when verifying the credentials
   * are correct for an organization.
   *
   * @param organizationKey organization key
   * @throws WebApplicationException if request isn't authorized
   */
  private void authorizeOrganizationChange(HttpServletRequest request, UUID organizationKey) {
    LegacyRequestAuthorization authorization = legacyAuthorizationService.authenticate(request);
    if (legacyAuthorizationService.isAuthorizedToModifyOrganization(
        authorization, organizationKey)) {
      SecurityContextHolder.getContext().setAuthentication(authorization);
    } else {
      LOG.error("Request to register not authorized!");
      throw new WebApplicationException(
          "Request to register not authorized", HttpStatus.UNAUTHORIZED);
    }
  }

  /**
   * Authorize request can make a change to an organization's dataset, setting the request security
   * context specifying the principal provider. Called for example, when updating or deleting a
   * dataset.
   *
   * @param datasetKey dataset key
   * @throws WebApplicationException if request isn't authorized
   */
  private void authorizeOrganizationDatasetChange(HttpServletRequest request, UUID datasetKey) {
    LegacyRequestAuthorization authorization = legacyAuthorizationService.authenticate(request);
    if (legacyAuthorizationService.isAuthorizedToModifyOrganizationsDataset(
        authorization, datasetKey)) {
      SecurityContextHolder.getContext().setAuthentication(authorization);
    } else {
      LOG.error("Request to update Dataset not authorized!");
      throw new WebApplicationException(
          "Request to update Dataset not authorized", HttpStatus.UNAUTHORIZED);
    }
  }

  /**
   * Authorize request can make a change to an installation, setting the request security context
   * specifying the principal provider. Called for example, when adding a new dataset.
   *
   * @param installationKey installation key
   * @throws WebApplicationException if request isn't authorized
   */
  private void authorizeInstallationChange(HttpServletRequest request, UUID installationKey) {
    LegacyRequestAuthorization authorization = legacyAuthorizationService.authenticate(request);
    if (legacyAuthorizationService.isAuthorizedToModifyInstallation(
        authorization, installationKey)) {
      SecurityContextHolder.getContext().setAuthentication(authorization);
    } else {
      LOG.error("Request to update IPT not authorized!");
      throw new WebApplicationException(
          "Request to update IPT not authorized", HttpStatus.UNAUTHORIZED);
    }
  }

  /**
   * Retrieve key from request path, where the key is the last path segment, e.g.
   * /registry/resource/{key} Ensure any trailing .json for example is removed.
   *
   * @param request request
   * @return dataset key
   * @throws WebApplicationException if incoming string key isn't a valid UUID
   */
  private UUID retrieveKeyFromRequestPath(HttpServletRequest request) {
    String path = request.getRequestURI();
    String key = path.substring(path.lastIndexOf(SLASH) + 1);
    if (key.contains(DOT)) {
      key = key.substring(0, key.lastIndexOf(DOT));
    }
    try {
      return UUID.fromString(key);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(
          "Key is not present it the request", HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Retrieve dataset key from form or query parameters.
   *
   * @param request request
   * @return dataset key
   * @throws WebApplicationException if incoming string key isn't a valid UUID
   */
  private UUID retrieveDatasetKeyFromFormOrQueryParameters(HttpServletRequest request) {
    Map<String, String[]> params = request.getParameterMap();
    String key = CommonWsUtils.getFirst(params, LegacyResourceConstants.RESOURCE_KEY_PARAM);
    try {
      if (key != null) {
        return UUID.fromString(key);
      } else {
        throw new IllegalArgumentException("Key is not present in parameters!");
      }
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(
          "Dataset key is not present in the parameters", HttpStatus.BAD_REQUEST);
    }
  }
}
