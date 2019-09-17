package org.gbif.registry.ws.util;

import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.registry.ws.security.SecurityContextCheck;
import org.gbif.ws.WebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.util.Collection;

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
  public static void checkUserIsInSecurityContext(String user, Authentication authentication) {
    // A null securityContext means that the class is executed locally
    if (!isUserAuthorizedInContext(authentication, user)) {
      LOG.warn("Unauthorized access detected, authenticated use, requested user {}", user);
      throw new WebApplicationException(HttpStatus.UNAUTHORIZED);
    }
  }

  /**
   * Checks if the user has the ADMIN_ROLE or is the same user in the current context.
   */
  public static boolean isUserAuthorizedInContext(Authentication authentication, String user) {
    return (authentication == null || SecurityContextCheck.checkUserInRole(authentication, ADMIN_ROLE)
        || (authentication.getPrincipal() != null && authentication.getName().equals(user)));
  }

  /**
   * Remove data that shouldn't be publicly exposed.
   */
  public static void clearSensitiveData(Authentication authentication, Download download) {
    if (download != null && download.getRequest() != null
        && !isUserAuthorizedInContext(authentication, download.getRequest().getCreator())) {
      download.getRequest().setNotificationAddresses(null);
      download.getRequest().setCreator(null);
    }
  }

  /**
   * Remove data that shouldn't be publicly exposed.
   */
  public static void clearSensitiveData(Authentication authentication,
                                        Collection<DatasetOccurrenceDownloadUsage> downloadUsages) {
    for (DatasetOccurrenceDownloadUsage downloadUsage : downloadUsages) {
      clearSensitiveData(authentication, downloadUsage.getDownload());
    }
  }
}
