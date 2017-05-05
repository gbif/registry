package org.gbif.registry.ws.resources;

import org.gbif.api.model.common.User;
import org.gbif.api.service.common.IdentityService;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Utilities to help with authentication.
 */
class Authentications {
  private static final Logger LOG = LoggerFactory.getLogger(Authentications.class);

  private static final String COOKIE_DOMAIN = ".gbif.org";
  private static final String COOKIE_SESSION = "SESSION";

  static User userFromCookie(HttpServletRequest request, IdentityService identityService) {
    String session = sessionFromCookie(request, identityService);
    LOG.info("Session from cookie: {}", session);
    if (session != null) {
      return identityService.getBySession(session);
    }
    return null;
  }

  static String sessionFromCookie(HttpServletRequest request, IdentityService identityService) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      LOG.info("Cookies: {}", cookies.length);
      for (Cookie cookie : cookies) {
        LOG.info("Cookie name: {}", cookie.getName());
        // TODO: Consider secure cookies only?
        // TODO: Consider SameSite cookies only?
        // TODO: Consider verification of the cookie domain?

        if (cookie.getName().equals(COOKIE_SESSION)) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }

  /**
   * Check that a user is present in the getUserPrincipal of the SecurityContext otherwise throw
   * WebApplicationException UNAUTHORIZED.
   *
   * @param securityContext
   *
   * @throws WebApplicationException UNAUTHORIZED if the user is not present in the {@link SecurityContext}
   */
  static void ensureUserSetInSecurityContext(SecurityContext securityContext)
          throws WebApplicationException {
    if (securityContext == null || securityContext.getUserPrincipal() == null ||
            StringUtils.isBlank(securityContext.getUserPrincipal().getName())) {
      LOG.warn("The user must be identified by the username. AuthenticationScheme: {}",
              securityContext.getAuthenticationScheme());
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
  }
}
