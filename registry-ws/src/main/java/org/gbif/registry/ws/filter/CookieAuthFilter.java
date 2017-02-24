package org.gbif.registry.ws.filter;

import org.gbif.api.model.common.User;
import org.gbif.api.model.common.UserPrincipal;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.identity.util.SessionTokens;

import java.security.Principal;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.SecurityContext;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.sun.jersey.api.core.HttpRequestContext;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A filter that will inspect the request and determine if a currently active user session is identified.
 * If a session is found, the associate user is added to the security principle.
 *
 * This will look at the cookeis,
 *
 * <b>This filter will do nothing if the security principle already identifies an account</b>
 */
public class CookieAuthFilter implements ContainerRequestFilter {
  private static final Logger LOG = LoggerFactory.getLogger(CookieAuthFilter.class);

  private static final String COOKIE_DOMAIN = ".gbif.org";
  private static final String COOKIE_SESSION = "USER_SESSION";
  private static final String GBIF_AUTH_SCHEME = "GBIF_SESSION";
  private static final String GBIF_USER_HEADER = "x-gbif-user-session";

  private final IdentityService identityService;

  @Inject
  public CookieAuthFilter(IdentityService identityService) {
    this.identityService = identityService;
  }

  @Override
  public ContainerRequest filter(final ContainerRequest request) {


    // Authentication is only invoked if a previous authentication scheme (e.g. GBIF trusted or HTTP
    // an BASIC) has not already identified a user principle
    if (request.getUserPrincipal() == null) {


      final User user = userFromRequest(request);

      if (user != null) {
        final UserPrincipal userPrincipal = new UserPrincipal(user);

        request.setSecurityContext(new SecurityContext() {
          @Override
          public Principal getUserPrincipal() {
            return userPrincipal;
          }

          @Override
          public boolean isUserInRole(String role) {
            return userPrincipal.hasRole(role);
          }

          @Override
          public boolean isSecure() {
            return request.isSecure();
          }

          @Override
          public String getAuthenticationScheme() {
            return GBIF_AUTH_SCHEME;
          }
        });
      }
    }
    return request;
  }

  /**
   * Extract the user from the session associated with the request, either by HTTP header or cookie.
   * @return the user or null
   */
  @VisibleForTesting
  User userFromRequest(ContainerRequest request) {
    String sessionToken = sessionTokenFromRequest(request);

    if (sessionToken != null) {
      String session = SessionTokens.session(sessionToken);
      String userName = SessionTokens.username(sessionToken);
      if (session != null && userName != null) {
        User user = identityService.getBySession(sessionToken);
        LOG.info("Presented {} with session[{}] matching user", userName, session, user);

        // not critical but defensive: verify provided user matches expected user
        if (userName != null && user != null && userName.equalsIgnoreCase(user.getUserName())) {
          return user;
        }

      }
    }

    return null;
  }

  /**
   * Extracts the session token from the request.
   * @return token or null
   */
  public static String sessionTokenFromRequest(HttpRequestContext request) {
    // prefer the header to the cookie
    String sessionToken = request.getHeaderValue(GBIF_USER_HEADER);
    if (sessionToken == null) {
      Cookie sessionCookie = request.getCookies().get(COOKIE_SESSION);
      if (sessionCookie != null) {
        sessionToken = sessionCookie.getValue();
      }
    }
    return sessionToken;
  }

  /**
   * Extracts the session token from the request.
   * @return token or null
   */
  public static String sessionTokenFromRequest(HttpServletRequest request) {
    // prefer the header to the cookie
    String sessionToken = request.getHeader(GBIF_USER_HEADER);
    if (sessionToken == null) {
      javax.servlet.http.Cookie[] cookies = request.getCookies();
      if (cookies != null) {
        for (javax.servlet.http.Cookie c : cookies) {
          if (COOKIE_SESSION.equals(c.getName())) {
            sessionToken = c.getValue();
          }
        }
      }
    }
    return sessionToken;
  }
}
