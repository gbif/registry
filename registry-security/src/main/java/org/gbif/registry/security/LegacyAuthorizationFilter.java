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
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

/**
 * A filter that will intercept legacy web service requests to /registry/* and perform
 * authentication setting a security context on the request.
 */
@Component
public class LegacyAuthorizationFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(LegacyAuthorizationFilter.class);

  private static final Splitter COLON_SPLITTER = Splitter.on(":").limit(2);
  private static final String BASIC_AUTH_HEADER = "Basic ";
  private static final String DOT = ".";
  private static final String SLASH = "/";
  private static final String GBRDS_REQUEST_INDICATOR = "registry/";
  private static final String DATASET_REQUEST_INDICATOR = "/resource";
  private static final String UPDATE_REQUEST_INDICATOR = "/update/";
  private static final String REGISTER_REQUEST_INDICATOR = "/register";
  private static final String ENDPOINT_REQUEST_INDICATOR = "/service";
  private static final String VALIDATION_REQUEST_INDICATOR = "/validation";
  private static final String INSTALLATION_REQUEST_INDICATOR = "/ipt";
  private static final String DATASET_UPDATE_OR_DELETE_REQUEST_INDICATOR = "/resource/";
  private static final String NETWORK_REQUEST_INDICATOR = "/network";
  private static final String IPT_AUTH_PREFIX = "IPT__";

  private final LegacyAuthorizationService legacyAuthorizationService;

  public LegacyAuthorizationFilter(LegacyAuthorizationService legacyAuthorizationService) {
    this.legacyAuthorizationService = legacyAuthorizationService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String path = request.getRequestURI().toLowerCase();

    if (isGbrdsRequest(path)) {
      if (isGetRequest(request)) {
        filterGetRequests(request);
      } else if (isPostPutDeleteRequest(request)) {
        filterPostPutDeleteRequests(request, path);
      }
    }

    if (isValidationRequest(request, path)) {
      handleValidationRequest(request);
    }

    // otherwise, just do nothing (request unchanged)
    filterChain.doFilter(request, response);
  }

  /**
   * Filter GET requests.
   *
   * @param httpRequest a request
   */
  private void filterGetRequests(HttpServletRequest httpRequest) {
    if (isOrganisationRequest(httpRequest)) {
      handleOrganisationRequest(httpRequest);
    }
  }

  /**
   * Filter POST, PUT and DELETE requests.
   *
   * @param httpRequest a request
   * @param path a request path
   */
  private void filterPostPutDeleteRequests(HttpServletRequest httpRequest, String path) {
    if (isInstallationRequest(path)) {
      handleInstallationRequest(httpRequest, path);
    } else if (isDatasetRequest(path)) {
      handleDatasetRequest(httpRequest, path);
    } else if (isEndpointRequest(path)) {
      handleEndpointRequest(httpRequest);
    }
  }

  /**
   * Handles dataset requests by determining the specific action based on the request path.
   * Supported operations include:
   *
   * <ul>
   *   <li><strong>Register a new dataset:</strong> requests like <code>/registry/ipt/resource</code></li>
   *   <li><strong>Add dataset to a network or remove from it:</strong> requests like <code>/registry/resource/{resourceKey}/network/{networkKey}</code></li>
   *   <li><strong>Update or delete a dataset:</strong> requests like <code>/registry/ipt/resource/{resourceKey}</code></li>
   * </ul>
   *
   * @param httpRequest the request
   * @param path the request path
   */
  private void handleDatasetRequest(HttpServletRequest httpRequest, String path) {
    if (isRegisterDatasetRequest(path)) {
      handleRegisterDatasetRequest(httpRequest);
    } else if (isAddDatasetToNetworkRequest(path)) {
      handleAddOrRemoveDatasetToNetworkRequest(httpRequest);
    } else if (isUpdateOrDeleteDatasetRequest(path)) {
      handleUpdateOrDeleteDatasetRequest(httpRequest);
    } else {
      LOG.warn("Unrecognized dataset request: {}", path);
    }
  }

  /**
   * Handles installation requests by determining the specific action based on the request path.
   * Supported operations include:
   *
   * <ul>
   *   <li><strong>Register a new installation:</strong> requests like <code>/registry/ipt/register</code></li>
   *   <li><strong>Update an existing installation:</strong> requests like <code>/registry/ipt/update/{iptKey}</code></li>
   * </ul>
   *
   * @param httpRequest the request
   * @param path the request path
   */
  private void handleInstallationRequest(HttpServletRequest httpRequest, String path) {
    if (isRegisterInstallationRequest(path)) {
      handleRegisterInstallationRequest(httpRequest);
    } else if (isUpdateInstallationRequest(path)) {
      handleUpdateInstallationRequest(httpRequest);
    } else {
      LOG.warn("Unrecognized installation request: {}", path);
    }
  }

  /**
   * Handles endpoint requests.
   *
   * @param httpRequest the request
   */
  private void handleEndpointRequest(HttpServletRequest httpRequest) {
    UUID datasetKey = retrieveDatasetKeyFromFormOrQueryParameters(httpRequest);
    authorizeOrganizationDatasetChange(httpRequest, datasetKey);
  }

  private void handleValidationRequest(HttpServletRequest httpRequest) {
    authorizeValidation(httpRequest);
  }

  /**
   * Handles <strong>register a new dataset</strong> requests like <code>/registry/ipt/resource</code>
   *
   * @param httpRequest a request
   */
  private void handleRegisterDatasetRequest(HttpServletRequest httpRequest) {
    authorizeOrganizationChange(httpRequest);
  }

  /**
   * Handles <strong>add dataset to a network or remove from it</strong> requests like
   * <code>/registry/resource/{resourceKey}/network/{networkKey}
   *
   * @param httpRequest a request
   */
  private void handleAddOrRemoveDatasetToNetworkRequest(HttpServletRequest httpRequest) {
    UUID datasetKey = retrieveKeyFromMiddleRequestPath(httpRequest);
    authorizeOrganizationDatasetChange(httpRequest, datasetKey);
  }

  /**
   * Handles <strong>update or delete a dataset</strong> requests like <code>/registry/ipt/resource/{resourceKey}
   *
   * @param httpRequest a request
   */
  private void handleUpdateOrDeleteDatasetRequest(HttpServletRequest httpRequest) {
    UUID datasetKey = retrieveKeyFromRequestPath(httpRequest);
    authorizeOrganizationDatasetChange(httpRequest, datasetKey);
  }

  /**
   * Handles <strong>register a new installation</strong> requests like <code>/registry/ipt/register</code>
   *
   * @param httpRequest a request
   */
  private void handleRegisterInstallationRequest(HttpServletRequest httpRequest) {
    authorizeOrganizationChange(httpRequest);
  }

  /**
   * Handles <strong>update an existing installation</strong> requests like <code>/registry/ipt/update/{iptKey}</code>
   *
   * @param httpRequest a request
   */
  private void handleUpdateInstallationRequest(HttpServletRequest httpRequest) {
    UUID installationKey = retrieveKeyFromRequestPath(httpRequest);
    authorizeInstallationChange(httpRequest, installationKey);
  }

  /**
   * Handle organization requests like <code>/registry/organisation/{organisationKey}?op=login</code>
   *
   * @param httpRequest a request
   */
  private void handleOrganisationRequest(HttpServletRequest httpRequest) {
    UUID organizationKey = retrieveKeyFromRequestPath(httpRequest);
    authorizeOrganizationChange(httpRequest, organizationKey);
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
   * Authorize a validation request.
   */
  private void authorizeValidation(HttpServletRequest request) {
    LegacyRequestAuthorization authorization = legacyAuthorizationService.authenticate(request);
    SecurityContextHolder.getContext().setAuthentication(authorization);
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
   * Retrieve key from request path, where the key is in the middle segment, e.g.
   * /registry/resource/{key}/network/{networkKey}
   *
   * @param request request
   * @return dataset key
   * @throws WebApplicationException if incoming string key isn't a valid UUID
   */
  private UUID retrieveKeyFromMiddleRequestPath(HttpServletRequest request) {
    String path = request.getRequestURI();
    String key =
        path.substring(
            path.lastIndexOf(DATASET_UPDATE_OR_DELETE_REQUEST_INDICATOR)
                + DATASET_UPDATE_OR_DELETE_REQUEST_INDICATOR.length());

    if (key.contains(SLASH)) {
      key = key.substring(0, key.indexOf(SLASH));
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

  /**
   * Is it "Legacy" GBRDS API request from the IPT?
   * Check whether the request contains "registry/"
   *
   * @param path a request path
   * @return true or false
   */
  private boolean isGbrdsRequest(String path) {
    return path.contains(GBRDS_REQUEST_INDICATOR);
  }

  /**
   * Is it GET request?
   *
   * @param request a request
   * @return true or false
   */
  private boolean isGetRequest(HttpServletRequest request) {
    return StringUtils.equalsIgnoreCase(request.getMethod(), "GET");
  }

  /**
   * Is it POST, PUT or DELETE request?
   *
   * @param request a request
   * @return true or false
   */
  private boolean isPostPutDeleteRequest(HttpServletRequest request) {
    return StringUtils.equalsAnyIgnoreCase(request.getMethod(), "POST", "PUT", "DELETE");
  }

  private boolean isEndpointRequest(String path) {
    return path.endsWith(ENDPOINT_REQUEST_INDICATOR);
  }

  private boolean isValidationRequest(HttpServletRequest request, String path) {
    String authentication = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (Strings.isNullOrEmpty(authentication) || !authentication.startsWith(BASIC_AUTH_HEADER)) {
      return false;
    }

    // should contain /validation in the path
    if (!path.contains(VALIDATION_REQUEST_INDICATOR)) {
      return false;
    }

    byte[] decryptedBytes = Base64.getDecoder().decode(authentication.substring(BASIC_AUTH_HEADER.length()));
    String decrypted = new String(decryptedBytes);
    Iterator<String> iter = COLON_SPLITTER.split(decrypted).iterator();
    String rawUsername = iter.next();

    // should contain "IPT__" prefix in the username
    return rawUsername.startsWith(IPT_AUTH_PREFIX);
  }

  private boolean isDatasetRequest(String path) {
    return path.contains(DATASET_REQUEST_INDICATOR);
  }

  private boolean isInstallationRequest(String path) {
    return path.contains(INSTALLATION_REQUEST_INDICATOR) && !path.contains(DATASET_REQUEST_INDICATOR);
  }

  private boolean isUpdateInstallationRequest(String path) {
    return path.contains(UPDATE_REQUEST_INDICATOR);
  }

  private boolean isRegisterInstallationRequest(String path) {
    return path.endsWith(REGISTER_REQUEST_INDICATOR);
  }

  private boolean isRegisterDatasetRequest(String path) {
    return path.endsWith(DATASET_REQUEST_INDICATOR);
  }

  private boolean isAddDatasetToNetworkRequest(String path) {
    return path.contains(NETWORK_REQUEST_INDICATOR);
  }

  private boolean isUpdateOrDeleteDatasetRequest(String path) {
    return path.contains(DATASET_UPDATE_OR_DELETE_REQUEST_INDICATOR);
  }

  /**
   * Whether request contains param op=login
   *
   * @param httpRequest a request
   * @return true or false
   */
  private boolean isOrganisationRequest(HttpServletRequest httpRequest) {
    return CommonWsUtils.getFirst(httpRequest.getParameterMap(), "op") != null
        && CommonWsUtils.getFirst(httpRequest.getParameterMap(), "op").equalsIgnoreCase("login");
  }
}
