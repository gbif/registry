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
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Organization;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.server.GbifHttpServletRequestWrapper;

import java.io.IOException;
import java.text.MessageFormat;
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
 * passed. First of all any resource method is required to have the role included in the Secured or
 * RolesAllowed annotation. Secondly this request filter needs to be passed for POST/PUT/DELETE
 * requests that act on existing and UUID identified main registry entities such as dataset,
 * organization, node, installation and network.
 *
 * <p>In order to do authorization the key of these entities is extracted from the requested path.
 * An exception to this is the create method for those main entities themselves. This is covered by
 * the BaseNetworkEntityResource.create() method directly.
 */
@SuppressWarnings("NullableProblems")
@Component
public class EditorAuthorizationFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(EditorAuthorizationFilter.class);

  // for POST requests which does not contain key
  private static final Pattern NODE_PATTERN_CREATE = Pattern.compile("^/node$");
  private static final Pattern NETWORK_PATTERN_CREATE = Pattern.compile("^/network$");
  private static final Pattern ORGANIZATION_PATTERN_CREATE = Pattern.compile("^/organization$");
  private static final Pattern INSTALLATION_PATTERN_CREATE = Pattern.compile("^/installation$");
  private static final Pattern DATASET_PATTERN_CREATE = Pattern.compile("^/dataset$");

  // for PUT and DELETE requests which contains key
  private static final Pattern NODE_PATTERN_UPDATE_DELETE =
      Pattern.compile("^/?node/([a-f0-9-]+).*");
  private static final Pattern NETWORK_PATTERN_UPDATE_DELETE =
      Pattern.compile("^/?network/([a-f0-9-]+).*");
  private static final Pattern ORGANIZATION_PATTERN_UPDATE_DELETE =
      Pattern.compile("^/?organization/([a-f0-9-]+).*");
  private static final Pattern INSTALLATION_PATTERN_UPDATE_DELETE =
      Pattern.compile("^/?installation/([a-f0-9-]+).*");
  private static final Pattern DATASET_PATTERN_UPDATE_DELETE =
      Pattern.compile("^/?dataset/([a-f0-9-]+).*");

  // for POST requests add constituent to network
  private static final Pattern NETWORK_PATTERN_CONSTITUENT_CREATE =
      Pattern.compile("^/network/([a-f0-9-]+).*/constituents/([a-f0-9-]+).*$");

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
    // only verify non GET methods with an authenticated REGISTRY_EDITOR
    // all other roles are taken care by simple 'Secured' or JSR250 annotations on the resource
    // methods
    final Authentication authentication = authenticationFacade.getAuthentication();
    final String path = request.getRequestURI().toLowerCase();

    // skip GET and OPTIONS requests
    if (isNotGetOrOptionsRequest(request) && checkRequestRequiresEditorValidation(path)) {
      // user must NOT be null if the resource requires editor rights restrictions
      ensureUserSetInSecurityContext(authentication, HttpStatus.FORBIDDEN);
      final String username = authentication.getName();

      // validate only if user not admin and not app
      if (checkIsNotAdmin(authentication) && checkIsNotApp(authentication)) {
        // only editors allowed to modify, because admins already excluded
        if (checkIsNotEditor(authentication)) {
          throw new WebApplicationException("User has no editor rights", HttpStatus.FORBIDDEN);
        }
        try {
          if ("POST".equals(request.getMethod())) { // POST
            ensureCreateRequest(username, request);
          } else { // PUT or DELETE
            ensureUpdateDeleteRequest(username, path);
          }
        } catch (IllegalArgumentException e) {
          LOG.warn("Invalid request: {}", e.getMessage());
        }
      }
    }
    filterChain.doFilter(request, response);
  }

  private void ensureCreateRequest(String username, HttpServletRequest request) {
    String path = request.getRequestURI().toLowerCase();
    Matcher matcher;
    if (DATASET_PATTERN_CREATE.matcher(path).matches()) {
      Dataset entity = null;
      try {
        entity =
            objectMapper.readValue(
                ((GbifHttpServletRequestWrapper) request).getContent(), Dataset.class);
      } catch (JsonProcessingException e) {
        LOG.error("Error processing json", e);
      }
      ensureNetworkEntity("dataset", entity, username, userAuthService::allowedToModifyDataset);
    } else if (INSTALLATION_PATTERN_CREATE.matcher(path).matches()) {
      Installation entity = null;
      try {
        entity =
            objectMapper.readValue(
                ((GbifHttpServletRequestWrapper) request).getContent(), Installation.class);
      } catch (JsonProcessingException e) {
        LOG.error("Error processing json", e);
      }
      ensureNetworkEntity(
          "installation", entity, username, userAuthService::allowedToModifyInstallation);
    } else if (ORGANIZATION_PATTERN_CREATE.matcher(path).matches()) {
      Organization entity = null;
      try {
        entity =
            objectMapper.readValue(
                ((GbifHttpServletRequestWrapper) request).getContent(), Organization.class);
      } catch (JsonProcessingException e) {
        LOG.error("Error processing json", e);
      }
      ensureNetworkEntity(
          "organization", entity, username, userAuthService::allowedToModifyOrganization);
    } else if (NODE_PATTERN_CREATE.matcher(path).matches()) {
      LOG.warn("User {} is not allowed to create nodes", username);
      throw new WebApplicationException(
          MessageFormat.format("User {0} is not allowed to create nodes", username),
          HttpStatus.FORBIDDEN);
    } else if (NETWORK_PATTERN_CREATE.matcher(path).matches()) {
      LOG.warn("User {} is not allowed to create networks", username);
      throw new WebApplicationException(
          MessageFormat.format("User {0} is not allowed to create networks", username),
          HttpStatus.FORBIDDEN);
    } else if ((matcher = NETWORK_PATTERN_CONSTITUENT_CREATE.matcher(path)).find()) {
      ensureNetworkEntity(
          "network",
          UUID.fromString(matcher.group(1)),
          username,
          userAuthService::allowedToModifyEntity);
    }
  }

  private void ensureUpdateDeleteRequest(String username, String path) {
    Matcher matcher;
    if ((matcher = DATASET_PATTERN_UPDATE_DELETE.matcher(path)).find()) {
      ensureNetworkEntity(
          "dataset",
          UUID.fromString(matcher.group(1)),
          username,
          userAuthService::allowedToModifyDataset);
    } else if ((matcher = INSTALLATION_PATTERN_UPDATE_DELETE.matcher(path)).find()) {
      ensureNetworkEntity(
          "installation",
          UUID.fromString(matcher.group(1)),
          username,
          userAuthService::allowedToModifyInstallation);
    } else if ((matcher = ORGANIZATION_PATTERN_UPDATE_DELETE.matcher(path)).find()) {
      ensureNetworkEntity(
          "organization",
          UUID.fromString(matcher.group(1)),
          username,
          userAuthService::allowedToModifyOrganization);
    } else if ((matcher = NODE_PATTERN_UPDATE_DELETE.matcher(path)).find()) {
      ensureNetworkEntity(
          "node",
          UUID.fromString(matcher.group(1)),
          username,
          userAuthService::allowedToModifyEntity);
    } else if ((matcher = NETWORK_PATTERN_UPDATE_DELETE.matcher(path)).find()) {
      ensureNetworkEntity(
          "network",
          UUID.fromString(matcher.group(1)),
          username,
          userAuthService::allowedToModifyEntity);
    }
  }

  private void ensureNetworkEntity(
      String entityName,
      UUID entityKey,
      String username,
      BiFunction<String, UUID, Boolean> checkAllowedToModifyEntity) {
    if (!checkAllowedToModifyEntity.apply(username, entityKey)) {
      LOG.warn("User {} is not allowed to modify {} {}", username, entityName, entityKey);
      throw new WebApplicationException(
          MessageFormat.format(
              "User {0} is not allowed to modify {1} {2}", username, entityName, entityKey),
          HttpStatus.FORBIDDEN);
    } else {
      LOG.debug("User {} is allowed to modify {} {}", username, entityName, entityKey);
    }
  }

  private <T extends NetworkEntity> void ensureNetworkEntity(
      String entityName,
      T entity,
      String username,
      BiFunction<String, T, Boolean> checkAllowedToModifyEntity) {
    if (!checkAllowedToModifyEntity.apply(username, entity)) {
      LOG.warn("User {} is not allowed to modify {} {}", username, entityName, entity.getKey());
      throw new WebApplicationException(
          MessageFormat.format(
              "User {0} is not allowed to modify {1} {2}", username, entityName, entity.getKey()),
          HttpStatus.FORBIDDEN);
    } else {
      LOG.debug("User {} is allowed to modify {} {}", username, entityName, entity.getKey());
    }
  }

  private boolean checkRequestRequiresEditorValidation(String path) {
    boolean isBaseNetworkEntityResource =
        path.startsWith("/node")
            || path.startsWith("/network")
            || path.startsWith("/organization")
            || path.startsWith("/installation")
            || path.startsWith("/dataset");

    // exclude endorsement and machine tag from validation
    boolean isNotEndorsement = !path.contains("endorsement");
    boolean isNotMachineTag = !path.contains("machineTag");
    boolean isNoTitles = !path.contains("titles");

    return isBaseNetworkEntityResource && isNotEndorsement && isNotMachineTag && isNoTitles;
  }

  private boolean isNotGetOrOptionsRequest(HttpServletRequest httpRequest) {
    return !"GET".equals(httpRequest.getMethod()) && !"OPTIONS".equals(httpRequest.getMethod());
  }
}
