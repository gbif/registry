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

import org.gbif.ws.WebApplicationException;

import java.io.IOException;
import java.text.MessageFormat;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

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

  private static final String ENTITY_KEY = "^/?%s/([a-f0-9-]+).*";
  private static final Pattern NODE_PATTERN = Pattern.compile(String.format(ENTITY_KEY, "node"));
  private static final Pattern NETWORK_PATTERN =
      Pattern.compile(String.format(ENTITY_KEY, "network"));
  private static final Pattern ORGANIZATION_PATTERN =
      Pattern.compile(String.format(ENTITY_KEY, "organization"));
  private static final Pattern INSTALLATION_PATTERN =
      Pattern.compile(String.format(ENTITY_KEY, "installation"));
  private static final Pattern DATASET_PATTERN =
      Pattern.compile(String.format(ENTITY_KEY, "dataset"));

  private final EditorAuthorizationService userAuthService;
  private final AuthenticationFacade authenticationFacade;

  public EditorAuthorizationFilter(
      EditorAuthorizationService userAuthService, AuthenticationFacade authenticationFacade) {
    this.userAuthService = userAuthService;
    this.authenticationFacade = authenticationFacade;
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
          ensureDataset(username, path);
          ensureInstallation(username, path);
          ensureOrganization(username, path);
          ensureNetwork(username, path);
          ensureNode(username, path);
        } catch (IllegalArgumentException e) {
          // no valid UUID, do nothing as it should not be a valid request anyway
        }
      }
    }
    filterChain.doFilter(request, response);
  }

  private void ensureNode(String username, String path) {
    Optional.of(NODE_PATTERN.matcher(path))
        .filter(Matcher::find)
        .ifPresent(
            matcher ->
                ensureNetworkEntity(
                    "node",
                    UUID.fromString(matcher.group(1)),
                    username,
                    userAuthService::allowedToModifyEntity));
  }

  private void ensureNetwork(String username, String path) {
    Optional.of(NETWORK_PATTERN.matcher(path))
        .filter(Matcher::find)
        .ifPresent(
            matcher ->
                ensureNetworkEntity(
                    "network",
                    UUID.fromString(matcher.group(1)),
                    username,
                    userAuthService::allowedToModifyEntity));
  }

  private void ensureOrganization(String username, String path) {
    Optional.of(ORGANIZATION_PATTERN.matcher(path))
        .filter(Matcher::find)
        .ifPresent(
            matcher ->
                ensureNetworkEntity(
                    "organization",
                    UUID.fromString(matcher.group(1)),
                    username,
                    userAuthService::allowedToModifyOrganization));
  }

  private void ensureInstallation(String username, String path) {
    Optional.of(INSTALLATION_PATTERN.matcher(path))
        .filter(Matcher::find)
        .ifPresent(
            matcher ->
                ensureNetworkEntity(
                    "installation",
                    UUID.fromString(matcher.group(1)),
                    username,
                    userAuthService::allowedToModifyInstallation));
  }

  private void ensureDataset(String username, String path) {
    Optional.of(DATASET_PATTERN.matcher(path))
        .filter(Matcher::find)
        .ifPresent(
            matcher ->
                ensureNetworkEntity(
                    "dataset",
                    UUID.fromString(matcher.group(1)),
                    username,
                    userAuthService::allowedToModifyDataset));
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

  private boolean checkRequestRequiresEditorValidation(String path) {
    boolean isBaseNetworkEntityResource =
        ORGANIZATION_PATTERN.matcher(path).matches()
            || DATASET_PATTERN.matcher(path).matches()
            || INSTALLATION_PATTERN.matcher(path).matches()
            || NODE_PATTERN.matcher(path).matches()
            || NETWORK_PATTERN.matcher(path).matches();

    // exclude endorsement and machine tag from validation
    boolean isNotEndorsement = !path.contains("endorsement");
    boolean isNotMachineTag = !path.contains("machineTag");

    return isBaseNetworkEntityResource && isNotEndorsement && isNotMachineTag;
  }

  private boolean isNotGetOrOptionsRequest(HttpServletRequest httpRequest) {
    return !"GET".equals(httpRequest.getMethod()) && !"OPTIONS".equals(httpRequest.getMethod());
  }
}
