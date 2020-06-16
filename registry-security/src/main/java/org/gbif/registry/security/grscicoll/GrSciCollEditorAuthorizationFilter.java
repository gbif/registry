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
package org.gbif.registry.security.grscicoll;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.registry.Identifier;
import org.gbif.registry.security.AuthenticationFacade;
import org.gbif.registry.security.UserRoles;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.server.GbifHttpServletRequestWrapper;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.UUID;
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

import static org.gbif.registry.security.SecurityContextCheck.checkUserInRole;
import static org.gbif.registry.security.SecurityContextCheck.ensureUserSetInSecurityContext;

/**
 * For requests authenticated with a GRSCICOLL_EDITOR role two levels of authorization need to be
 * passed. First of all any resource method is required to have the role included in the Secured or
 * RolesAllowed annotation. Secondly this request filter needs to be passed for POST/PUT/DELETE
 * requests that act on existing and UUID identified collection entities.
 */
@Component
public class GrSciCollEditorAuthorizationFilter extends OncePerRequestFilter {

  private static final Logger LOG =
      LoggerFactory.getLogger(GrSciCollEditorAuthorizationFilter.class);

  private static final Pattern ENTITY_PATTERN =
      Pattern.compile(".*/grscicoll/(collection|institution|person)/([a-f0-9-]+).*");
  private static final Pattern FIRST_CLASS_ENTITY_UPDATE =
      Pattern.compile(".*/grscicoll/(collection|institution|person)/([a-f0-9-]+)$");
  private static final Pattern IDENTIFIER_PATTERN_DELETE =
      Pattern.compile(
          ".*/grscicoll/(collection|institution|person)/([a-f0-9-]+)/identifier/([0-9]+).*");

  private static final Pattern INST_COLL_CREATE_PATTERN =
      Pattern.compile(".*/grscicoll/(collection|institution)$");

  private final GrSciCollEditorAuthorizationService authService;
  private final AuthenticationFacade authenticationFacade;
  private final ObjectMapper objectMapper;

  public GrSciCollEditorAuthorizationFilter(
      GrSciCollEditorAuthorizationService authService,
      AuthenticationFacade authenticationFacade,
      @Qualifier("registryObjectMapper") ObjectMapper objectMapper) {
    this.authService = authService;
    this.authenticationFacade = authenticationFacade;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    // only verify non GET methods with an authenticated GRSCICOLL_EDITOR
    // all other roles are taken care by simple 'Secured' or JSR250 annotations on the resource
    // methods
    final Authentication authentication = authenticationFacade.getAuthentication();
    final String path = request.getRequestURI();

    // skip GET and OPTIONS requests and only check requests to grscicoll
    if (isNotGetOrOptionsRequest(request) && path.contains("grscicoll")) {
      // user must NOT be null if the resource requires editor rights restrictions
      ensureUserSetInSecurityContext(authentication, HttpStatus.FORBIDDEN);
      final String username = authentication.getName();

      // validate only if user is not GrSciColl admin
      if (!checkUserInRole(authentication, UserRoles.GRSCICOLL_ADMIN_ROLE)) {
        // only editors allowed to modify, because admins already excluded
        if (!checkUserInRole(authentication, UserRoles.GRSCICOLL_EDITOR_ROLE)) {
          throw new WebApplicationException(
              "User has no GrSciColl editor rights", HttpStatus.FORBIDDEN);
        }

        // editors cannot edit IH_IRN identifiers
        checkIrnIdentifierPermissions(request, path, username);

        // editors cannot edit machine tags
        checkMachineTagsPermissions(path, username);

        // editors cannot create institutions or collections without institution
        checkInstitutionAndCollectionCreationPermissions(request, path, username);

        // check user rights in institution and collection
        checkInstitutionAndCollectionUpdatePermissions(request, path, username);
      }
    }
    filterChain.doFilter(request, response);
  }

  private void checkInstitutionAndCollectionUpdatePermissions(
      HttpServletRequest request, String path, String username) {
    if (path.startsWith("/grscicoll/institution")) {
      // we check user rights of the institution
      Matcher matcher = ENTITY_PATTERN.matcher(path);
      if (matcher.find()) {
        UUID entityKey = UUID.fromString(matcher.group(2));
        if (!authService.allowedToModifyEntity(username, entityKey)) {
          throw new WebApplicationException(
              MessageFormat.format(
                  "User {0} is not allowed to edit entity {1}", username, entityKey),
              HttpStatus.FORBIDDEN);
        }
      }
    } else if (path.startsWith("/grscicoll/collection")) {
      // we check user rights of the collection and its institution
      Matcher matcher = ENTITY_PATTERN.matcher(path);
      if (matcher.find()) {
        UUID entityKey = UUID.fromString(matcher.group(2));
        Collection collectionInMessageBody = null;
        if (FIRST_CLASS_ENTITY_UPDATE.matcher(path).matches()) {
          collectionInMessageBody = readEntity(request, Collection.class);
        }
        if (!authService.allowedToUpdateCollection(username, entityKey, collectionInMessageBody)) {
          throw new WebApplicationException(
              MessageFormat.format(
                  "User {0} is not allowed to edit or create entity {1}", username, entityKey),
              HttpStatus.FORBIDDEN);
        }
      }
    }
  }

  private void checkInstitutionAndCollectionCreationPermissions(
      HttpServletRequest request, String path, String username) {
    Matcher createEntityMatch = INST_COLL_CREATE_PATTERN.matcher(path);
    if ("POST".equals(request.getMethod()) && createEntityMatch.find()) {
      String entityType = createEntityMatch.group(1);

      boolean allowed = true;
      if ("institution".equalsIgnoreCase(entityType)) {
        allowed = false;
      }

      if ("collection".equalsIgnoreCase(entityType)) {
        Collection collection = readEntity(request, Collection.class);
        if (collection == null || collection.getInstitutionKey() == null) {
          allowed = false;
        } else {
          allowed = authService.allowedToModifyEntity(username, collection.getInstitutionKey());
        }
      }

      if (!allowed) {
        throw new WebApplicationException(
            MessageFormat.format(
                "User {0} is not allowed to create entity {1}", username, entityType),
            HttpStatus.FORBIDDEN);
      }
    }
  }

  private void checkMachineTagsPermissions(String path, String username) {
    if (path.contains("/machineTag")) {
      throw new WebApplicationException(
          MessageFormat.format(
              "User {0} is not allowed to create or delente machine tags of GrSciColl entities",
              username),
          HttpStatus.FORBIDDEN);
    }
  }

  private void checkIrnIdentifierPermissions(
      HttpServletRequest request, String path, String username) {
    if (path.contains("/identifier")) {
      if ("POST".equals(request.getMethod())) {
        Identifier identifierToCreate = readEntity(request, Identifier.class);
        if (authService.isIrnIdentifier(identifierToCreate)) {
          throw new WebApplicationException(
              MessageFormat.format("User {0} is not allowed to create an IRN identifier", username),
              HttpStatus.FORBIDDEN);
        }
      } else if ("DELETE".equals(request.getMethod())) {
        Matcher matcher = IDENTIFIER_PATTERN_DELETE.matcher(path);
        if (matcher.find()) {
          String entityType = matcher.group(1);
          UUID entityKey = UUID.fromString(matcher.group(2));
          int identifierKey = Integer.parseInt(matcher.group(3));
          if (authService.isIrnIdentifier(entityType, entityKey, identifierKey)) {
            throw new WebApplicationException(
                MessageFormat.format(
                    "User {0} is not allowed to delete an IRN identifier {1} from {2} {3}",
                    username, identifierKey, entityType, entityKey),
                HttpStatus.FORBIDDEN);
          }
        }
      }
    }
  }

  private <T> T readEntity(HttpServletRequest request, Class<T> clazz) {
    try {
      return objectMapper.readValue(((GbifHttpServletRequestWrapper) request).getContent(), clazz);
    } catch (JsonProcessingException e) {
      LOG.warn("Couldn't read entity from message body", e);
      return null;
    }
  }

  private boolean isNotGetOrOptionsRequest(HttpServletRequest httpRequest) {
    return !"GET".equals(httpRequest.getMethod()) && !"OPTIONS".equals(httpRequest.getMethod());
  }
}
