package org.gbif.registry.ws.security;

import org.apache.commons.lang3.StringUtils;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.security.GbifAuthUtils;
import org.gbif.ws.security.GbifAuthentication;
import org.gbif.ws.util.SecurityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.gbif.ws.util.SecurityConstants.GBIF_SCHEME;

/**
 * Utility methods to check conditions on {@link Authentication}
 * Convention:
 * - ensure methods throws exception
 * - check methods: return boolean
 */
public class SecurityContextCheck {

  private static final Logger LOG = LoggerFactory.getLogger(SecurityContextCheck.class);

  /**
   * Utility class
   */
  private SecurityContextCheck() {
  }

  // TODO: 2019-07-10 current problems/concerns:
  // 1) what should be used instead of jax-rs' SecurityContext
  // 2) where do one store authenticationScheme
  // 3) what should be thrown instead of WebApplicationException (custom?)
  // 4) white list

  /**
   * Check that a user is present in the getUserPrincipal of the SecurityContext otherwise throw
   * WebApplicationException UNAUTHORIZED.
   *
   * @param authentication
   * @throws WebApplicationException UNAUTHORIZED if the user is not present in the {@link Authentication}
   */
  public static void ensureUserSetInSecurityContext(final Authentication authentication) {
    if (authentication == null || authentication.getPrincipal() == null ||
        StringUtils.isBlank(((UserDetails) authentication.getPrincipal()).getUsername())) {
      LOG.warn("The user must be identified by the username.");
      throw new WebApplicationException(HttpStatus.UNAUTHORIZED);
    }
  }

  /**
   * Ensure a {@link Authentication} was obtained using the {@link SecurityConstants#GBIF_SCHEME} authentication scheme.
   * If the {@link Authentication} is null, this method will throw {@link WebApplicationException} FORBIDDEN.
   *
   * @param authentication
   * @throws WebApplicationException FORBIDDEN if the {@link Authentication} is null or was not obtained using the
   *                                 GBIF authentication scheme.
   */
  public static void ensureGbifScheme(final Authentication authentication) {
    if (authentication != null
        && GBIF_SCHEME.equals(((GbifAuthentication) authentication).getAuthenticationScheme())) {
      return;
    }
    throw new WebApplicationException(HttpStatus.FORBIDDEN);
  }

  /**
   * Ensure a {@link Authentication} was not obtained using the {@link SecurityConstants#GBIF_SCHEME} authentication
   * scheme.
   * If the {@link Authentication} is null, this method will throw {@link WebApplicationException} FORBIDDEN.
   *
   * @param authentication
   * @throws WebApplicationException FORBIDDEN if the {@link Authentication} is null or was obtained using the GBIF
   *                                 authentication scheme.
   */
  public static void ensureNotGbifScheme(final Authentication authentication) {
    if (authentication != null
        && !GBIF_SCHEME.equals(((GbifAuthentication) authentication).getAuthenticationScheme())) {
      return;
    }
    throw new WebApplicationException(HttpStatus.FORBIDDEN);
  }

  /**
   * Check the precondition unless the {@link Authentication} contains a specific role.
   *
   * @param authentication                   if null the precondition will failed
   * @param role
   * @param precondition
   * @param statusOnPreconditionFailed
   */
  public static void ensurePreconditionUnlessRoleIs(final String role, boolean precondition,
                                                    final Authentication authentication, HttpStatus statusOnPreconditionFailed) {
    if (authentication != null && (authentication.getAuthorities().contains(new SimpleGrantedAuthority(role)) || precondition)) {
      return;
    }
    throw new WebApplicationException(statusOnPreconditionFailed);
  }

  /**
   * Check a precondition and throw a {@link WebApplicationException} if not met.
   *
   * @param precondition
   * @param statusOnPreconditionFailed
   */
  public static void ensurePrecondition(boolean precondition, HttpStatus statusOnPreconditionFailed) {
    if (precondition) {
      return;
    }
    throw new WebApplicationException(statusOnPreconditionFailed);
  }

  /**
   * A user impersonation is when an application is authenticated using the appkey to act on behalf of a user.
   * This method ensures that if user impersonation is used, it is done by an authorized appkey.
   *
   * @param authentication
   * @param authHeader
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
    throw new WebApplicationException(HttpStatus.FORBIDDEN);
  }

  /**
   * Check if the user represented by the {@link Authentication} has at least one of the
   * provided roles.
   *
   * @param authentication
   * @param roles           this methods will return true if the user is at least in one role. If no role is
   *                        provided this method will return false.
   * @return the user is at least in one of the provided role(s)
   */
  public static boolean checkUserInRole(Authentication authentication, String... roles) {
    Objects.requireNonNull(authentication, "authentication shall be provided");

    if (roles == null || roles.length < 1) {
      return false;
    }

    return Arrays.stream(roles)
        .map(SimpleGrantedAuthority::new)
        .anyMatch(role -> authentication.getAuthorities().contains(role));
  }
}
