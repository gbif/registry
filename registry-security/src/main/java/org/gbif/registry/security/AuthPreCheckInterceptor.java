package org.gbif.registry.security;

import org.gbif.ws.WebApplicationException;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * Intercepts all the requests and stops the execution chain for all the requests that contain the query
 * param {{@link #CHECK_PERMISSIONS_PARAM}} as <strong>true</strong>.
 */
public class AuthPreCheckInterceptor extends HandlerInterceptorAdapter {

  static final String CHECK_PERMISSIONS_PARAM = "checkPermissionsOnly";

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (request.getParameter(CHECK_PERMISSIONS_PARAM) != null
        && request.getParameter(CHECK_PERMISSIONS_PARAM).equalsIgnoreCase("true")) {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

      if (handler instanceof HandlerMethod) {
        Secured securedAnnotation =
            ((HandlerMethod) handler).getMethod().getAnnotation(Secured.class);

        if (securedAnnotation == null) {
          return false;
        }

        String[] roles = securedAnnotation.value();
        if (!SecurityContextCheck.checkUserInRole(authentication, roles)) {
          // safety check. This should never happen since the spring security filters should reject
          // this request before getting here
          throw new WebApplicationException(
              "User has to be in one if these roles: " + Arrays.toString(roles),
              HttpStatus.FORBIDDEN);
        }
      }

      return false;
    }

    return true;
  }
}
