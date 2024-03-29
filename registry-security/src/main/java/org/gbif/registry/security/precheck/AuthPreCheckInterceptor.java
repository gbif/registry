/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.security.precheck;

import org.gbif.registry.security.SecurityContextCheck;
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
 * Intercepts all the requests and stops the execution chain for all the requests that contain the
 * query param {{@link #CHECK_PERMISSIONS_ONLY_PARAM}} as <strong>true</strong>.
 */
public class AuthPreCheckInterceptor extends HandlerInterceptorAdapter {

  public static final String CHECK_PERMISSIONS_ONLY_PARAM = "checkPermissionsOnly";

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (containsCheckPermissionsOnlyParam(request)) {
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

  static boolean containsCheckPermissionsOnlyParam(HttpServletRequest request) {
    return request.getParameter(CHECK_PERMISSIONS_ONLY_PARAM) != null
        && request.getParameter(CHECK_PERMISSIONS_ONLY_PARAM).equalsIgnoreCase("true");
  }
}
