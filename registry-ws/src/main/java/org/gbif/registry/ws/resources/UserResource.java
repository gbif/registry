package org.gbif.registry.ws.resources;

import org.gbif.api.model.common.User;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.service.common.UserSession;
import org.gbif.identity.model.Session;
import org.gbif.identity.model.UserModelMutationResult;
import org.gbif.registry.ws.filter.CookieAuthFilter;
import org.gbif.registry.ws.model.AuthenticationDataParameters;
import org.gbif.ws.util.ExtraMediaTypes;

import java.util.Optional;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.BooleanUtils;
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
 * - In order to strictly control the data that is exposed this class uses "view models" (e.g. {@link UserSession}).
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
  public UserSession getAuthenticatedUser(@Context SecurityContext security, @Context HttpServletResponse response) {
    response.addHeader("Access-Control-Allow-Credentials", "true");
    return UserSession.from(identityService.get(security.getUserPrincipal().getName()));
  }

  /**
   *
   * @return the user and a session token as {@link UserSession}
   */
  @GET
  @Path("/login")
  public UserSession login(@Context SecurityContext security, @Context HttpServletRequest request) {

    ensureUserSetInSecurityContext(security);

    // create a session only if one is not already present
    String sessionToken = CookieAuthFilter.sessionTokenFromRequest(request);
    if (sessionToken == null) {
      Session session = identityService.createSession(security.getUserPrincipal().getName());
      sessionToken = session.getSession();
    }
    User user = identityService.get(security.getUserPrincipal().getName());
    identityService.updateLastLogin(user.getKey());
    return UserSession.from(user, sessionToken);
  }

  @GET
  @RolesAllowed({USER_ROLE})
  @Path("/logout")
  public Response logout(@Context SecurityContext security, @Context HttpServletRequest request,
                         @Nullable @QueryParam("allSessions") Boolean allSessions) {

    Response.Status returnStatus = Response.Status.NO_CONTENT;
    if (BooleanUtils.isTrue(allSessions)) {
      identityService.terminateAllSessions(security.getUserPrincipal().getName());
    } else {
      String sessionToken = CookieAuthFilter.sessionTokenFromRequest(request);
      if (sessionToken != null) {
        identityService.terminateSession(sessionToken);
      }
      else {
        returnStatus = Response.Status.BAD_REQUEST;
      }
    }
    return buildResponse(returnStatus);
  }

  @PUT
  @RolesAllowed({USER_ROLE})
  @Path("/changePassword")
  public Response changePassword(@Context SecurityContext securityContext,
                                 AuthenticationDataParameters authenticationDataParameters) {

    ensureUserSetInSecurityContext(securityContext);

    String identifier = securityContext.getUserPrincipal().getName();
    User user = Optional.ofNullable(identityService.get(identifier))
            .orElse(identityService.getByEmail(identifier));
    if (user != null) {
      // initiate mail, and store the challenge etc.
      identityService.updatePassword(user.getKey(), authenticationDataParameters.getPassword());
    }
    return Response.noContent().build();
  }

}
