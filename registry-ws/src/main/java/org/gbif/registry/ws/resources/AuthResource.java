package org.gbif.registry.ws.resources;

import org.gbif.api.model.common.User;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.common.IdentityService;
import org.gbif.identity.model.Session;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.ws.util.ExtraMediaTypes;

import java.security.Principal;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.mybatis.guice.transactional.Transactional;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.EDITOR_ROLE;
import static org.gbif.registry.ws.security.UserRoles.USER_ROLE;

/**
 * Services relating to authentication and account creation.
 */
@Path("auth")
@Produces({MediaType.APPLICATION_JSON, ExtraMediaTypes.APPLICATION_JAVASCRIPT})
@Consumes(MediaType.APPLICATION_JSON)
@Singleton
public class AuthResource {

  private final IdentityService identityService;

  // This Guice injection is only used for testing purpose
  @Inject(optional = true)
  @Named("guiceInjectedSecurityContext")
  @Context
  private SecurityContext securityContext;

  @Inject
  public AuthResource(IdentityService identityService) {
    this.identityService = identityService;
  }

  @GET
  @Path("login")
  @Transactional
  @RolesAllowed({USER_ROLE, ADMIN_ROLE, EDITOR_ROLE})
  public User login(@Context HttpServletResponse response) {
    Principal p = securityContext.getUserPrincipal();
    // add the session to the response
    response.addCookie(new Cookie("tim", "tim")); // TODO: create a token
    return identityService.get(p.getName());
  }
}
