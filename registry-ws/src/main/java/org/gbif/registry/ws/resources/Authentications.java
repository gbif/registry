package org.gbif.registry.ws.resources;

import org.gbif.api.model.common.User;
import org.gbif.api.service.common.IdentityService;
import org.gbif.identity.model.Session;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

  static void attachNewCookieForSession(Session session, HttpServletResponse response) {
    OffsetDateTime oneHourFromNow
      = OffsetDateTime.now(ZoneOffset.UTC)
                      .plus(Duration.ofHours(1));

    String cookieExpires
      = DateTimeFormatter.RFC_1123_DATE_TIME
      .format(oneHourFromNow);

    response.setHeader("Set-Cookie", COOKIE_SESSION + "=" + session.getSession() + "; "
                                     + "Domain=.gbif-dev.org; Expires=" + cookieExpires + "; Path=/; HTTPOnly");

    /*
    final Boolean useSecureCookie = new Boolean(false);
    final int expiryTime = 60*60;  // 60 mins
    Cookie cookie = new Cookie(COOKIE_SESSION, session.getSession());
    cookie.setMaxAge(expiryTime);
    cookie.setPath("/");
    cookie.setDomain(".gbif-dev.org");
    response.addCookie(cookie);
    */
  }
}
