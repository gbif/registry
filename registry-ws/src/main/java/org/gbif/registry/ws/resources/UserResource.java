package org.gbif.registry.ws.resources;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.service.common.LoggedUser;
import org.gbif.identity.model.UserModelMutationResult;
import org.gbif.registry.ws.model.AuthenticationDataParameters;
import org.gbif.registry.ws.security.jwt.JwtConfiguration;
import org.gbif.registry.ws.security.jwt.JwtUtils;
import org.gbif.ws.response.GbifResponseStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.http.HttpStatus;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import static org.gbif.registry.ws.security.SecurityContextCheck.ensureNotGbifScheme;
import static org.gbif.registry.ws.security.SecurityContextCheck.ensureUserSetInSecurityContext;
import static org.gbif.registry.ws.security.UserRoles.USER_ROLE;
import static org.gbif.registry.ws.util.ResponseUtils.buildResponse;

/**
 * The "/user" resource represents the "endpoints" the user can call using its own credentials.
 * <p>
 * Web layer relating to authentication.
 * <p>
 * Design and implementation decisions:
 * - This resource contains mostly the routing to the business logic ({@link IdentityService}) including
 * authorizations. This resource does NOT implement the service but aggregates it (by Dependency Injection).
 * - Methods can return {@link Response} instead of object to minimize usage of exceptions and provide
 * better control over the HTTP code returned. This also allows to return an entity in case
 * of errors (e.g. {@link UserModelMutationResult}
 * - keys (user id) are not considered public, therefore the username is used as key
 * - In order to strictly control the data that is exposed this class uses "view models" (e.g. {@link LoggedUser})
 */
@Path("user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Singleton
public class UserResource {

  @VisibleForTesting
  public static final String ROLES_FIELD_RESPONSE = "roles";
  @VisibleForTesting
  public static final String EDITOR_RIGHTS_FIELD_RESPONSE = "editorRoleScopes";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final IdentityService identityService;
  private final JwtConfiguration jwtConfiguration;

  /**
   * Main {@link UserResource} constructor
   *
   * @param identityService implementation of the identity service to use.
   */
  @Inject
  public UserResource(IdentityService identityService, JwtConfiguration jwtConfiguration) {
    this.identityService = identityService;
    this.jwtConfiguration = jwtConfiguration;
  }

  /**
   * Check the credentials of a user using Basic Authentication and return a {@link LoggedUser} if successful.
   *
   * @return the user as {@link LoggedUser}
   */
  @GET
  @Path("/login")
  public Response loginGet(@Context SecurityContext securityContext, @Context HttpServletRequest request) {
    // the user shall be authenticated using basic auth. scheme only.
    ensureNotGbifScheme(securityContext);
    ensureUserSetInSecurityContext(securityContext);

    return login(securityContext.getUserPrincipal().getName());
  }

  @POST
  @Path("/login")
  public Response loginPost(@Context SecurityContext securityContext, @Context HttpServletRequest request) {
    // the user shall be authenticated using basic auth. scheme only.
    ensureNotGbifScheme(securityContext);
    ensureUserSetInSecurityContext(securityContext);

    return login(securityContext.getUserPrincipal().getName());
  }

  // only to use in login since it updates the last login
  private Response login(String username) {
    // get the user
    GbifUser user = identityService.get(username);

    if (user == null) {
      return Response.status(HttpStatus.SC_BAD_REQUEST).build();
    }

    identityService.updateLastLogin(user.getKey());

    // build response
    ObjectNode response = buildResponseUserLogged(user);
    // add jwt token
    response.put(JwtConfiguration.TOKEN_FIELD_RESPONSE, JwtUtils.generateJwt(user.getUserName(), jwtConfiguration));

    return Response.ok(response).cacheControl(createNoCacheHeaders()).build();
  }

  private ObjectNode buildResponseUserLogged(GbifUser user) {
    if (user == null) {
      return OBJECT_MAPPER.createObjectNode();
    }

    // get editor rights
    List<UUID> editorRights = identityService.listEditorRights(user.getUserName());

    // build response
    ObjectNode response = OBJECT_MAPPER.valueToTree(LoggedUser.from(user));

    // add roles
    ArrayNode rolesArray = response.putArray(ROLES_FIELD_RESPONSE);
    Optional.ofNullable(user.getRoles()).ifPresent(userRoles -> userRoles.forEach(role -> rolesArray.add(role.name())));

    // add editor rights
    ArrayNode editorRightsArray = response.putArray(EDITOR_RIGHTS_FIELD_RESPONSE);
    Optional.ofNullable(editorRights).ifPresent(rights -> rights.forEach(v -> editorRightsArray.add(v.toString())));

    return response;
  }

  @POST
  @Path("/whoami")
  public Response whoAmI(@Context SecurityContext securityContext, @Context HttpServletRequest request) {
    // the user shall be authenticated using basic auth. scheme
    ensureNotGbifScheme(securityContext);
    ensureUserSetInSecurityContext(securityContext);

    // get the user
    GbifUser user = identityService.get(securityContext.getUserPrincipal().getName());

    if (user == null) {
      return Response.status(HttpStatus.SC_BAD_REQUEST).build();
    }

    return Response.ok(buildResponseUserLogged(user)).cacheControl(createNoCacheHeaders()).build();
  }

  /**
   * Allows a user to change its own password.
   */
  @PUT
  @RolesAllowed({USER_ROLE})
  @Path("/changePassword")
  public Response changePassword(
    @Context SecurityContext securityContext, AuthenticationDataParameters authenticationDataParameters
  ) {
    // the user shall be authenticated using basic auth. scheme
    ensureNotGbifScheme(securityContext);
    ensureUserSetInSecurityContext(securityContext);

    String identifier = securityContext.getUserPrincipal().getName();
    GbifUser user = identityService.get(identifier);
    if (user != null) {
      UserModelMutationResult updatePasswordMutationResult =
        identityService.updatePassword(user.getKey(), authenticationDataParameters.getPassword());
      if (updatePasswordMutationResult.containsError()) {
        return buildResponse(GbifResponseStatus.UNPROCESSABLE_ENTITY.getStatus(), updatePasswordMutationResult);
      }
    }
    return Response.noContent().build();
  }

  private static CacheControl createNoCacheHeaders() {
    CacheControl cacheControl = new CacheControl();
    cacheControl.setPrivate(true);
    cacheControl.setNoCache(true);
    cacheControl.setNoStore(true);

    return cacheControl;
  }

}
