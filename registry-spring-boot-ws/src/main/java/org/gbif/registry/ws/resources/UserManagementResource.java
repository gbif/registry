package org.gbif.registry.ws.resources;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.registry.ConfirmationKeyParameter;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.service.common.LoggedUser;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.identity.model.UserModelMutationResult;
import org.gbif.registry.ws.model.UserAdminView;
import org.gbif.registry.ws.model.UserCreation;
import org.gbif.registry.ws.model.UserUpdate;
import org.gbif.registry.ws.security.UserUpdateRulesManager;
import org.gbif.utils.AnnotationUtils;
import org.gbif.ws.security.GbifUserPrincipal;
import org.gbif.ws.server.filter.AppIdentityFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.APP_ROLE;
import static org.gbif.registry.ws.security.UserRoles.USER_ROLE;

// TODO: 2019-07-03 revise all comments
/**
 * The "/admin/user" resource represents the "endpoints" related to user management. This means the resource
 * it expected to be called by another application (mostly the Registry Console and the portal backend).
 * <p>
 * Design and implementation decisions:
 * - This resource contains mostly the routing to the business logic ({@link IdentityService}) including
 * authorizations. This resource does NOT implement the service but aggregates it (by Dependency Injection).
 * - Methods can return {@link ResponseEntity} instead of object to minimize usage of exceptions and provide
 * better control over the HTTP code returned. This also allows to return an entity in case
 * of errors (e.g. {@link UserModelMutationResult}
 * - keys (user id) are not considered public, therefore the username is used as key
 * - In order to strictly control the data that is exposed this class uses "view models" (e.g. {@link UserAdminView})
 * <p>
 * Please note there is 3 possible ways to be authenticated:
 * - HTTP Basic authentication
 * - User impersonation using appKey. ALL applications with a valid appKey can impersonate a user.
 * - Application itself (APP_ROLE). All applications with a valid appKey that is also present in the appKey whitelist.
 * See {@link AppIdentityFilter}.
 */
// TODO: 2019-07-03 where 'Response' is expected what will happen if it gets 'ResponseEntity' instead? (especially in case of exception)
// TODO: 2019-07-03 do not forget ResponseBody\RequestBody
// TODO: 2019-06-14 add produce ExtraMediaTypes.APPLICATION_JAVASCRIPT
// TODO: 2019-07-02 for all resources:
// 1) replace SecurityContext + @Context + @RolesAllowed (remove javax-annotation-api?)
// 2) replace Response (doesn't it break contracts?)
// 3) use Spring Transactional(?). make sure it's working properly
@RestController
@RequestMapping("/admin/user")
public class UserManagementResource {

  //filters roles that are deprecated
  private static final List<UserRole> USER_ROLES = Arrays.stream(UserRole.values()).filter(r ->
      !AnnotationUtils.isFieldDeprecated(UserRole.class, r.name())).collect(Collectors.toList());

  private final IdentityService identityService;

  // TODO: 2019-07-02 find good way to inject these properties (empty for now)
//  @Named(APPKEYS_WHITELIST)
  private final List<String> appKeyWhitelist = Collections.emptyList();

  /**
   * {@link UserManagementResource} main constructor.
   *
   * @param identityService
   */
  public UserManagementResource(IdentityService identityService) {
    this.identityService = identityService;
  }

  @GetMapping("roles")
  public List<UserRole> listRoles() {
    return USER_ROLES;
  }

  /**
   * Creates a new user. (only available to the portal backend).
   *
   * @param user
   * @return
   */
  // TODO: 2019-07-12 tried to create an admin with role REGISTRY_ADMIN, but it was created with USER role ???
  // TODO: 2019-07-11 add APP_ROLE security
//  @Secured(APP_ROLE)
  @RequestMapping(method = RequestMethod.POST)
  @ResponseBody
  public ResponseEntity<UserModelMutationResult> create(@RequestBody UserCreation user) {

    int returnStatusCode = HttpStatus.CREATED.value();
    UserModelMutationResult result = identityService.create(
        UserUpdateRulesManager.applyCreate(user), user.getPassword());
    if (result.containsError()) {
      returnStatusCode = HttpStatus.UNPROCESSABLE_ENTITY.value();
    }
    return ResponseEntity.status(returnStatusCode).body(result);
  }

  // TODO: 2019-07-10 mb instead of parameter use explicit SecurityContextHolder
  // TODO: 2019-07-10 check for admin and app roles
  /**
   * Updates a user. Available to admin-console and portal backend.
   * {@link UserUpdateRulesManager} will be used to determine which properties it is possible to update based on the role,
   * all other properties will be ignored.
   * <p>
   * At the moment, a user cannot update its own data calling the API directly using HTTP Basic auth.
   * If this is required/wanted, it would go in {@link UserResource} to only accept the role USER and ensure
   * a user can only update its own data.
   *
   * @param username
   * @param userUpdate
   * @return
   */
  @Secured({ADMIN_ROLE, APP_ROLE})
  @PutMapping("/{username}")
  public ResponseEntity<UserModelMutationResult> update(
      @PathVariable("username") String username,
      @RequestBody UserUpdate userUpdate,
      Authentication authentication) {

    ResponseEntity<UserModelMutationResult> response = ResponseEntity.noContent().build();
    //ensure the key used to access the update is actually the one of the user represented by the UserUpdate
    GbifUser currentUser = identityService.get(username);
    if (currentUser == null || !currentUser.getUserName().equals(userUpdate.getUserName())) {
      response = ResponseEntity.badRequest().build();
    } else {
      GbifUser updateInitiator = authentication == null ? null
          : identityService.get(((GbifUserPrincipal) authentication.getPrincipal()).getUsername());

      UserModelMutationResult result = identityService.update(
          UserUpdateRulesManager.applyUpdate(
              updateInitiator == null ? null : updateInitiator.getRoles(),
              currentUser,
              userUpdate,
              authentication != null &&
              authentication.getAuthorities() != null &&
              authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_APP"))));

      if (result.containsError()) {
        response = ResponseEntity.unprocessableEntity().body(result);
      }
    }
    return response;
  }

  /**
   * Confirm a confirmationKey for a specific user.
   * The username is expected to be present in the security context (authenticated by appkey).
   *
   * @param confirmationKeyParameter confirmation key (UUID)
   * @return logged user data
   */
  @Secured(USER_ROLE)
  @PostMapping("/confirm")
  @Transactional
  @ResponseBody
  public ResponseEntity<LoggedUser> confirmChallengeCode(
      Authentication authentication,
      @RequestHeader("Authorization") String authHeader,
      @NotNull @Valid @RequestBody ConfirmationKeyParameter confirmationKeyParameter) {

    // TODO: 2019-07-11 figure out where should 'GBIF' authorization header come from
    // we ONLY accept user impersonation, and only from a trusted app key.
//    SecurityContextCheck.ensureAuthorizedUserImpersonation(authentication, authHeader, appKeyWhitelist);

    GbifUser user = identityService.get(authentication.getName());
    if (user != null && identityService.confirmUser(user.getKey(), confirmationKeyParameter.getConfirmationKey())) {
      identityService.updateLastLogin(user.getKey());

      // TODO: 2019-07-11 check CreatedResponseFilter
      // ideally we would return 200 OK but CreatedResponseFilter automatically change it to 201 CREATED
      return ResponseEntity.status(HttpStatus.CREATED).body(LoggedUser.from(user));
    }
    return ResponseEntity.badRequest().build();
  }

  /**
   * For admin console only.
   * Relax content-type to wildcard to allow angularjs.
   */
  @Secured(ADMIN_ROLE)
  @DeleteMapping(value = "/{userKey}", consumes = MediaType.ALL_VALUE)
  public ResponseEntity<Void> delete(@PathVariable("userKey") int userKey) {
    identityService.delete(userKey);
    return ResponseEntity.noContent().build();
  }
}
