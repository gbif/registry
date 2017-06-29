package org.gbif.registry.ws.resources;

import org.gbif.api.model.common.User;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.service.common.LoggedUser;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.identity.model.PropertyConstants;
import org.gbif.identity.model.UserModelMutationResult;
import org.gbif.identity.service.IdentityServiceModule;
import org.gbif.registry.ws.model.AuthenticationDataParameters;
import org.gbif.registry.ws.model.UserAdminView;
import org.gbif.registry.ws.model.UserCreation;
import org.gbif.registry.ws.model.UserUpdate;
import org.gbif.registry.ws.security.UpdateRulesManager;
import org.gbif.registry.ws.util.ResponseUtils;
import org.gbif.utils.AnnotationUtils;
import org.gbif.ws.response.GbifResponseStatus;
import org.gbif.ws.security.GbifAuthService;
import org.gbif.ws.util.ExtraMediaTypes;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.registry.ws.filter.AppIdentityFilter.GBIF_SCHEME_APP_ROLE;
import static org.gbif.registry.ws.resources.Authentications.ensureUserSetInSecurityContext;
import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.USER_ROLE;
import static org.gbif.registry.ws.util.ResponseUtils.buildResponse;

/**
 * Web layer relating to user management.
 * Mostly used by the Registry Console and the portal backend.
 *
 * Design and implementation decisions:
 * - This resource contains mostly to routing to the business logic ({@link IdentityService}) including
 *   authorizations
 * - Return {@link Response} instead of object to minimize usage of exceptions and provide
 *   better control over the HTTP code returned. This also allows to return an entity in case
 *   of errors (e.g. {@link UserModelMutationResult}.
 * - keys (user id) are not considered public, therefore the username is used as key
 * - In order to strictly control the data that is exposed this class uses "view models" (e.g. {@link LoggedUser}).
 */
@Path("admin/user")
@Produces({MediaType.APPLICATION_JSON, ExtraMediaTypes.APPLICATION_JAVASCRIPT})
@Consumes(MediaType.APPLICATION_JSON)
@Singleton
public class UserManagementResource {

  private static final Logger LOG = LoggerFactory.getLogger(UserResource.class);

  //filters roles that are deprecated
  private static final List<UserRole> USER_ROLES = Arrays.stream(UserRole.values()).filter(r ->
          !AnnotationUtils.isFieldDeprecated(UserRole.class, r.name())).collect(Collectors.toList());

  private final IdentityService identityService;
  private final List<String> appKeyWhitelist;

  /**
   *
   * @param identityService
   * @param appKeyWhitelist list of appkeys that are allowed to use this resource.
   */
  @Inject
  public UserManagementResource(IdentityService identityService, @Named(IdentityServiceModule.APPKEYS_WHITELIST) List<String> appKeyWhitelist) {
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
   * Mostly for admin console and access by authorized appkey (e.g. portal node backend).
   * Returns the identified user account.
   * @return the {@link UserAdminView} or null
   */
  @GET
  @RolesAllowed({ADMIN_ROLE, GBIF_SCHEME_APP_ROLE})
  @Path("/{username}")
  public UserAdminView getUser(@PathParam("username") String username, @Context SecurityContext securityContext,
                               @Context HttpServletRequest request) {

    //if we are logged in by app key ensure it is a trusted one
    ensureIsTrustedApp(securityContext, request, true);

    User user = identityService.get(username);
    if(user == null) {
      return null;
    }
    return new UserAdminView(user, identityService.containsChallengeCode(user.getKey()));
  }

  @POST
  @RolesAllowed({GBIF_SCHEME_APP_ROLE})
  @Path("/")
  public Response create(@Context SecurityContext securityContext, @Context HttpServletRequest request, UserCreation user) {

    ensureIsTrustedApp(securityContext, request, false);

    int returnStatusCode = Response.Status.CREATED.getStatusCode();
    UserModelMutationResult result = identityService.create(
            UpdateRulesManager.applyCreate(user), user.getPassword());
    if(result.containsError()) {
      returnStatusCode = GbifResponseStatus.UNPROCESSABLE_ENTITY.getStatus();
    }
    return buildResponse(returnStatusCode, result);
  }

  @PUT
  @RolesAllowed({ADMIN_ROLE, GBIF_SCHEME_APP_ROLE})
  @Path("/{username}")
  public Response update(@PathParam("username") String username, UserUpdate userUpdate, @Context SecurityContext securityContext,
                         @Context HttpServletRequest request) {

    //if we are logged in by app key ensure it is a trusted one
    ensureIsTrustedApp(securityContext, request, true);
    boolean requestFromTrustedApp = securityContext.getUserPrincipal() == null;

    Response response = Response.noContent().build();
    //ensure the key used to access the update is actually the one of the user represented by the UserUpdate
    User currentUser = identityService.get(username);
    if(currentUser == null || !currentUser.getUserName().equals(userUpdate.getUserName())) {
      response = buildResponse(Response.Status.BAD_REQUEST);
    }
    else{
      User updateInitiator = securityContext.getUserPrincipal() == null ? null :
              identityService.get(securityContext.getUserPrincipal().getName());

      UserModelMutationResult result = identityService.update(UpdateRulesManager.applyUpdate(
              updateInitiator == null ? null : updateInitiator.getRoles(), currentUser, userUpdate, requestFromTrustedApp));
      if(result.containsError()) {
        response = buildResponse(GbifResponseStatus.UNPROCESSABLE_ENTITY.getStatus(), result);
      }
    }
    return response;
  }

  /**
   * Confirm a challengeCode for a specific user.
   * The username is taken from the securityContext.
   *
   * @param securityContext
   * @param request
   * @param authenticationDataParameters
   *
   * @return
   */
  @POST
  @RolesAllowed({USER_ROLE})
  @Path("/confirm")
  @Transactional
  public Response confirmChallengeCode(@Context SecurityContext securityContext, @Context HttpServletRequest request,
                                       AuthenticationDataParameters authenticationDataParameters) {

    ensureIsTrustedApp(securityContext, request, true);
    ensureUserSetInSecurityContext(securityContext);

    User user = identityService.get(securityContext.getUserPrincipal().getName());
    if(user != null && identityService.confirmChallengeCode(user.getKey(), authenticationDataParameters.getChallengeCode())){
      identityService.updateLastLogin(user.getKey());

      //ideally we would return 200 OK but CreatedResponseFilter automatically
      //change it to 201 CREATED
      return buildResponse(Response.Status.CREATED, LoggedUser.from(user));
    }
    return Response.status(Response.Status.BAD_REQUEST).build();
  }

  /**
   * For admin console
   * Relax content-type to wildcard to allow angularjs.
   */
  @DELETE
  @RolesAllowed({ADMIN_ROLE})
  @Consumes(MediaType.WILDCARD)
  @Path("/{userKey}")
  public Response delete(@PathParam("userKey") int userKey) {
    identityService.delete(userKey);
    return Response.noContent().build();
  }

  /**
   * User search, intended for user administration console use only.
   */
  @GET
  @Path("/search")
  @RolesAllowed({ADMIN_ROLE})
  public PagingResponse<User> search(@QueryParam("q") String query, @Context @Nullable Pageable page) {
    page = page == null ? new PagingRequest() : page;
    String q = Strings.nullToEmpty(CharMatcher.WHITESPACE.trimFrom(query));
    return identityService.search(q, page);
  }

  /**
   *
   */
  @POST
  @RolesAllowed({USER_ROLE})
  @Path("/resetPassword")
  public Response resetPassword(@Context SecurityContext securityContext, @Context HttpServletRequest request) {

    ensureIsTrustedApp(securityContext, request, true);
    ensureUserSetInSecurityContext(securityContext);

    String identifier= securityContext.getUserPrincipal().getName();
    User user = Optional.ofNullable(identityService.get(identifier))
            .orElse(identityService.getByEmail(identifier));
    if (user != null) {
      // initiate mail, and store the challenge etc.
      identityService.resetPassword(user.getKey());
    }
    return Response.noContent().build();
  }

  /**
   * Updates the user password only if the token presented is valid for the user account.
   */
  @POST
  @RolesAllowed({USER_ROLE})
  @Path("/updatePassword")
  @Transactional
  public Response updatePassword(@Context SecurityContext securityContext, @Context HttpServletRequest request,
                                 AuthenticationDataParameters authenticationDataParameters) {

    ensureIsTrustedApp(securityContext, request, true);
    ensureUserSetInSecurityContext(securityContext);

    String username = securityContext.getUserPrincipal().getName();
    User user = identityService.get(username);

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
   * @param challengeCode To check
   */
  @GET
  @RolesAllowed({USER_ROLE})
  @Path("/challengeCodeValid")
  public Response tokenValidityCheck(@Context SecurityContext securityContext, @Context HttpServletRequest request,
                                     @QueryParam("challengeCode") UUID challengeCode) {

    ensureIsTrustedApp(securityContext, request, true);
    ensureUserSetInSecurityContext(securityContext);

    String username = securityContext.getUserPrincipal().getName();
    User user = identityService.get(username);

    if(identityService.isChallengeCodeValid(user.getKey(), challengeCode)) {
      return Response.noContent().build();
    }
    return buildResponse(Response.Status.UNAUTHORIZED);
  }

  /**
   * Check if the {@link SecurityContext} was obtained by the GBIF Authenticated scheme AND the appkey is
   * in our whitelist.
   * @param security
   * @param request
   * @throws WebApplicationException FORBIDDEN if the request is not coming from a trusted application
   */
  private void ensureIsTrustedApp(SecurityContext security, HttpServletRequest request, boolean allowOtherScheme) {
    boolean isGbifScheme = GbifAuthService.GBIF_SCHEME.equals(security.getAuthenticationScheme());

    //first check if we have something to check
    if(!isGbifScheme && allowOtherScheme){
      return;
    }

    //ensure the appkey is allowed
    if (isGbifScheme
            && appKeyWhitelist.contains(GbifAuthService.getAppKeyFromRequest(request::getHeader))) {
      return;
    }
    throw new WebApplicationException(Response.Status.FORBIDDEN);
  }

}
