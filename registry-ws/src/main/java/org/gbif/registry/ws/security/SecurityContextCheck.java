package org.gbif.registry.ws.security;

import java.util.Arrays;
import java.util.Objects;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  public static void ensureUserSetInSecurityContext(SecurityContext securityContext)
          throws WebApplicationException {
    if (securityContext == null || securityContext.getUserPrincipal() == null ||
            StringUtils.isBlank(securityContext.getUserPrincipal().getName())) {
      LOG.warn("The user must be identified by the username. AuthenticationScheme: {}",
              securityContext.getAuthenticationScheme());
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
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
