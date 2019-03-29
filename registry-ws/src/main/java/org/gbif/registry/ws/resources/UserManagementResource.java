package org.gbif.registry.ws.resources;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.ConfirmationKeyParameter;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.service.common.LoggedUser;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.identity.model.PropertyConstants;
import org.gbif.identity.model.UserModelMutationResult;
import org.gbif.registry.ws.model.AuthenticationDataParameters;
import org.gbif.registry.ws.model.UserAdminView;
import org.gbif.registry.ws.model.UserCreation;
import org.gbif.registry.ws.model.UserUpdate;
import org.gbif.registry.ws.security.SecurityContextCheck;
import org.gbif.registry.ws.security.UserUpdateRulesManager;
import org.gbif.registry.ws.util.ResponseUtils;
import org.gbif.utils.AnnotationUtils;
import org.gbif.ws.response.GbifResponseStatus;
import org.gbif.ws.util.ExtraMediaTypes;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.mybatis.guice.transactional.Transactional;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.APP_ROLE;
import static org.gbif.registry.ws.security.UserRoles.USER_ROLE;
import static org.gbif.registry.ws.util.ResponseUtils.buildResponse;
import static org.gbif.ws.server.filter.AppIdentityFilter.APPKEYS_WHITELIST;

/**
 * The "/admin/user" resource represents the "endpoints" related to user management. This means the resource
 * it expected to be called by another application (mostly the Registry Console and the portal backend).
 *
 * Design and implementation decisions:
 * - This resource contains mostly the routing to the business logic ({@link IdentityService}) including
 *   authorizations. This resource does NOT implement the service but aggregates it (by Dependency Injection).
 * - Methods can return {@link Response} instead of object to minimize usage of exceptions and provide
 *   better control over the HTTP code returned. This also allows to return an entity in case
 *   of errors (e.g. {@link UserModelMutationResult}
 * - keys (user id) are not considered public, therefore the username is used as key
 * - In order to strictly control the data that is exposed this class uses "view models" (e.g. {@link UserAdminView})
 *
 * Please note there is 3 possible ways to be authenticated:
 * - HTTP Basic authentication
 * - User impersonation using appKey. ALL applications with a valid appKey can impersonate a user.
 * - Application itself (APP_ROLE). All applications with a valid appKey that is also present in the appKey whitelist.
 * See {@link org.gbif.ws.server.filter.AppIdentityFilter}.
 */
@Path("admin/user")
@Produces({MediaType.APPLICATION_JSON, ExtraMediaTypes.APPLICATION_JAVASCRIPT})
@Consumes(MediaType.APPLICATION_JSON)
@Singleton
public class UserManagementResource {

  //filters roles that are deprecated
  private static final List<UserRole> USER_ROLES = Arrays.stream(UserRole.values()).filter(r ->
          !AnnotationUtils.isFieldDeprecated(UserRole.class, r.name())).collect(Collectors.toList());

  private final IdentityService identityService;
  private final List<String> appKeyWhitelist;

  /**
   * {@link UserManagementResource} main constructor.
   * @param identityService
   * @param appKeyWhitelist list of authorized appkeys. Used to determine if user impersonation can be trusted.
   */
  @Inject
  public UserManagementResource(IdentityService identityService,
                                @Named(APPKEYS_WHITELIST) List<String> appKeyWhitelist) {
    this.identityService = identityService;
    this.appKeyWhitelist = appKeyWhitelist;
  }

  @GET
  @Path("/roles")
  public List<UserRole> listRoles() {
    return USER_ROLES;
  }

  /**
   * GET a {@link UserAdminView} of a user.
   * Mostly for admin console and access by authorized appkey (e.g. portal nodejs backend).
   * Returns the identified user account.
   * @return the {@link UserAdminView} or null
   */
  @GET
  @RolesAllowed({ADMIN_ROLE, APP_ROLE})
  @Path("/{username}")
  public UserAdminView getUser(@PathParam("username") String username, @Context SecurityContext securityContext,
                               @Context HttpServletRequest request) {

    GbifUser user = identityService.get(username);
    if(user == null) {
      return null;
    }
    return new UserAdminView(user, identityService.hasPendingConfirmation(user.getKey()));
  }

  @GET
  @RolesAllowed({ADMIN_ROLE, APP_ROLE})
  @Path("/find")
  public UserAdminView getUserBySystemSetting(@Context HttpServletRequest request) {
    GbifUser user = null;
    if(request.getParameterNames().hasMoreElements()){
      String paramName = request.getParameterNames().nextElement();
      user = identityService.getBySystemSetting(paramName, request.getParameter(paramName));
    }

    if(user == null) {
      return null;
    }
    return new UserAdminView(user, identityService.hasPendingConfirmation(user.getKey()));
  }

  /**
   * Creates a new user. (only available to the portal backend).
   *
   * @param securityContext
   * @param request
   * @param user
   * @return
   */
  @POST
  @RolesAllowed(APP_ROLE)
  @Path("/")
  public Response create(@Context SecurityContext securityContext, @Context HttpServletRequest request, UserCreation user) {

    int returnStatusCode = Response.Status.CREATED.getStatusCode();
    UserModelMutationResult result = identityService.create(
            UserUpdateRulesManager.applyCreate(user), user.getPassword());
    if(result.containsError()) {
      returnStatusCode = GbifResponseStatus.UNPROCESSABLE_ENTITY.getStatus();
    }
    return buildResponse(returnStatusCode, result);
  }

  /**
   * Updates a user. Available to admin-console and portal backend.
   * {@link UserUpdateRulesManager} will be used to determine which properties it is possible to update based on the role,
   * all other properties will be ignored.
   *
   * At the moment, a user cannot update its own data calling the API directly using HTTP Basic auth.
   * If this is required/wanted, it would go in {@link UserResource} to only accept the role USER and ensure
   * a user can only update its own data.
   *
   * @param username
   * @param userUpdate
   * @param securityContext
   * @param request
   * @return
   */
  @PUT
  @RolesAllowed({ADMIN_ROLE, APP_ROLE})
  @Path("/{username}")
  public Response update(@PathParam("username") String username, UserUpdate userUpdate,
                         @Context SecurityContext securityContext, @Context HttpServletRequest request) {

    Response response = Response.noContent().build();
    //ensure the key used to access the update is actually the one of the user represented by the UserUpdate
    GbifUser currentUser = identityService.get(username);
    if(currentUser == null || !currentUser.getUserName().equals(userUpdate.getUserName())) {
      response = buildResponse(Response.Status.BAD_REQUEST);
    }
    else{
      GbifUser updateInitiator = securityContext.getUserPrincipal() == null ? null :
              identityService.get(securityContext.getUserPrincipal().getName());

      UserModelMutationResult result = identityService.update(UserUpdateRulesManager.applyUpdate(
              updateInitiator == null ? null : updateInitiator.getRoles(), currentUser, userUpdate,
              securityContext.isUserInRole(APP_ROLE)));
      if(result.containsError()) {
        response = buildResponse(GbifResponseStatus.UNPROCESSABLE_ENTITY.getStatus(), result);
      }
    }
    return response;
  }

  /**
   * Confirm a confirmationKey for a specific user.
   * The username is expected to be present in the security context (authenticated by appkey).
   *
   * @param securityContext
   * @param request
   * @param confirmationKeyParameter
   *
   * @return
   */
  @POST
  @RolesAllowed(USER_ROLE)
  @Path("/confirm")
  @Transactional
  public Response confirmChallengeCode(@Context SecurityContext securityContext, @Context HttpServletRequest request,
                                       @NotNull @Valid ConfirmationKeyParameter confirmationKeyParameter) {

    // we ONLY accept user impersonation, and only from a trusted app key.
    SecurityContextCheck.ensureAuthorizedUserImpersonation(securityContext, request, appKeyWhitelist);

    GbifUser user = identityService.get(securityContext.getUserPrincipal().getName());
    if(user != null && identityService.confirmUser(user.getKey(), confirmationKeyParameter.getConfirmationKey())){
      identityService.updateLastLogin(user.getKey());

      //ideally we would return 200 OK but CreatedResponseFilter automatically
      //change it to 201 CREATED
      return buildResponse(Response.Status.CREATED, LoggedUser.from(user));
    }
    return Response.status(Response.Status.BAD_REQUEST).build();
  }

  /**
   * For admin console only.
   * Relax content-type to wildcard to allow angularjs.
   */
  @DELETE
  @RolesAllowed(ADMIN_ROLE)
  @Consumes(MediaType.WILDCARD)
  @Path("/{userKey}")
  public Response delete(@PathParam("userKey") int userKey) {
    identityService.delete(userKey);
    return Response.noContent().build();
  }

  /**
   * For admin console only.
   * User search, intended for user administration console use only.
   */
  @GET
  @Path("/search")
  @RolesAllowed(ADMIN_ROLE)
  public PagingResponse<GbifUser> search(@QueryParam("q") String query, @Context @Nullable Pageable page) {
    page = page == null ? new PagingRequest() : page;
    String q = Optional.ofNullable(query).map(v -> Strings.nullToEmpty(CharMatcher.WHITESPACE.trimFrom(v))).orElse(null);
    return identityService.search(q, page);
  }

  /**
   * A user requesting his password to be reset.
   * The username is expected to be present in the security context (authenticated by appkey).
   * This method will always return 204 No Content.
   */
  @POST
  @RolesAllowed(USER_ROLE)
  @Path("/resetPassword")
  public Response resetPassword(@Context SecurityContext securityContext, @Context HttpServletRequest request) {

    // we ONLY accept user impersonation, and only from a trusted app key.
    SecurityContextCheck.ensureAuthorizedUserImpersonation(securityContext, request, appKeyWhitelist);

    String identifier = securityContext.getUserPrincipal().getName();
    GbifUser user = identityService.get(identifier);
    if (user != null) {
      // initiate mail, and store the challenge etc.
      identityService.resetPassword(user.getKey());
    }
    return Response.noContent().build();
  }

  /**
   * Updates the user password only if the token presented is valid for the user account.
   * The username is expected to be present in the security context (authenticated by appkey).
   */
  @POST
  @RolesAllowed(USER_ROLE)
  @Path("/updatePassword")
  @Transactional
  public Response updatePassword(@Context SecurityContext securityContext, @Context HttpServletRequest request,
                                 AuthenticationDataParameters authenticationDataParameters) {

    // we ONLY accept user impersonation, and only from a trusted app key.
    SecurityContextCheck.ensureAuthorizedUserImpersonation(securityContext, request, appKeyWhitelist);

    String username = securityContext.getUserPrincipal().getName();
    GbifUser user = identityService.get(username);

    UserModelMutationResult updatePasswordMutationResult = identityService.updatePassword(user.getKey(),
            authenticationDataParameters.getPassword(), authenticationDataParameters.getChallengeCode());

    if(!updatePasswordMutationResult.containsError()){
      identityService.updateLastLogin(user.getKey());
      return Response.ok().entity(LoggedUser.from(user)).build();
    }

    //determine if it's a challengeCode error
    boolean challengeCodeError = updatePasswordMutationResult.getConstraintViolation().keySet()
            .stream()
            .anyMatch(s -> s.equalsIgnoreCase(PropertyConstants.CHALLENGE_CODE_PROPERTY_NAME));

    return challengeCodeError ? ResponseUtils.buildResponse(Response.Status.UNAUTHORIZED) :
            ResponseUtils.buildResponse(GbifResponseStatus.UNPROCESSABLE_ENTITY.getStatus(), updatePasswordMutationResult);
  }

  /**
   * Utility to determine if the challengeCode provided is valid for the given user.
   * The username is expected to be present in the security context (authenticated by appkey).
   * @param confirmationKey To check
   */
  @GET
  @RolesAllowed(USER_ROLE)
  @Path("/confirmationKeyValid")
  public Response tokenValidityCheck(@Context SecurityContext securityContext, @Context HttpServletRequest request,
                                     @QueryParam("confirmationKey") UUID confirmationKey) {

    // we ONLY accept user impersonation, and only from a trusted app key.
    SecurityContextCheck.ensureAuthorizedUserImpersonation(securityContext, request, appKeyWhitelist);

    String username = securityContext.getUserPrincipal().getName();
    GbifUser user = identityService.get(username);

    if(identityService.isConfirmationKeyValid(user.getKey(), confirmationKey)) {
      return Response.noContent().build();
    }
    return buildResponse(Response.Status.UNAUTHORIZED);
  }

  /**
   * List the editor rights for a user.
   */
  @GET
  @RolesAllowed({ADMIN_ROLE, USER_ROLE})
  @Path("/{username}/editorRight")
  public Response editorRights(@PathParam("username") String username,
                                 @Context SecurityContext securityContext, @Context HttpServletRequest request) {
    // Non-admin users can only see their own entry.
    if (!SecurityContextCheck.checkUserInRole(securityContext, ADMIN_ROLE)) {
      if (!securityContext.getUserPrincipal().getName().equals(username)) {
        return ResponseUtils.buildResponse(Response.Status.UNAUTHORIZED);
      }
    }

    // Ensure user exists
    GbifUser currentUser = identityService.get(username);
    if (currentUser == null) {
      return buildResponse(Response.Status.NOT_FOUND);
    }

    List<UUID> rights = identityService.listEditorRights(username);
    return Response.ok(rights).build();
  }

  /**
   * Add an entity right for a user.
   */
  @POST
  @RolesAllowed({ADMIN_ROLE})
  @Consumes({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
  @Path("/{username}/editorRight")
  public Response addEditorRight(@PathParam("username") String username, UUID key,
                             @Context SecurityContext securityContext, @Context HttpServletRequest request) {

    // Ensure user exists
    GbifUser currentUser = identityService.get(username);
    if (currentUser == null) {
      return buildResponse(Response.Status.NOT_FOUND);
    }

    if (identityService.listEditorRights(username).contains(key)) {
      return buildResponse(Response.Status.CONFLICT);
    } else {
      identityService.addEditorRight(username, key);
      return Response.ok(key).build();
    }
  }

  /**
   * Delete an entity right for a user.
   */
  @DELETE
  @RolesAllowed(ADMIN_ROLE)
  @Path("/{username}/editorRight/{key}")
  public Response deleteEditorRight(@PathParam("username") String username, @PathParam("key") UUID key,
                                @Context SecurityContext securityContext, @Context HttpServletRequest request) {

    // Ensure user exists
    GbifUser currentUser = identityService.get(username);
    if (currentUser == null) {
      return buildResponse(Response.Status.NOT_FOUND);
    }

    if (!identityService.listEditorRights(username).contains(key)) {
      return buildResponse(Response.Status.NOT_FOUND);
    } else {
      identityService.deleteEditorRight(username, key);
      return Response.noContent().build();
    }
  }
}
