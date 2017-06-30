package org.gbif.registry.ws.resources;

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
