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
package org.gbif.registry.security.grscicoll;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.merge.ConvertToCollectionParams;
import org.gbif.api.model.collections.merge.MergeParams;
import org.gbif.api.model.registry.MachineTag;
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
 *
 * <p>NOTE that this filter should be in sync with {@link
 * org.gbif.registry.security.precheck.AuthPreCheckCreationRequestFilter}.
 */
@Component
public class GrSciCollEditorAuthorizationFilter extends OncePerRequestFilter {

  private static final Logger LOG =
      LoggerFactory.getLogger(GrSciCollEditorAuthorizationFilter.class);

  public static final String GRSCICOLL_PATH = "grscicoll";
  public static final Pattern ENTITY_PATTERN =
      Pattern.compile(".*/grscicoll/(collection|institution)/([a-f0-9-]+).*");
  public static final Pattern FIRST_CLASS_ENTITY_UPDATE =
      Pattern.compile(".*/grscicoll/(collection|institution|person)/([a-f0-9-]+)$");
  public static final Pattern CHANGE_SUGGESTION_UPDATE_PATTERN =
      Pattern.compile(".*/grscicoll/(collection|institution)/changeSuggestion/([0-9]+).*");
  public static final Pattern INST_COLL_CREATE_PATTERN =
      Pattern.compile(".*/grscicoll/(collection|institution)$");
  public static final Pattern MACHINE_TAG_PATTERN_DELETE =
      Pattern.compile(
          ".*/grscicoll/(collection|institution|person)/([a-f0-9-]+)/machineTag/([0-9]+).*");
  public static final Pattern MERGE_PATTERN =
      Pattern.compile(".*/grscicoll/(collection|institution)/([a-f0-9-]+)/merge$");
  public static final Pattern CONVERSION_PATTERN =
      Pattern.compile(".*/grscicoll/institution/([a-f0-9-]+)/convertToCollection$");

  public static final String INSTITUTION = "institution";
  public static final String COLLECTION = "collection";

  private final GrSciCollAuthorizationService authService;
  private final AuthenticationFacade authenticationFacade;
  private final ObjectMapper objectMapper;

  public GrSciCollEditorAuthorizationFilter(
      GrSciCollAuthorizationService authService,
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
    if (isNotGetOrOptionsRequest(request)
        && path.contains(GRSCICOLL_PATH)
        && !isChangeSuggestionCreation(request, path)) {
      // user must NOT be null if the resource requires editor rights restrictions
      ensureUserSetInSecurityContext(authentication, HttpStatus.FORBIDDEN);

      // validate only if user is not GrSciColl admin
      if (!checkUserInRole(authentication, UserRoles.GRSCICOLL_ADMIN_ROLE)) {
        // only editors allowed to modify, because admins already excluded
        if (!checkUserInRole(
            authentication, UserRoles.GRSCICOLL_EDITOR_ROLE, UserRoles.GRSCICOLL_MEDIATOR_ROLE)) {
          throw new WebApplicationException(
              "User has no GrSciColl editor rights", HttpStatus.FORBIDDEN);
        }

        if (isChangeSuggestionRequest(path)) {
          checkChangeSuggestionUpdate(path, authentication);
        } else if (!isBatchRequest(path)) {
          // editors cannot edit machine tags
          checkMachineTagsPermissions(request, path, authentication);

          checkInstitutionAndCollectionCreationPermissions(request, path, authentication);

          checkInstitutionAndCollectionUpdatePermissions(request, path, authentication);
        }
      }
    }
    filterChain.doFilter(request, response);
  }

  private boolean isChangeSuggestionRequest(String path) {
    return path.contains("/changeSuggestion");
  }

  private boolean isBatchRequest(String path) {
    return path.contains("/batch");
  }

  // it's a POST but can be done by anyone
  private boolean isChangeSuggestionCreation(HttpServletRequest request, String path) {
    return "POST".equals(request.getMethod()) && isChangeSuggestionRequest(path);
  }

  private void checkChangeSuggestionUpdate(String path, Authentication authentication) {
    Matcher matcher = CHANGE_SUGGESTION_UPDATE_PATTERN.matcher(path);
    if (matcher.find()) {
      String entityType = matcher.group(1);
      int key = Integer.parseInt(matcher.group(2));
      if (!authService.allowedToUpdateChangeSuggestion(key, entityType, authentication)) {
        throw new WebApplicationException(
            MessageFormat.format(
                "User {0} is not allowed to update change suggestion {1}",
                authentication.getName(), key),
            HttpStatus.FORBIDDEN);
      }
    }
  }

  private void checkInstitutionAndCollectionUpdatePermissions(
      HttpServletRequest request, String path, Authentication authentication) {
    Matcher matcher = ENTITY_PATTERN.matcher(path);
    if (matcher.find()) {
      UUID entityKey = UUID.fromString(matcher.group(2));
      String entityType = matcher.group(1);

      boolean firstClassEntityUpdate = FIRST_CLASS_ENTITY_UPDATE.matcher(path).matches();
      boolean isDeleteEntity = "DELETE".equals(request.getMethod()) && firstClassEntityUpdate;
      boolean isMerge = MERGE_PATTERN.matcher(path).matches();
      boolean isConversion = CONVERSION_PATTERN.matcher(path).matches();

      boolean allowed = false;
      if (INSTITUTION.equalsIgnoreCase(entityType)) {
        if (isDeleteEntity) {
          allowed = authService.allowedToDeleteInstitution(authentication, entityKey);
        } else if (isMerge) {
          allowed =
              authService.allowedToMergeInstitution(
                  authentication, entityKey, getMergeTargetEntityKey(request));
        } else if (isConversion) {
          allowed =
              authService.allowedToConvertInstitution(
                  authentication, entityKey, getConversionNewInstitutionKey(request));
        } else {
          allowed = authService.allowedToModifyInstitution(authentication, entityKey);
        }
      } else if (COLLECTION.equalsIgnoreCase(entityType)) {
        if (isDeleteEntity) {
          allowed = authService.allowedToDeleteCollection(authentication, entityKey);
        } else if (isMerge) {
          allowed =
              authService.allowedToMergeCollection(
                  authentication, entityKey, getMergeTargetEntityKey(request));
        } else {
          Collection collectionInMessageBody = null;
          if (firstClassEntityUpdate) {
            collectionInMessageBody = readEntity(request, Collection.class);
          }
          allowed =
              authService.allowedToModifyCollection(
                  authentication, entityKey, collectionInMessageBody);
        }
      }

      if (!allowed) {
        throw new WebApplicationException(
            MessageFormat.format(
                "User {0} is not allowed to modify entity {1} of type {2}",
                authentication.getName(), entityKey, entityType),
            HttpStatus.FORBIDDEN);
      }
    }
  }

  private UUID getMergeTargetEntityKey(HttpServletRequest request) {
    MergeParams mergeParams = readEntity(request, MergeParams.class);
    return mergeParams != null ? mergeParams.getReplacementEntityKey() : null;
  }

  private UUID getConversionNewInstitutionKey(HttpServletRequest request) {
    ConvertToCollectionParams conversionParams =
        readEntity(request, ConvertToCollectionParams.class);
    return conversionParams != null ? conversionParams.getInstitutionForNewCollectionKey() : null;
  }

  private void checkInstitutionAndCollectionCreationPermissions(
      HttpServletRequest request, String path, Authentication authentication) {
    Matcher createEntityMatch = INST_COLL_CREATE_PATTERN.matcher(path);
    if ("POST".equals(request.getMethod()) && createEntityMatch.find()) {
      String entityType = createEntityMatch.group(1);

      boolean allowed = false;
      if (INSTITUTION.equalsIgnoreCase(entityType)) {
        Institution institution = readEntity(request, Institution.class);
        allowed = authService.allowedToCreateInstitution(institution, authentication);
      } else if (COLLECTION.equalsIgnoreCase(entityType)) {
        Collection collection = readEntity(request, Collection.class);
        allowed = authService.allowedToCreateCollection(collection, authentication);
      }

      if (!allowed) {
        throw new WebApplicationException(
            MessageFormat.format(
                "User {0} is not allowed to create entity {1}",
                authentication.getName(), entityType),
            HttpStatus.FORBIDDEN);
      }
    }
  }

  private void checkMachineTagsPermissions(
      HttpServletRequest request, String path, Authentication authentication) {
    if (path.contains("/machineTag")) {
      boolean allowed = false;
      if ("POST".equals(request.getMethod())) {
        MachineTag machineTagToCreate = readEntity(request, MachineTag.class);
        allowed = authService.allowedToCreateMachineTag(authentication, machineTagToCreate);
      } else if ("DELETE".equals(request.getMethod())) {
        Matcher matcher = MACHINE_TAG_PATTERN_DELETE.matcher(path);
        if (matcher.find()) {
          int machineTagKey = Integer.parseInt(matcher.group(3));
          allowed = authService.allowedToDeleteMachineTag(authentication, machineTagKey);
        }
      }

      if (!allowed) {
        throw new WebApplicationException(
            MessageFormat.format(
                "User {0} is not allowed to create or delete machine tag",
                authentication.getName()),
            HttpStatus.FORBIDDEN);
      }
    }
  }

  private <T> T readEntity(HttpServletRequest request, Class<T> clazz) {
    String content = ((GbifHttpServletRequestWrapper) request).getContent();
    if (content == null) {
      return null;
    }
    try {
      return objectMapper.readValue(content, clazz);
    } catch (JsonProcessingException e) {
      LOG.warn("Couldn't read entity from message body", e);
      return null;
    }
  }

  private boolean isNotGetOrOptionsRequest(HttpServletRequest httpRequest) {
    return !"GET".equals(httpRequest.getMethod()) && !"OPTIONS".equals(httpRequest.getMethod());
  }
}
