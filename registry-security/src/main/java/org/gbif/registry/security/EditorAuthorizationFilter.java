/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Organization;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.server.GbifHttpServletRequestWrapper;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.gbif.registry.security.SecurityContextCheck.checkIsNotAdmin;
import static org.gbif.registry.security.SecurityContextCheck.checkIsNotApp;
import static org.gbif.registry.security.SecurityContextCheck.checkIsNotEditor;
import static org.gbif.registry.security.SecurityContextCheck.ensureUserSetInSecurityContext;

/**
 * For requests authenticated with a REGISTRY_EDITOR role two levels of authorization need to be
 * passed. First of all any resource method is required to have the role included in the Secured
 * annotation. Secondly this request filter needs to be passed for POST/PUT/DELETE/GET requests that
 * act on existing and UUID identified main registry entities such as dataset, organization, node,
 * installation and network.
 *
 * <p>In order to do authorization the key of these entities is extracted from the requested path or
 * from the request body.
 *
 * <p>NOTE: Request path patterns do not expect query parameters. So in case of adding new paths
 * please make sure they work properly with or without query parameters!
 *
 * <p>NOTE that this filter should be in sync with {@link
 * org.gbif.registry.security.precheck.AuthPreCheckCreationRequestFilter}.
 */
@SuppressWarnings("NullableProblems")
@Component
public class EditorAuthorizationFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(EditorAuthorizationFilter.class);

  // filtered GET methods
  public static final List<Pattern> GET_RESOURCES_TO_FILTER = Collections.singletonList(
      Pattern.compile("^GET /(organization)/([a-f0-9-]+)/password$", Pattern.CASE_INSENSITIVE));

  // filtered POST methods
  public static final List<Pattern> POST_RESOURCES_TO_FILTER = Arrays.asList(
      Pattern.compile("^POST /(pipelines)/history/run/([a-f0-9-]+)$", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^POST /(pipelines)/history/run/([a-f0-9-]+)/[0-9]+$", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^POST /(dataset)/([a-f0-9-]+)/document$", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^POST /(network)/([a-f0-9-]+)/constituents/[a-f0-9-]+$", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^POST /(organization|dataset|installation|node|network)$", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^POST /(organization|dataset|installation|node|network)/([a-f0-9-]+)/comment$", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^POST /(organization|dataset|installation|node|network)/([a-f0-9-]+)/tag$", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^POST /(organization|dataset|installation|node|network)/([a-f0-9-]+)/machineTag$", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^POST /(organization|dataset|installation|node|network)/([a-f0-9-]+)/contact$", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^POST /(organization|dataset|installation|node|network)/([a-f0-9-]+)/endpoint$", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^POST /(organization|dataset|installation|node|network)/([a-f0-9-]+)/identifier$", Pattern.CASE_INSENSITIVE));

  // filtered PUT methods
  public static final List<Pattern> PUT_RESOURCES_TO_FILTER = Arrays.asList(
      Pattern.compile("^PUT /(organization)/([a-f0-9-]+)/endorsement$", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^PUT /(organization|dataset|installation|node|network)$", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^PUT /(organization|dataset|installation|node|network)/([a-f0-9-]+)$", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^PUT /(organization|dataset|installation|node|network)/([a-f0-9-]+)/contact$", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^PUT /(organization|dataset|installation|node|network)/([a-f0-9-]+)/contact/[0-9]+$", Pattern.CASE_INSENSITIVE));

  // filtered DELETE methods
  public static final List<Pattern> DELETE_RESOURCES_TO_FILTER = Arrays.asList(
      Pattern.compile("^DELETE /(organization)/([a-f0-9-]+)/endorsement$", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^DELETE /(network)/([a-f0-9-]+)/constituents/[a-f0-9-]+$", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^DELETE /(organization|dataset|installation|node|network)/([a-f0-9-]+)$", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^DELETE /(organization|dataset|installation|node|network)/([a-f0-9-]+)/comment/[0-9]+$", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^DELETE /(organization|dataset|installation|node|network)/([a-f0-9-]+)/tag/[0-9]+$", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^DELETE /(organization|dataset|installation|node|network)/([a-f0-9-]+)/machineTag/([0-9]+)$", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^DELETE /(organization|dataset|installation|node|network)/([a-f0-9-]+)/contact/[0-9]+$", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^DELETE /(organization|dataset|installation|node|network)/([a-f0-9-]+)/endpoint/[0-9]+$", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^DELETE /(organization|dataset|installation|node|network)/([a-f0-9-]+)/identifier/[0-9]+$", Pattern.CASE_INSENSITIVE));

  public static final String PIPELINES = "pipelines";
  public static final String MACHINE_TAG = "machineTag";
  public static final String ORGANIZATION = "organization";
  public static final String DATASET = "dataset";
  public static final String INSTALLATION = "installation";
  public static final String NODE = "node";
  public static final String NETWORK = "network";
  public static final List<String> NETWORK_ENTITIES = Arrays.asList(
      "organization", "dataset", "installation", "node", "network");

  private final EditorAuthorizationService userAuthService;
  private final AuthenticationFacade authenticationFacade;
  private final ObjectMapper objectMapper;

  public EditorAuthorizationFilter(
      EditorAuthorizationService userAuthService,
      AuthenticationFacade authenticationFacade,
      @Qualifier("registryObjectMapper") ObjectMapper objectMapper) {
    this.userAuthService = userAuthService;
    this.authenticationFacade = authenticationFacade;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    // only verify non OPTIONS methods with an authenticated REGISTRY_EDITOR
    // all other roles are taken care by simple 'Secured' or JSR250 annotations
    // on the resource methods
    final Authentication authentication = authenticationFacade.getAuthentication();
    final String path = request.getRequestURI();

    // method + path
    // e.g. POST /organization/8d453ca7-553f-40d2-8054-82708dc354f6/comment
    final String requestString = request.getMethod() + " " + path;
    LOG.debug("Editor auth filter request {}", requestString);

    // try to find a proper matcher for the request
    Optional<Matcher> pathMatcherOpt =
        getResourcesListToFilterByMethodType(request.getMethod())
            .stream()
            .map(p -> p.matcher(requestString))
            .filter(Matcher::matches)
            .findFirst();

    // skip OPTIONS requests
    // if the request path matches a pattern, check editor rights
    if (isNotOptionsRequest(request) && pathMatcherOpt.isPresent()) {
      // user must NOT be null if the resource requires editor rights restrictions
      ensureUserSetInSecurityContext(authentication, HttpStatus.FORBIDDEN);
      final String username = authentication.getName();
      LOG.debug("Context user: {}", username);

      // validate only if user not admin and not app
      if (checkIsNotAdmin(authentication) && checkIsNotApp(authentication)) {
        // only editors allowed to modify, because admins already excluded
        if (checkIsNotEditor(authentication)) {
          throw new WebApplicationException("User has no editor rights", HttpStatus.FORBIDDEN);
        }
        ensureRequest(username, path, pathMatcherOpt.get(), request);
      }
    }

    // request does not match, skip checks
    filterChain.doFilter(request, response);
  }

  /**
   * Returns list of patterns for the corresponding method.
   *
   * @param method HTTP method (GET, POST etc.)
   * @return list of patterns for the corresponding method
   */
  private List<Pattern> getResourcesListToFilterByMethodType(String method) {
    switch (method) {
      case "GET":
        return GET_RESOURCES_TO_FILTER;
      case "POST":
        return POST_RESOURCES_TO_FILTER;
      case "PUT":
        return PUT_RESOURCES_TO_FILTER;
      case "DELETE":
        return DELETE_RESOURCES_TO_FILTER;
      default:
        return Collections.emptyList();
    }
  }

  /**
   * Ensure request is allowed for the user.
   * If so do nothing, if not throw {@link WebApplicationException}.
   *
   * @param username username
   * @param path     request path
   * @param matcher  matcher which contains the match between request path and pattern
   * @param request  request
   */
  private void ensureRequest(String username, String path, Matcher matcher, HttpServletRequest request) {
    int groupCount = matcher.groupCount();
    LOG.debug("Matcher groups: {}", groupCount);
    String resourceName; // resource name (e.g. dataset)
    String resourceKey = null; // resource key (id of the primary resource)
    String subKey = null; // sub resource key (e.g. id of the identifier)

    // one group - requests like POST /organization - no entity key
    if (groupCount == 1) {
      resourceName = matcher.group(1);
      // two groups - requests like PUT /dataset/{key} - with entity key
    } else if (groupCount == 2) {
      resourceName = matcher.group(1);
      resourceKey = matcher.group(2);
      // three groups - machine tag DELETE requests
    } else if (groupCount == 3) {
      resourceName = matcher.group(1);
      resourceKey = matcher.group(2);
      subKey = matcher.group(3);
    } else {
      LOG.error("Unexpected exception. Something wrong with request: user {}, path {}",
          username, path);
      throw new WebApplicationException(
          MessageFormat.format(
              "Unexpected exception. Something wrong with request: user {0}, path {1}",
              username, path),
          HttpStatus.BAD_REQUEST);
    }
    LOG.debug("Extracted from the request path: resource name [{}] and resource key [{}]", resourceName, resourceKey);

    // pipelines requests
    if (PIPELINES.equals(resourceName)) {
      ensurePipelinesRunRequest(username, resourceKey);
    }
    // machine tag requests
    else if (path.contains(MACHINE_TAG)) {
      if (subKey != null) {
        ensureMachineTagRequestWithKey(resourceName, username, resourceKey, subKey);
      } else {
        ensureMachineTagRequestWithoutKey(resourceName, username, resourceKey, request);
      }
    }
    // network entities requests
    else if (NETWORK_ENTITIES.contains(resourceName)) {
      if (resourceKey != null) {
        ensureNetworkEntityRequestWithKey(resourceName, username, resourceKey);
      } else {
        ensureNetworkEntityRequestWithoutKey(resourceName, username, request);
      }
    } else {
      LOG.error("Unexpected exception. Something wrong with the request: user {}, path {}",
          username, path);
      throw new WebApplicationException(
          MessageFormat.format(
              "Unexpected exception. Something wrong with the request: user {0}, path {1}",
              username, path),
          HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Ensure machine tag request is allowed for the user.
   * If so do nothing, if not throw {@link WebApplicationException}.
   * No key present in the path so this tries to deserialize body.
   *
   * @param entityName  network entity name (e.g. dataset, organization)
   * @param username    username
   * @param resourceKey network entity key
   * @param request     tag key or namespace
   */
  private void ensureMachineTagRequestWithoutKey(
      String entityName, String username, String resourceKey, HttpServletRequest request) {
    if (resourceKey == null || isNotUuid(resourceKey)) {
      // invalid request, it fails later
      LOG.debug("Invalid machine tag request without key. entityName [{}], username [{}], resourceKey [{}]",
          entityName, username, resourceKey);
      return;
    }

    try {
      MachineTag machineTag =
          objectMapper.readValue(
              ((GbifHttpServletRequestWrapper) request).getContent(), MachineTag.class);

      if (!userAuthService.allowedToCreateMachineTag(username, UUID.fromString(resourceKey), machineTag)) {
        LOG.warn("User {} is not allowed to add machine tags to {} {}", username, entityName, resourceKey);
        throw new WebApplicationException(
            MessageFormat.format(
                "User {0} is not allowed to add machine tags to the {1} {2}", username, entityName, resourceKey),
            HttpStatus.FORBIDDEN);
      } else {
        LOG.debug("User {} is allowed to add machine tags to {} {}", username, entityName, resourceKey);
      }
    } catch (JsonProcessingException e) {
      LOG.error("Failed to deserialize JSON", e);
      throw new WebApplicationException("Failed to deserialize JSON", HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Ensure machine tag request is allowed for the user.
   * If so do nothing, if not throw {@link WebApplicationException}.
   * Uses machineTagKey.
   *
   * @param entityName    network entity name (e.g. dataset, organization)
   * @param username      username
   * @param resourceKey   network entity key
   * @param machineTagKey the machine tag key
   */
  private void ensureMachineTagRequestWithKey(String entityName, String username, String resourceKey, String machineTagKey) {
    if (resourceKey == null || isNotUuid(resourceKey) || isNotInt(machineTagKey)) {
      // invalid request, it fails later
      LOG.debug("Invalid machine tag request with key. entityName [{}], username [{}], resourceKey [{}], machineTagKey [{}]",
          entityName, username, resourceKey, machineTagKey);
      return;
    }

    if (!userAuthService.allowedToDeleteMachineTag(username, UUID.fromString(resourceKey), Integer.parseInt(machineTagKey))) {
      LOG.warn("User {} is not allowed to delete machine tags from {} {}", username, entityName, resourceKey);
      throw new WebApplicationException(
          MessageFormat.format(
              "User {0} is not allowed to delete machine tags from {1} {2}", username, entityName, resourceKey),
          HttpStatus.FORBIDDEN);
    }

    LOG.debug("User {} is allowed to delete machine tags from {} {}", username, entityName, resourceKey);
  }

  /**
   * Ensure pipelines request is allowed for the user.
   * If so do nothing, if not throw {@link WebApplicationException}.
   *
   * @param username    username
   * @param resourceKey resourceKey
   */
  private void ensurePipelinesRunRequest(String username, String resourceKey) {
    if (resourceKey == null || isNotUuid(resourceKey)) {
      // invalid request, it fails later
      LOG.debug("Invalid pipelines request. username [{}], resourceKey [{}]", username, resourceKey);
      return;
    }

    if (!userAuthService.allowedToModifyDataset(username, UUID.fromString(resourceKey))) {
      LOG.warn("User {} is not allowed to run pipelines for the dataset {}", username, resourceKey);
      throw new WebApplicationException(
          MessageFormat.format(
              "User {0} is not allowed to run pipelines for the dataset {1}", username, resourceKey),
          HttpStatus.FORBIDDEN);
    } else {
      LOG.debug("User {} is allowed to run pipelines for the dataset {}", username, resourceKey);
    }
  }

  /**
   * Ensure network entity request (without resource key) is allowed for the user.
   * If so do nothing, if not throw {@link WebApplicationException}.
   *
   * @param entityName network entity name (e.g. dataset, organization)
   * @param username   username
   * @param request    HTTP request
   */
  private void ensureNetworkEntityRequestWithoutKey(String entityName, String username, HttpServletRequest request) {
    NetworkEntity entity;
    try {
      BiFunction<String, NetworkEntity, Boolean> checkAllowedToModifyEntity;
      switch (entityName) {
        case DATASET:
          entity =
              objectMapper.readValue(
                  ((GbifHttpServletRequestWrapper) request).getContent(), Dataset.class);
          checkAllowedToModifyEntity = (name, e) -> userAuthService.allowedToModifyDataset(name, ((Dataset) e));
          break;
        case ORGANIZATION:
          entity =
              objectMapper.readValue(
                  ((GbifHttpServletRequestWrapper) request).getContent(), Organization.class);
          checkAllowedToModifyEntity = (name, e) -> userAuthService.allowedToModifyOrganization(name, ((Organization) e));
          break;
        case INSTALLATION:
          entity =
              objectMapper.readValue(
                  ((GbifHttpServletRequestWrapper) request).getContent(), Installation.class);
          checkAllowedToModifyEntity = (name, e) -> userAuthService.allowedToModifyInstallation(name, ((Installation) e));
          break;
        case NETWORK:
        case NODE:
          LOG.warn("User {} is not allowed to create {}", username, entityName);
          throw new WebApplicationException(
              MessageFormat.format("User {0} is not allowed to create {1}", username, entityName),
              HttpStatus.FORBIDDEN);
        default:
          LOG.error("Unexpected network entity. entity name: {}", entityName);
          throw new WebApplicationException(
              MessageFormat.format(
                  "Unexpected network entity. entity name: {0}", entityName),
              HttpStatus.BAD_REQUEST);
      }

      if (!checkAllowedToModifyEntity.apply(username, entity)) {
        LOG.warn("User {} is not allowed to create or modify {} {}",
            username, entityName, entity.getKey() != null ? entity.getKey() : "");
        throw new WebApplicationException(
            MessageFormat.format(
                "User {0} is not allowed to create or modify {1} {2}",
                username, entityName, entity.getKey() != null ? entity.getKey() : ""),
            HttpStatus.FORBIDDEN);
      } else {
        LOG.debug("User {} is allowed to modify {} {}", username, entityName, entity.getKey());
      }
    } catch (JsonProcessingException e) {
      LOG.error("Failed to deserialize JSON", e);
      throw new WebApplicationException("Failed to deserialize JSON", HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Ensure network entity request (with resource key) is allowed for the user.
   * If so do nothing, if not throw {@link WebApplicationException}.
   *
   * @param entityName  network entity name (e.g. dataset, organization)
   * @param username    username
   * @param resourceKey network entity key
   */
  private void ensureNetworkEntityRequestWithKey(String entityName, String username, String resourceKey) {
    if (isNotUuid(resourceKey)) {
      // invalid request, it fails later
      LOG.debug("Invalid network entity request with key. entityName [{}], username [{}], resourceKey [{}]",
          entityName, username, resourceKey);
      return;
    }

    BiFunction<String, UUID, Boolean> checkAllowedToModifyEntity;
    switch (entityName) {
      case DATASET:
        checkAllowedToModifyEntity = userAuthService::allowedToModifyDataset;
        break;
      case ORGANIZATION:
        checkAllowedToModifyEntity = userAuthService::allowedToModifyOrganization;
        break;
      case INSTALLATION:
        checkAllowedToModifyEntity = userAuthService::allowedToModifyInstallation;
        break;
      case NETWORK:
      case NODE:
        checkAllowedToModifyEntity = userAuthService::allowedToModifyEntity;
        break;
      default:
        LOG.error("Unexpected network entity. entity name: {}", entityName);
        throw new WebApplicationException(
            MessageFormat.format(
                "Unexpected network entity. entity name: {0}", entityName),
            HttpStatus.BAD_REQUEST);
    }

    if (!checkAllowedToModifyEntity.apply(username, UUID.fromString(resourceKey))) {
      LOG.warn("User {} is not allowed to modify {} {}", username, entityName, resourceKey);
      throw new WebApplicationException(
          MessageFormat.format(
              "User {0} is not allowed to modify {1} {2}", username, entityName, resourceKey),
          HttpStatus.FORBIDDEN);
    } else {
      LOG.debug("User {} is allowed to modify {} {}", username, entityName, resourceKey);
    }
  }

  /**
   * Check if request is OPTIONS
   *
   * @param httpRequest HTTP request
   * @return true - request is OPTIONS, false otherwise
   */
  private boolean isNotOptionsRequest(HttpServletRequest httpRequest) {
    return !"OPTIONS".equals(httpRequest.getMethod());
  }

  /**
   * Check if string is NOT UUID
   *
   * @param str string to check
   * @return true - string is NOT UUID, false otherwise
   */
  @SuppressWarnings("ResultOfMethodCallIgnored")
  private boolean isNotUuid(String str) {
    try {
      UUID.fromString(str);
      return false;
    } catch (IllegalArgumentException e) {
      return true;
    }
  }

  /**
   * Check if string is NOT int
   *
   * @param str string to check
   * @return true - string is int, false otherwise
   */
  private boolean isNotInt(String str) {
    try {
      Integer.parseInt(str);
      return false;
    } catch (NumberFormatException er) {
      return true;
    }
  }
}
