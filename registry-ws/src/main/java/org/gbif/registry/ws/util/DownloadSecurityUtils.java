package org.gbif.registry.ws.util;

import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;

import java.util.Collection;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;

/**
 * Class that exposes common functions used by services that expose data about occurrence downloads.
 */
public class DownloadSecurityUtils {

  private static final Logger LOG = LoggerFactory.getLogger(DownloadSecurityUtils.class);

  private DownloadSecurityUtils() {
    // private constructor
  }

  /**
   * Checks if the user has the ADMIN_ROLE or is the same user in the current context.
   */
  public static void checkUserIsInSecurityContext(String user, SecurityContext securityContext) {
    // A null securityContext means that the class is executed locally
    if (!isUserAuthorizedInContext(securityContext, user)) {
      LOG.warn(String.format("Unauthorized access detected, authenticated user %s, requested user %s",
        securityContext.getUserPrincipal().getName(), user));
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
  }

  /**
   * Checks if the user has the ADMIN_ROLE or is the same user in the current context.
   */
  public static boolean isUserAuthorizedInContext(SecurityContext securityContext, String user) {
    return (securityContext == null || securityContext.isUserInRole(ADMIN_ROLE) || (securityContext.getUserPrincipal() != null
    && securityContext.getUserPrincipal().getName().equals(user)));
  }

  /**
   * Remove data that shouldn't be publicly exposed.
   */
  public static void clearSensitiveData(SecurityContext securityContext, Download download) {
    if (download.getRequest() != null
      && !isUserAuthorizedInContext(securityContext, download.getRequest().getCreator())) {
      download.getRequest().setNotificationAddresses(null);
    }
  }

  /**
   * Remove data that shouldn't be publicly exposed.
   */
  public static void clearSensitiveData(SecurityContext securityContext,
    Collection<DatasetOccurrenceDownloadUsage> downloadUsages) {
    for (DatasetOccurrenceDownloadUsage downloadUsage : downloadUsages) {
      clearSensitiveData(securityContext, downloadUsage.getDownload());
    }
  }
}
