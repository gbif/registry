package org.gbif.registry.ws.resources;

import org.gbif.api.model.common.User;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.service.common.LoggedUser;
import org.gbif.identity.model.UserModelMutationResult;
import org.gbif.registry.ws.model.AuthenticationDataParameters;
import org.gbif.ws.response.GbifResponseStatus;
import org.gbif.ws.util.ExtraMediaTypes;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.registry.ws.resources.Authentications.ensureUserSetInSecurityContext;
import static org.gbif.registry.ws.security.UserRoles.USER_ROLE;
import static org.gbif.registry.ws.util.ResponseUtils.buildResponse;

/**
 * Web layer relating to authentication.
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
@Path("user")
@Produces({MediaType.APPLICATION_JSON, ExtraMediaTypes.APPLICATION_JAVASCRIPT})
@Consumes(MediaType.APPLICATION_JSON)
@Singleton
public class UserResource {
  private static final Logger LOG = LoggerFactory.getLogger(UserResource.class);

  private final IdentityService identityService;

  /**
   *
   * @param identityService
   */
  @Inject
  public UserResource(IdentityService identityService) {
    this.identityService = identityService;
  }

  /**
   * Returns the authenticated user.
   * @return the user
   */
  @GET
  @RolesAllowed({USER_ROLE})
  @Path("/")
  public LoggedUser getAuthenticatedUser(@Context SecurityContext security, @Context HttpServletResponse response) {
    response.addHeader("Access-Control-Allow-Credentials", "true");
    return LoggedUser.from(identityService.get(security.getUserPrincipal().getName()));
  }

  /**
   *
   * @return the user and a session token as {@link LoggedUser}
   */
  @GET
  @Path("/login")
  public LoggedUser login(@Context SecurityContext security, @Context HttpServletRequest request) {

    ensureUserSetInSecurityContext(security);

    User user = identityService.get(security.getUserPrincipal().getName());
    identityService.updateLastLogin(user.getKey());
    return LoggedUser.from(user);
  }

  @PUT
  @RolesAllowed({USER_ROLE})
  @Path("/changePassword")
  public Response changePassword(@Context SecurityContext securityContext,
                                 AuthenticationDataParameters authenticationDataParameters) {

    ensureUserSetInSecurityContext(securityContext);

    String identifier = securityContext.getUserPrincipal().getName();
    User user = identityService.get(identifier);
    if (user != null) {
      UserModelMutationResult updatePasswordMutationResult = identityService.updatePassword(user.getKey(),
              authenticationDataParameters.getPassword());
      if(updatePasswordMutationResult.containsError()) {
        return buildResponse(GbifResponseStatus.UNPROCESSABLE_ENTITY.getStatus(), updatePasswordMutationResult);
      }
    }
    return Response.noContent().build();
  }

}
