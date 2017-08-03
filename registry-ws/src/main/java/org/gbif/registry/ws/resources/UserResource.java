package org.gbif.registry.ws.resources;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.service.common.LoggedUser;
import org.gbif.identity.model.UserModelMutationResult;
import org.gbif.registry.ws.model.AuthenticationDataParameters;
import org.gbif.ws.response.GbifResponseStatus;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
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

import static org.gbif.registry.ws.security.SecurityContextCheck.ensureNotGbifScheme;
import static org.gbif.registry.ws.security.SecurityContextCheck.ensureUserSetInSecurityContext;
import static org.gbif.registry.ws.security.UserRoles.USER_ROLE;
import static org.gbif.registry.ws.util.ResponseUtils.buildResponse;

/**
 * The "/user" resource represents the "endpoints" the user can call using its own credentials.
 *
 * Web layer relating to authentication.
 *
 * Design and implementation decisions:
 * - This resource contains mostly the routing to the business logic ({@link IdentityService}) including
 *   authorizations. This resource does NOT implement the service but aggregates it (by Dependency Injection).
 * - Methods can return {@link Response} instead of object to minimize usage of exceptions and provide
 *   better control over the HTTP code returned. This also allows to return an entity in case
 *   of errors (e.g. {@link UserModelMutationResult}
 * - keys (user id) are not considered public, therefore the username is used as key
 * - In order to strictly control the data that is exposed this class uses "view models" (e.g. {@link LoggedUser})
 */
@Path("user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Singleton
public class UserResource {

  private final IdentityService identityService;

  /**
   * Main {@link UserResource} constructor
   * @param identityService implementation of the identity service to use.
   */
  @Inject
  public UserResource(IdentityService identityService) {
    this.identityService = identityService;
  }

  /**
   * Check the credentials of a user using Basic Authentication and return a {@link LoggedUser} if successful.
   * @return the user as {@link LoggedUser}
   */
  @GET
  @Path("/login")
  public LoggedUser login(@Context SecurityContext securityContext, @Context HttpServletRequest request) {
    // the user shall be authenticated using basic auth. scheme only.
    ensureNotGbifScheme(securityContext);
    ensureUserSetInSecurityContext(securityContext);

    GbifUser user = identityService.get(securityContext.getUserPrincipal().getName());
    identityService.updateLastLogin(user.getKey());
    return LoggedUser.from(user);
  }

  /**
   * Allows a user to change its own password.
   *
   * @param securityContext
   * @param authenticationDataParameters
   * @return
   */
  @PUT
  @RolesAllowed({USER_ROLE})
  @Path("/changePassword")
  public Response changePassword(@Context SecurityContext securityContext,
                                 AuthenticationDataParameters authenticationDataParameters) {
    // the user shall be authenticated using basic auth. scheme
    ensureNotGbifScheme(securityContext);
    ensureUserSetInSecurityContext(securityContext);

    String identifier = securityContext.getUserPrincipal().getName();
    GbifUser user = identityService.get(identifier);
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
