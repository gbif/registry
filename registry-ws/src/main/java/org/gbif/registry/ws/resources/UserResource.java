package org.gbif.registry.ws.resources;

import org.gbif.api.model.common.User;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.common.IdentityService;
import org.gbif.identity.model.Session;
import org.gbif.identity.util.PasswordEncoder;
import org.gbif.registry.ws.filter.CookieAuthFilter;
import org.gbif.ws.util.ExtraMediaTypes;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.core.HttpRequestContext;
import com.sun.jersey.spi.container.ContainerRequest;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.EDITOR_ROLE;
import static org.gbif.registry.ws.security.UserRoles.USER_ROLE;

/**
 * Services relating to authentication and account creation.
 */
@Path("user")
@Produces({MediaType.APPLICATION_JSON, ExtraMediaTypes.APPLICATION_JAVASCRIPT})
@Consumes(MediaType.APPLICATION_JSON)
@Singleton
public class UserResource {
  private static final Logger LOG = LoggerFactory.getLogger(UserResource.class);
  private static final PasswordEncoder passwordEncoder = new PasswordEncoder();

  private final IdentityService identityService;

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
  public User getAuthenticatedUser(@Context SecurityContext security, @Context HttpServletResponse response) {
    //response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
    //response.addHeader("Access-Control-Allow-Origin", "http://localhost:8080");
    response.addHeader("Access-Control-Allow-Credentials", "true");



    return identityService.get(security.getUserPrincipal().getName());
  }

  /**
   * Redirects the user to the target, appending a session token.
   * @return the user
   */
  @GET
  @RolesAllowed({USER_ROLE})
  @Path("/login")
  public Map<String, Object>  login(@Context SecurityContext security, @Context HttpServletRequest request,
                    @Context HttpServletResponse response) {

    // Definsive coding follows: create a session only if one is not already present
    String sessionToken = CookieAuthFilter.sessionTokenFromRequest(request);
    if (sessionToken == null) {
      Session session = identityService.createSession(security.getUserPrincipal().getName());
      sessionToken = session.getSession();
    }

    User user = identityService.get(security.getUserPrincipal().getName());
    // strip sensitive information
    user.setPasswordHash("TODO");
    user.getRoles().clear(); // TODO
    return ImmutableMap.<String, Object>of("user", user, "session", sessionToken);
  }

  /**
   * Returns the User account for the username and password provided (HTTP Basic Auth).
   * @return the user
   */
  @GET
  @RolesAllowed({USER_ROLE})
  @Path("/logout")
  public void logout(@Context SecurityContext security, @Context HttpServletRequest request,
                     @QueryParam("allSessions") boolean allSessions) {
    if (allSessions) {
      identityService.terminateAllSessions(security.getUserPrincipal().getName());
    } else {
      String sessionToken = CookieAuthFilter.sessionTokenFromRequest(request);
      if (sessionToken != null) {
        identityService.terminateSession(sessionToken);
      }
    }
  }

  /**
   * Creates the user account.
   */
  @POST
  @Path("/")
  public void create(User user) {
    identityService.create(user);
  }

  /**
   * Updates the user asserting that the user being updated is indeed the authenticated user.
   * TODO: An admin console equivalent that allows registry_editor role to do this.
   */
  @PUT
  @Path("/")
  @RolesAllowed({USER_ROLE})
  public void update(User user, @Context SecurityContext securityContext) {
    Preconditions.checkArgument(securityContext.getUserPrincipal().getName() == user.getUserName(),
                                "The account being updated must be the authenticated user");
    identityService.update(user);
  }

  /**
   * Utility to determine if the token provided is valid for the given user.
   * @param token To check
   * @return true or false
   */
  @GET
  @Path("/tokenValid?token={token}")
  public void tokenValidityCheck(@QueryParam("token") String token, @Context SecurityContext securityContext) {
    Preconditions.checkArgument("GBIF".equalsIgnoreCase(securityContext.getAuthenticationScheme()),
                                "Only trusted applications may call this service.  Is your application registered?");
    Preconditions.checkArgument(securityContext.getUserPrincipal() != null && securityContext.getUserPrincipal().getName() != null,
                                "The user must be identified by the username");

    String username = securityContext.getUserPrincipal().getName();
    User user = identityService.get(username);
    // TODO - check the token makes sense!
  }

  /**
   * Updates the user password only if the token presented is valid for the user account.
   */
  @POST
  @Path("/updatePassword")
  public void updatePassword(@Context SecurityContext securityContext, String password, String token) {
    Preconditions.checkArgument("GBIF".equalsIgnoreCase(securityContext.getAuthenticationScheme()),
                                "Only trusted applications may call this service.  Is your application registered?");
    Preconditions.checkArgument(securityContext.getUserPrincipal() != null && securityContext.getUserPrincipal().getName() != null,
      "The user must be identified by the username");

    String username = securityContext.getUserPrincipal().getName();
    User user = identityService.get(username);
    // TODO - check the token makes sense!
    user.setPasswordHash(passwordEncoder.encode(password));
    identityService.update(user);
  }

  /**
   * Updates the user password only if the token presented is valid for the user account.
   */
  @POST
  @Path("/requestPassword")
  public void requestPassword(@Context SecurityContext securityContext, @FormParam("password") String password,
                              @FormParam("token") String token) {
    Preconditions.checkArgument("GBIF".equalsIgnoreCase(securityContext.getAuthenticationScheme()),
                                "Only trusted applications may call this service.  Is your application registered?");
    Preconditions.checkArgument(securityContext.getUserPrincipal() != null && securityContext.getUserPrincipal().getName() != null,
                                "The user must be identified by the username");

    String username = securityContext.getUserPrincipal().getName();
    User user = identityService.get(username);
    // initiate mail, and store the challenge etc.
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
   * Returns the identified user account.
   * @return the user or null
   */
  @GET
  @RolesAllowed({EDITOR_ROLE, ADMIN_ROLE})
  @Path("/{userId}")
  public User getById(@PathParam("userId") int userId) {
    return identityService.getByKey(userId);
  }

}
