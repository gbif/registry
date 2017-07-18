package org.gbif.registry.ws.security;

import org.gbif.ws.security.GbifAuthService;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.registry.ws.security.UserRoles.APP_ROLE;

/**
 * Utility methods to check conditions on {@link SecurityContext}
 *
 * Convention:
 *  - ensure methods throws exception
 *  - check methods: return boolean
 */
public class SecurityContextCheck {

  private static final Logger LOG = LoggerFactory.getLogger(SecurityContextCheck.class);

  /**
   * Utility class
   */
  private SecurityContextCheck(){}

  /**
   * Check that a user is present in the getUserPrincipal of the SecurityContext otherwise throw
   * WebApplicationException UNAUTHORIZED.
   *
   * @param securityContext
   *
   * @throws WebApplicationException UNAUTHORIZED if the user is not present in the {@link SecurityContext}
   */
  public static void ensureUserSetInSecurityContext(final SecurityContext securityContext)
          throws WebApplicationException {
    if (securityContext == null || securityContext.getUserPrincipal() == null ||
            StringUtils.isBlank(securityContext.getUserPrincipal().getName())) {
      LOG.warn("The user must be identified by the username. AuthenticationScheme: {}",
              securityContext.getAuthenticationScheme());
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
  }

  /**
   * Ensure a {@link SecurityContext} was obtained using the {@link GbifAuthService#GBIF_SCHEME} authentication scheme.
   * If the {@link SecurityContext} is null, this method will throw {@link WebApplicationException} FORBIDDEN.
   * @param security
   *
   * @throws WebApplicationException FORBIDDEN if the {@link SecurityContext} is null or was not obtained using the GBIF
   * authentication scheme.
   */
  public static void ensureGbifScheme(final SecurityContext security) {
    if(security != null && GbifAuthService.GBIF_SCHEME.equals(security.getAuthenticationScheme())){
      return;
    }
    throw new WebApplicationException(Response.Status.FORBIDDEN);
  }

  /**
   * A user impersonation is when an application is authenticated using the appkey to act on behalf of a user.
   * This method ensures that if user impersonation is used, it is done by an authorized appkey.
   * @param security
   * @param request
   * @param appKeyWhitelist depending on the context the appKey whitelist may be different
   */
  public static void ensureAuthorizedUserImpersonation(final SecurityContext security, final HttpServletRequest request,
                                                       final List<String> appKeyWhitelist) {
    ensureUserSetInSecurityContext(security);
    ensureGbifScheme(security);

    String appKey = GbifAuthService.getAppKeyFromRequest(request::getHeader);
    if(appKeyWhitelist.contains(appKey)){
      return;
    }
    throw new WebApplicationException(Response.Status.FORBIDDEN);
  }

  /**
   * Currently, the best way to determine if we have user impersonation is to check the
   * authentication scheme and the roles.
   *
   * @param security
   * @return if the {@link SecurityContext} is using user impersonation or not
   */
  public static boolean isUsingUserImpersonation(final SecurityContext security) {
    return security != null &&
            GbifAuthService.GBIF_SCHEME.equals(security.getAuthenticationScheme())
            && !security.isUserInRole(APP_ROLE);
  }

  /**
   * Check if the user represented by the {@link SecurityContext} has at least one of the
   * provided roles.
   *
   * @param securityContext
   * @param roles           this methods will return true if a the user is at least int one role. If no role is provided
   *                        this method will return false.
   *
   * @return the user is at least in one of the provided role(s)
   */
  public static boolean checkUserInRole(SecurityContext securityContext, String... roles) {
    Objects.requireNonNull(securityContext, "securityContext shall be provided");

    if(roles == null || roles.length < 1) {
      return false;
    }
    return Arrays.stream(roles)
            .filter(securityContext::isUserInRole)
            .findFirst().isPresent();
  }
}
