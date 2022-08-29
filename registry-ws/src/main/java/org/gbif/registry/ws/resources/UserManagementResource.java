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
package org.gbif.registry.ws.resources;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.ConfirmationKeyParameter;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.domain.ws.AuthenticationDataParameters;
import org.gbif.registry.domain.ws.EmailChangeRequest;
import org.gbif.registry.domain.ws.UserAdminView;
import org.gbif.registry.domain.ws.UserCreation;
import org.gbif.registry.domain.ws.UserUpdate;
import org.gbif.registry.identity.model.LoggedUser;
import org.gbif.registry.identity.model.UserModelMutationResult;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.persistence.mapper.OccurrenceDownloadMapper;
import org.gbif.registry.security.SecurityContextCheck;
import org.gbif.registry.security.UserUpdateRulesManager;
import org.gbif.registry.ws.UpdatePasswordException;
import org.gbif.utils.AnnotationUtils;
import org.gbif.ws.security.AppkeysConfigurationProperties;
import org.gbif.ws.server.filter.AppIdentityFilter;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.APP_ROLE;
import static org.gbif.registry.security.UserRoles.USER_ROLE;

/**
 * The "/admin/user" resource represents the "endpoints" related to user management. This means the
 * resource it expected to be called by another application (mostly the Registry Console and the
 * portal backend).
 *
 * <p>Design and implementation decisions: - This resource contains mostly the routing to the
 * business logic ({@link IdentityService}) including authorizations. This resource does NOT
 * implement the service but aggregates it (by Dependency Injection). - Methods can return {@link
 * ResponseEntity} instead of object to minimize usage of exceptions and provide better control over
 * the HTTP code returned. This also allows to return an entity in case of errors (e.g. {@link
 * UserModelMutationResult} - keys (user id) are not considered public, therefore the username is
 * used as key - In order to strictly control the data that is exposed this class uses "view models"
 * (e.g. {@link UserAdminView})
 *
 * <p>Please note there is 3 possible ways to be authenticated: - HTTP Basic authentication - User
 * impersonation using appKey. ALL applications with a valid appKey can impersonate a user. -
 * Application itself (APP_ROLE). All applications with a valid appKey that is also present in the
 * appKey whitelist. See {@link AppIdentityFilter}.
 */
@SuppressWarnings("UnstableApiUsage")
@Validated
@RestController
@RequestMapping(path = "admin/user", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserManagementResource {

  // filters roles that are deprecated
  private static final List<UserRole> USER_ROLES =
      Arrays.stream(UserRole.values())
          .filter(r -> !AnnotationUtils.isFieldDeprecated(UserRole.class, r.name()))
          .collect(Collectors.toList());

  private final IdentityService identityService;
  private final List<String> appKeyWhitelist;
  private final OccurrenceDownloadMapper occurrenceDownloadMapper;

  /** {@link UserManagementResource} main constructor. */
  public UserManagementResource(
      IdentityService identityService,
      AppkeysConfigurationProperties appkeysConfiguration,
      OccurrenceDownloadMapper occurrenceDownloadMapper) {
    this.identityService = identityService;
    appKeyWhitelist = appkeysConfiguration.getWhitelist();
    this.occurrenceDownloadMapper = occurrenceDownloadMapper;
  }

  @GetMapping("roles")
  public List<UserRole> listRoles() {
    return USER_ROLES;
  }

  /**
   * GET a {@link UserAdminView} of a user. Mostly for admin console and access by authorized appkey
   * (e.g. portal nodejs backend). Returns the identified user account.
   *
   * @return the {@link UserAdminView} or null
   */
  @GetMapping("{username}")
  @Secured({ADMIN_ROLE, APP_ROLE})
  public UserAdminView getUser(@PathVariable String username) {
    GbifUser user = identityService.get(username);
    if (user == null) {
      return null;
    }
    return new UserAdminView(user, identityService.hasPendingConfirmation(user.getKey()));
  }

  @GetMapping("find")
  @Secured({ADMIN_ROLE, APP_ROLE})
  public UserAdminView getUserBySystemSetting(@RequestParam Map<String, String> queryParams) {
    GbifUser user = null;
    Iterator<Map.Entry<String, String>> it = queryParams.entrySet().iterator();
    if (it.hasNext()) {
      Map.Entry<String, String> paramPair = it.next();
      user = identityService.getBySystemSetting(paramPair.getKey(), paramPair.getValue());
    }

    if (user == null) {
      return null;
    }
    return new UserAdminView(user, identityService.hasPendingConfirmation(user.getKey()));
  }

  /** Creates a new user. (only available to the portal backend). */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Secured(APP_ROLE)
  public ResponseEntity<UserModelMutationResult> create(
      @RequestBody @NotNull @Valid UserCreation user) {
    int returnStatusCode = HttpStatus.CREATED.value();
    UserModelMutationResult result =
        identityService.create(UserUpdateRulesManager.applyCreate(user), user.getPassword());
    if (result.containsError()) {
      returnStatusCode = HttpStatus.UNPROCESSABLE_ENTITY.value();
    }
    return ResponseEntity.status(returnStatusCode).body(result);
  }

  /**
   * Updates a user. Available to admin-console and portal backend. {@link UserUpdateRulesManager}
   * will be used to determine which properties it is possible to update based on the role, all
   * other properties will be ignored.
   *
   * <p>At the moment, a user cannot update its own data calling the API directly using HTTP Basic
   * auth. If this is required/wanted, it would go in {@link UserResource} to only accept the role
   * USER and ensure a user can only update its own data.
   */
  @PutMapping(path = "{username}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Secured({ADMIN_ROLE, APP_ROLE})
  public ResponseEntity<UserModelMutationResult> update(
      @PathVariable String username,
      @RequestBody @NotNull @Valid UserUpdate userUpdate,
      Authentication authentication) {
    ResponseEntity<UserModelMutationResult> response = ResponseEntity.noContent().build();
    // ensure the key used to access the update is actually the one of the user represented by the
    // UserUpdate
    GbifUser currentUser = identityService.get(username);
    if (SecurityContextCheck.checkSameUser(currentUser, userUpdate.getUserName())) {
      GbifUser updateInitiator = null;

      if (authentication != null) {
        updateInitiator = identityService.get(authentication.getName());
      }

      boolean fromTrustedApp = SecurityContextCheck.checkUserInRole(authentication, APP_ROLE);
      Set<UserRole> initiatorRoles = updateInitiator != null ? updateInitiator.getRoles() : null;

      GbifUser user =
          UserUpdateRulesManager.applyUpdate(
              initiatorRoles, currentUser, userUpdate, fromTrustedApp);
      UserModelMutationResult result = identityService.update(user);

      if (result.containsError()) {
        response = ResponseEntity.unprocessableEntity().body(result);
      }
    } else {
      response = ResponseEntity.badRequest().build();
    }
    return response;
  }

  /**
   * Confirm a confirmationKey for a specific user. The username is expected to be present in the
   * security context (authenticated by appkey).
   *
   * @param confirmationKeyParameter confirmation key (UUID)
   * @return logged user data
   */
  @PostMapping(path = "confirm", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Secured(USER_ROLE)
  @Transactional
  public ResponseEntity<LoggedUser> confirmChallengeCode(
      Authentication authentication,
      @RequestHeader("Authorization") String authHeader,
      @RequestBody @NotNull @Valid ConfirmationKeyParameter confirmationKeyParameter) {

    // we ONLY accept user impersonation, and only from a trusted app key.
    SecurityContextCheck.ensureAuthorizedUserImpersonation(
        authentication, authHeader, appKeyWhitelist);

    GbifUser user = identityService.get(authentication.getName());
    if (user != null
        && identityService.confirmAndNotifyUser(
            user.getKey(), confirmationKeyParameter.getConfirmationKey())) {
      identityService.updateLastLogin(user.getKey());

      // ideally we would return 200 OK but CreatedResponseFilter automatically change it to 201
      // CREATED
      return ResponseEntity.status(HttpStatus.CREATED).body(LoggedUser.from(user));
    }
    return ResponseEntity.badRequest().build();
  }

  /** For admin console only. */
  @Secured(ADMIN_ROLE)
  @DeleteMapping("{username}")
  public ResponseEntity<Void> delete(@PathVariable String username) {
    GbifUser user = identityService.get(username);

    if (user == null) {
      return ResponseEntity.notFound().build();
    }

    String newUsername = RandomStringUtils.randomAlphanumeric(6);
    String newEmail = "deleted_" + newUsername + "@deleted.invalid";
    String oldUsername = user.getUserName();

    // get all downloads before erase
    List<Download> downloads = occurrenceDownloadMapper.listByUser(user.getUserName(), null, null, null);

    // erase user from downloads
    occurrenceDownloadMapper.updateNotificationAddresses(oldUsername, newUsername, "{}");

    GbifUser userErasedPersonalData = new GbifUser(user);

    // remove sensitive data, delete user and send an email
    userErasedPersonalData.setFirstName(null);
    userErasedPersonalData.setLastName(null);
    userErasedPersonalData.setUserName(newUsername);
    userErasedPersonalData.setEmail(newEmail);
    userErasedPersonalData.setPasswordHash("DELETED_DELETED_DELETED_DELETED_");
    userErasedPersonalData.setRoles(Collections.emptySet());
    userErasedPersonalData.setSettings(null);
    userErasedPersonalData.setSystemSettings(null);
    identityService.delete(user, userErasedPersonalData, downloads);

    return ResponseEntity.noContent().build();
  }

  /** For admin console only. User search, intended for user administration console use only. */
  @GetMapping("search")
  @Secured(ADMIN_ROLE)
  public PagingResponse<GbifUser> search(
      @Nullable @RequestParam(value = "q", required = false) String query,
      @Nullable @RequestParam(value = "role", required = false) Set<UserRole> roles,
      @Nullable @RequestParam(value = "editorRightsOn", required = false) Set<UUID> editorRightsOn,
      @Nullable @RequestParam(value = "namespaceRightsOn", required = false)
          Set<String> namespaceRightsOn,
      @Nullable @RequestParam(value = "countryRightsOn", required = false)
          Set<String> countryRightsOn,
      Pageable page) {
    page = page == null ? new PagingRequest() : page;
    String q =
        Optional.ofNullable(query)
            .map(v -> Strings.nullToEmpty(CharMatcher.WHITESPACE.trimFrom(v)))
            .orElse(null);
    Set<Country> countries =
        Optional.ofNullable(countryRightsOn)
            .map(v -> v.stream().map(Country::fromIsoCode).collect(Collectors.toSet()))
            .orElse(null);

    return identityService.search(q, roles, editorRightsOn, namespaceRightsOn, countries, page);
  }

  /**
   * A user requesting his password to be reset. The username is expected to be present in the
   * security context (authenticated by appkey). This method will always return 204 No Content.
   */
  @PostMapping("resetPassword")
  @Secured(USER_ROLE)
  public ResponseEntity<Void> resetPassword(
      Authentication authentication, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
    // we ONLY accept user impersonation, and only from a trusted app key.
    SecurityContextCheck.ensureAuthorizedUserImpersonation(
        authentication, authHeader, appKeyWhitelist);

    String identifier = authentication.getName();
    GbifUser user = identityService.get(identifier);
    if (user != null) {
      // initiate mail, and store the challenge etc.
      identityService.resetPassword(user.getKey());
    }
    return ResponseEntity.noContent().build();
  }

  /**
   * Updates the user password only if the token presented is valid for the user account. The
   * username is expected to be present in the security context (authenticated by appkey).
   */
  @PostMapping(path = "updatePassword", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Secured(USER_ROLE)
  @Transactional
  public ResponseEntity<LoggedUser> updatePassword(
      Authentication authentication,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
      @RequestBody @NotNull AuthenticationDataParameters authenticationDataParameters) {

    // we ONLY accept user impersonation, and only from a trusted app key.
    SecurityContextCheck.ensureAuthorizedUserImpersonation(
        authentication, authHeader, appKeyWhitelist);

    String username = authentication.getName();
    GbifUser user = identityService.get(username);

    UserModelMutationResult updatePasswordMutationResult =
        identityService.updatePassword(
            user.getKey(),
            authenticationDataParameters.getPassword(),
            authenticationDataParameters.getChallengeCode());

    if (updatePasswordMutationResult.containsError()) {
      throw new UpdatePasswordException(updatePasswordMutationResult);
    } else {
      identityService.updateLastLogin(user.getKey());
      return ResponseEntity.ok(LoggedUser.from(user));
    }
  }

  /**
   * Utility to determine if the challengeCode provided is valid for the given user. The username is
   * expected to be present in the security context (authenticated by appkey).
   *
   * @param confirmationKey To check
   */
  @GetMapping("confirmationKeyValid")
  @Secured(USER_ROLE)
  public ResponseEntity<Void> tokenValidityCheck(
      Authentication authentication,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
      @Nullable @RequestParam(value = "confirmationKey", required = false) UUID confirmationKey,
      @Nullable @RequestParam(value = "email", required = false) String email) {

    // we ONLY accept user impersonation, and only from a trusted app key.
    SecurityContextCheck.ensureAuthorizedUserImpersonation(
        authentication, authHeader, appKeyWhitelist);

    String username = authentication.getName();
    GbifUser user = identityService.get(username);

    if (user == null
        || identityService.isConfirmationKeyValid(user.getKey(), email, confirmationKey)) {
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }

  /** List the editor rights for a user. */
  @GetMapping("{username}/editorRight")
  @Secured({ADMIN_ROLE, USER_ROLE})
  public ResponseEntity<List<UUID>> editorRights(
      @PathVariable String username, Authentication authentication) {
    // Non-admin users can only see their own entry.
    if (!SecurityContextCheck.checkUserInRole(authentication, ADMIN_ROLE)) {
      String usernameInContext = authentication.getName();
      if (!usernameInContext.equals(username)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
      }
    }

    // Ensure user exists
    GbifUser currentUser = identityService.get(username);
    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    List<UUID> rights = identityService.listEditorRights(username);
    return ResponseEntity.ok(rights);
  }

  /** Add an entity right for a user. */
  @PostMapping(
      path = "{username}/editorRight",
      consumes = {MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Secured({ADMIN_ROLE})
  public ResponseEntity<UUID> addEditorRight(
      @PathVariable String username, @RequestBody @NotNull String strKey) {

    final UUID key = UUID.fromString(strKey);

    // Ensure user exists
    GbifUser currentUser = identityService.get(username);
    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    if (identityService.listEditorRights(username).contains(key)) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    } else {
      identityService.addEditorRight(username, key);
      return ResponseEntity.ok(key);
    }
  }

  /** Delete an entity right for a user. */
  @DeleteMapping("{username}/editorRight/{key}")
  @Secured(ADMIN_ROLE)
  public ResponseEntity<Void> deleteEditorRight(
      @PathVariable String username, @PathVariable UUID key) {

    // Ensure user exists
    GbifUser currentUser = identityService.get(username);
    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    if (!identityService.listEditorRights(username).contains(key)) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    } else {
      identityService.deleteEditorRight(username, key);
      return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
  }

  /** List the namespace rights for a user. */
  @GetMapping("{username}/namespaceRight")
  @Secured({ADMIN_ROLE, USER_ROLE})
  public ResponseEntity<List<String>> namespaceRights(
      @PathVariable String username, Authentication authentication) {
    // Non-admin users can only see their own entry.
    if (!SecurityContextCheck.checkUserInRole(authentication, ADMIN_ROLE)) {
      String usernameInContext = authentication.getName();
      if (!usernameInContext.equals(username)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
      }
    }

    // Ensure user exists
    GbifUser currentUser = identityService.get(username);
    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    List<String> rights = identityService.listNamespaceRights(username);
    return ResponseEntity.ok(rights);
  }

  /** Add a namespace right for a user. */
  @PostMapping(
      path = "{username}/namespaceRight",
      consumes = {MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Secured({ADMIN_ROLE})
  public ResponseEntity<String> addNamespaceRight(
      @PathVariable String username, @RequestBody @NotNull String namespace) {

    // Ensure user exists
    GbifUser currentUser = identityService.get(username);
    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    if (identityService.listNamespaceRights(username).contains(namespace)) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    } else {
      identityService.addNamespaceRight(username, namespace);
      return ResponseEntity.ok(namespace);
    }
  }

  /** Delete a namespace right for a user. */
  @DeleteMapping("{username}/namespaceRight/{namespace}")
  @Secured(ADMIN_ROLE)
  public ResponseEntity<Void> deleteNamespaceRight(
      @PathVariable String username, @PathVariable String namespace) {

    // Ensure user exists
    GbifUser currentUser = identityService.get(username);
    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    if (!identityService.listNamespaceRights(username).contains(namespace)) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    } else {
      identityService.deleteNamespaceRight(username, namespace);
      return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
  }

  /** List the namespace rights for a user. */
  @GetMapping("{username}/countryRight")
  @Secured({ADMIN_ROLE, USER_ROLE})
  public ResponseEntity<List<Country>> countryRights(
      @PathVariable String username, Authentication authentication) {
    // Non-admin users can only see their own entry.
    if (!SecurityContextCheck.checkUserInRole(authentication, ADMIN_ROLE)) {
      String usernameInContext = authentication.getName();
      if (!usernameInContext.equals(username)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
      }
    }

    // Ensure user exists
    GbifUser currentUser = identityService.get(username);
    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    List<Country> rights = identityService.listCountryRights(username);
    return ResponseEntity.ok(rights);
  }

  /** Add a country right for a user. */
  @PostMapping(
      path = "{username}/countryRight",
      consumes = {MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Secured({ADMIN_ROLE})
  public ResponseEntity<String> addCountryRight(
      @PathVariable String username, @RequestBody @NotNull String countryParam) {

    final Country country = Country.fromIsoCode(countryParam);

    if (country == null) {
      throw new IllegalArgumentException("Country not found: " + countryParam);
    }

    // Ensure user exists
    GbifUser currentUser = identityService.get(username);
    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    if (identityService.listCountryRights(username).contains(country)) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    } else {
      identityService.addCountryRight(username, country);
      return ResponseEntity.ok(country.getIso2LetterCode());
    }
  }

  /** Delete a country right for a user. */
  @DeleteMapping("{username}/countryRight/{countryParam}")
  @Secured(ADMIN_ROLE)
  public ResponseEntity<Void> deleteCountryRight(
      @PathVariable String username, @PathVariable String countryParam) {

    final Country country = Country.fromIsoCode(countryParam);

    if (country == null) {
      throw new IllegalArgumentException("Country not found: " + countryParam);
    }

    // Ensure user exists
    GbifUser currentUser = identityService.get(username);
    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    if (!identityService.listCountryRights(username).contains(country)) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    } else {
      identityService.deleteCountryRight(username, country);
      return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
  }

  /**
   * Changes the user email only if the token presented is valid for the user account. The username
   * is expected to be present in the security context (authenticated by appkey).
   */
  @Secured({USER_ROLE})
  @PutMapping(path = "changeEmail", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<UserModelMutationResult> changeEmail(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
      Authentication authentication,
      @RequestBody @NotNull @Valid EmailChangeRequest request) {
    // we ONLY accept user impersonation, and only from a trusted app key.
    SecurityContextCheck.ensureAuthorizedUserImpersonation(
        authentication, authHeader, appKeyWhitelist);

    final String identifier = authentication.getName();
    final GbifUser user = identityService.get(identifier);
    if (user != null) {
      UserModelMutationResult result =
          identityService.updateEmail(
              user.getKey(), user.getEmail(), request.getEmail(), request.getChallengeCode());
      if (result.containsError()) {
        return ResponseEntity.unprocessableEntity().body(result);
      }
    }
    return ResponseEntity.noContent().build();
  }
}
