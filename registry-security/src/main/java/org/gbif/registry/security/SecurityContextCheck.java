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
package org.gbif.registry.security;

import org.gbif.api.model.common.GbifUser;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.security.GbifAuthUtils;
import org.gbif.ws.security.GbifAuthentication;
import org.gbif.ws.util.SecurityConstants;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Utility methods to check conditions on {@link Authentication} *** IMPORTANT *** Convention: -
 * ensure methods throws exception - check methods: return boolean
 */
public final class SecurityContextCheck {

  private static final Logger LOG = LoggerFactory.getLogger(SecurityContextCheck.class);

  /**
   * Utility class
   */
  private SecurityContextCheck() {}

  /**
   * Ensure that a user is present in the security context otherwise throw WebApplicationException
   * UNAUTHORIZED.
   *
   * @throws WebApplicationException UNAUTHORIZED if the user is not present in the {@link
   *                                 Authentication}.
   */
  public static void ensureUserSetInSecurityContext(final Authentication authentication) {
    ensureUserSetInSecurityContext(authentication, HttpStatus.UNAUTHORIZED);
  }

  /**
   * Ensure that a user is present in the security context otherwise throw provided status.
   *
   * @throws WebApplicationException if the user is not present in the {@link Authentication}.
   */
  public static void ensureUserSetInSecurityContext(
      final Authentication authentication, final HttpStatus status) {
    if (authentication == null || StringUtils.isBlank(authentication.getName())) {
      LOG.debug("Unauthenticated or incomplete request");
      throw new WebApplicationException("Unauthenticated or incomplete request", status);
    }
  }

  /**
   * Ensure a {@link Authentication} was obtained using the {@link SecurityConstants#GBIF_SCHEME}
   * authentication scheme. If the {@link Authentication} is null, this method will throw {@link
   * WebApplicationException} FORBIDDEN.
   *
   * @throws WebApplicationException FORBIDDEN if the {@link Authentication} is null or was not
   *                                 obtained using the GBIF authentication scheme.
   */
  public static void ensureGbifScheme(final Authentication authentication) {
    if (authentication != null
        && SecurityConstants.GBIF_SCHEME.equals(
            ((GbifAuthentication) authentication).getAuthenticationScheme())) {
      return;
    }
    throw new WebApplicationException("GBIF scheme is expected", HttpStatus.FORBIDDEN);
  }

  /**
   * Ensure a {@link Authentication} was not obtained using the {@link
   * SecurityConstants#GBIF_SCHEME} authentication scheme. If the {@link Authentication} is null,
   * this method will throw {@link WebApplicationException} FORBIDDEN.
   *
   * @throws WebApplicationException FORBIDDEN if the {@link Authentication} is null or was obtained
   *                                 using the GBIF authentication scheme.
   */
  public static void ensureNotGbifScheme(final Authentication authentication) {
    if (authentication != null
        && !SecurityConstants.GBIF_SCHEME.equals(
            ((GbifAuthentication) authentication).getAuthenticationScheme())) {
      return;
    }
    throw new WebApplicationException("Not GBIF scheme is expected", HttpStatus.FORBIDDEN);
  }

  /**
   * A user impersonation is when an application is authenticated using the appkey to act on behalf
   * of a user. This method ensures that if user impersonation is used, it is done by an authorized
   * appkey.
   *
   * @param appKeyWhitelist depending on the context the appKey whitelist may be different
   */
  public static void ensureAuthorizedUserImpersonation(
      final Authentication authentication,
      final String authHeader,
      final List<String> appKeyWhitelist) {
    ensureUserSetInSecurityContext(authentication);
    ensureGbifScheme(authentication);

    final String appKey = GbifAuthUtils.getAppKeyFromRequest(authHeader);
    if (appKeyWhitelist.contains(appKey)) {
      return;
    }
    throw new WebApplicationException(
        "User is not present in the white list", HttpStatus.FORBIDDEN);
  }

  public static boolean checkSameUser(GbifUser user, String username) {
    return user != null && user.getUserName().equals(username);
  }

  /**
   * Check if the user represented by the {@link Authentication} has at least one of the provided
   * roles.
   *
   * @param roles this methods will return true if the user is at least in one role. If no role is
   *              provided this method will return false.
   * @return the user is at least in one of the provided role(s)
   */
  public static boolean checkUserInRole(Authentication authentication, String... roles) {
    Objects.requireNonNull(authentication, "authentication shall be provided");

    if (roles == null || roles.length < 1) {
      return false;
    }

    return Arrays.stream(roles)
        .filter(StringUtils::isNotEmpty)
        .map(SimpleGrantedAuthority::new)
        .anyMatch(role -> authentication.getAuthorities().contains(role));
  }

  /**
   * Check if the user represented by the {@link Authentication} does NOT have the ADMIN role.
   */
  public static boolean checkIsNotAdmin(Authentication authentication) {
    return !checkUserInRole(authentication, UserRoles.ADMIN_ROLE);
  }

  /**
   * Check if the user represented by the {@link Authentication} does NOT have the EDITOR role.
   */
  public static boolean checkIsNotEditor(Authentication authentication) {
    return !checkUserInRole(authentication, UserRoles.EDITOR_ROLE);
  }

  /**
   * Check if the user represented by the {@link Authentication} does NOT have the APP role.
   */
  public static boolean checkIsNotApp(Authentication authentication) {
    return !checkUserInRole(authentication, UserRoles.APP_ROLE);
  }
}
