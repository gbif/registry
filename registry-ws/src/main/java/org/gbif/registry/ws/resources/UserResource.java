package org.gbif.registry.ws.resources;

import org.gbif.api.model.common.User;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.service.common.UserSession;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.identity.model.Session;
import org.gbif.identity.model.UserModelMutationResult;
import org.gbif.registry.ws.filter.CookieAuthFilter;
import org.gbif.registry.ws.guice.IdentityEmailManagerMock;
import org.gbif.registry.ws.model.UserCreation;
import org.gbif.registry.ws.model.UserUpdate;
import org.gbif.registry.ws.security.UpdateRulesManager;
import org.gbif.utils.AnnotationUtils;
import org.gbif.ws.response.GbifResponseStatus;
import org.gbif.ws.security.GbifAuthService;
import org.gbif.ws.util.ExtraMediaTypes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
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
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.EDITOR_ROLE;
import static org.gbif.registry.ws.security.UserRoles.USER_ROLE;

/**
 * Web layer relating to authentication and account creation.
 *
 * Design and implementation decisions:
 * - This resource contains mostly to routing to the business logic ({@link IdentityService}
 * - Return {@link Response} instead of object to minimize usage of exceptions and provide
 *   better control over the HTTP code returned. This also allows to return an entity in case
 *   of errors (e.g. {@link UserModelMutationResult}.
 * - keys (user id) are not considered public, therefore, they are not returned and/or accepted
 *   by methods that are not under ADMIN_ROLE or EDITOR_ROLE.
 * - In order to strictly control the data that is exposed this class uses "view models" (e.g. {@link UserSession}).
 */
@Path("user")
@Produces({MediaType.APPLICATION_JSON, ExtraMediaTypes.APPLICATION_JAVASCRIPT})
@Consumes(MediaType.APPLICATION_JSON)
@Singleton
public class UserResource {
  private static final Logger LOG = LoggerFactory.getLogger(UserResource.class);

  private final IdentityService identityService;
  private final IdentityEmailManagerMock tempIdentityEmailManager;

  private static final List<UserRole> USER_ROLES = Arrays.stream(UserRole.values()).filter( r ->
          !AnnotationUtils.isFieldDeprecated(UserRole.class, r.name())).collect(Collectors.toList());

  @Inject
  public UserResource(IdentityService identityService, IdentityEmailManagerMock tempIdentityEmailManager) {
    this.identityService = identityService;
    this.tempIdentityEmailManager = tempIdentityEmailManager;
  }

  @GET
  @Path(("/debug"))
  public Map<String, Object> debug() {
    Map<String, Object> debugMap = new HashMap<>();
    //tempIdentityEmailManager.
    debugMap.put("challengeCodes", tempIdentityEmailManager.getAllChallengeCode());
    return debugMap;
  }

  @GET
  @Path("/roles")
  public List<UserRole> listRoles() {
    return USER_ROLES;
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

    ensureUserInSecurityContext(security);

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
  @Path("/logout")
  public Response logout(@Context SecurityContext security, @Context HttpServletRequest request,
                         @Nullable @QueryParam("allSessions") Boolean allSessions) {

    ensureUserInSecurityContext(security);

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

  /**
   * Creates a user account.
   */
  @POST
  @Path("/")
  public Response create(@Context SecurityContext securityContext, UserCreation user) {

    ensureIsTrustedApp(securityContext);

    int returnStatusCode = Response.Status.CREATED.getStatusCode();
    UserModelMutationResult result = identityService.create(
            UpdateRulesManager.applyCreate(user), user.getPassword());
    if(result.containsError()) {
      returnStatusCode = GbifResponseStatus.UNPROCESSABLE_ENTITY.getStatus();
    }
    return generateResponse(returnStatusCode, result);
  }

  /**
   * Confirm a challengeCode for a specific user.
   * The username is taken from the securityContext.
   *
   * @param securityContext
   * @param challengeCode
   *
   * @return
   */
  @POST
  @Path("/confirm")
  public Response confirmChallengeCode(@Context SecurityContext securityContext,
                                       @QueryParam("challengeCode") UUID challengeCode) {

    ensureIsTrustedApp(securityContext);
    ensureUserInSecurityContext(securityContext);

    User user = identityService.get(securityContext.getUserPrincipal().getName());
    if(user != null && identityService.confirmChallengeCode(user.getKey(), challengeCode)){
      //generate a token
      Session session = identityService.createSession(user.getUserName());
      String sessionToken = session.getSession();
      identityService.updateLastLogin(user.getKey());

      //ideally we would return 200 OK but CreatedResponseFilter automatically
      //change it to 201 CREATED
      return buildResponse(Response.Status.CREATED, UserSession.from(user, sessionToken));
    }
    return Response.status(Response.Status.BAD_REQUEST).build();
  }

  /**
   * Updates the user asserting that the user being updated is indeed the authenticated user.
   * TODO: An admin console equivalent that allows registry_editor role to do this.
   */
  @PUT
  @Path("/")
  @RolesAllowed({USER_ROLE})
  public Response update(UserUpdate user, @Context SecurityContext securityContext) {
    ensureUserInSecurityContext(securityContext);

    User updateInitiator = identityService.get(securityContext.getUserPrincipal().getName());
    User currentUser = identityService.get(user.getUserName());

    //TODO check the user is himself or is an admin
    Response response = Response.noContent().build();
    UserModelMutationResult result = identityService.update(UpdateRulesManager.applyUpdate(
            updateInitiator.getRoles(), currentUser, user));
    if(result.containsError()) {
      response = generateResponse(GbifResponseStatus.UNPROCESSABLE_ENTITY.getStatus(), result);
    }
    return response;
  }

  /**
   * Utility to determine if the token provided is valid for the given user.
   * @param challengeCode To check
   */
  @GET
  @Path("/challengeCodeValid")
  public Response tokenValidityCheck(@Context SecurityContext securityContext,
                                 @QueryParam("challengeCode") UUID challengeCode) {

    ensureIsTrustedApp(securityContext);
    ensureUserInSecurityContext(securityContext);

    String username = securityContext.getUserPrincipal().getName();
    User user = identityService.get(username);

    if(identityService.isChallengeCodeValid(user.getKey(), challengeCode)) {
      return Response.noContent().build();
    }
    return buildResponse(Response.Status.UNAUTHORIZED);
  }

  /**
   *
   */
  @POST
  @Path("/resetPassword")
  public Response resetPassword(@Context SecurityContext securityContext) {
    ensureIsTrustedApp(securityContext);
    ensureUserInSecurityContext(securityContext);

    String identifier= securityContext.getUserPrincipal().getName();
    User user = Optional.ofNullable(identityService.get(identifier))
            .orElse(identityService.getByEmail(identifier));
    if (user != null) {
      // initiate mail, and store the challenge etc.
      identityService.resetPassword(user.getKey());
    }
    //this will probably send 201
    return Response.noContent().build();
  }

  /**
   * Updates the user password only if the token presented is valid for the user account.
   */
  @POST
  @Path("/updatePassword")
  public Response updatePassword(@Context SecurityContext securityContext, @QueryParam("password")String password,
                                 @QueryParam("challengeCode") UUID challengeCode) {
    ensureIsTrustedApp(securityContext);
    ensureUserInSecurityContext(securityContext);

    String username = securityContext.getUserPrincipal().getName();
    User user = identityService.get(username);

    if(identityService.updatePassword(user.getKey(), password, challengeCode)){
      //terminate all previous sessions
      identityService.terminateAllSessions(user.getUserName());

      //generate a new one
      Session session = identityService.createSession(user.getUserName());
      String sessionToken = session.getSession();
      identityService.updateLastLogin(user.getKey());
      return Response.ok().entity(UserSession.from(user, sessionToken)).build();
    }

    return buildResponse(Response.Status.UNAUTHORIZED);
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
   * For admin console
   * Returns the identified user account.
   * @return the user or null
   */
  @GET
  @RolesAllowed({EDITOR_ROLE, ADMIN_ROLE})
  @Path("/{userKey}")
  public User getById(@PathParam("userKey") int userKey) {
    return identityService.getByKey(userKey);
  }

  /**
   * For admin console
   */
  @PUT
  @RolesAllowed({EDITOR_ROLE, ADMIN_ROLE})
  @Path("/{userKey}")
  public Response updateById(@PathParam("userKey") int userKey, UserUpdate user, @Context SecurityContext securityContext) {
    return update(user, securityContext);
  }

  /**
   *
   * @param security
   * @return
   * @throws WebApplicationException FORBIDDEN if the request is not coming from a trusted application
   */
  private static void ensureIsTrustedApp(SecurityContext security) {
    if(GbifAuthService.GBIF_SCHEME.equals(security.getAuthenticationScheme())) {
      //TODO check the appKey is portal16 or registry console
      return;
    }
    throw new WebApplicationException(Response.Status.FORBIDDEN);
  }

  /**
   * Check that a user is present in the SecurityContext otherwise throw WebApplicationException.
   * @param securityContext
   * @throws WebApplicationException FORBIDDEN if the user is not present in the {@link SecurityContext}
   */
  private static void ensureUserInSecurityContext(SecurityContext securityContext)
          throws WebApplicationException {
    if(securityContext.getUserPrincipal() == null ||
            StringUtils.isBlank(securityContext.getUserPrincipal().getName())){
      LOG.warn("The user must be identified by the username. AuthenticationScheme: {}", securityContext.getAuthenticationScheme());
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
  }

  private static Response buildResponse(Response.Status status) {
    return buildResponse(status, null);
  }
  private static Response buildResponse(Response.Status status, @Nullable Object entity) {
    return generateResponse(status.getStatusCode(), entity);
  }

  private static Response generateResponse(int returnStatusCode, @Nullable Object entity) {
    Response.ResponseBuilder bldr = Response.status(returnStatusCode);
    if(entity != null) {
      bldr.entity(entity);
    }
    return bldr.build();
  }

}
