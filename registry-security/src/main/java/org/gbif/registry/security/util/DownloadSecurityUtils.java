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
package org.gbif.registry.security.util;

import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.registry.security.SecurityContextCheck;
import org.gbif.ws.WebApplicationException;

import java.text.MessageFormat;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;

/**
 * Class that exposes common functions used by services that expose data about occurrence downloads.
 */
public final class DownloadSecurityUtils {

  private static final Logger LOG = LoggerFactory.getLogger(DownloadSecurityUtils.class);

  private DownloadSecurityUtils() {
    // private constructor
  }

  /** Checks if the user has the ADMIN_ROLE or is the same user in the current context. */
  public static void checkUserIsInSecurityContext(String user, Authentication authentication) {
    // A null securityContext means that the class is executed locally
    if (isUserNotAuthorizedInContext(authentication, user)) {
      LOG.warn("Unauthorized access detected, authenticated use, requested user {}", user);
      throw new WebApplicationException(
          MessageFormat.format(
              "Unauthorized access detected, authenticated use, requested user {0}", user),
          HttpStatus.UNAUTHORIZED);
    }
  }

  /** Checks if the user has the ADMIN_ROLE or is the same user in the current context. */
  private static boolean isUserNotAuthorizedInContext(Authentication authentication, String user) {
    if (authentication == null || authentication.getName() == null) {
      return true;
    }

    if (authentication.getName().equals(user)) {
      return false;
    }

    return !SecurityContextCheck.checkUserInRole(authentication, ADMIN_ROLE);
  }

  /** Remove data that shouldn't be publicly exposed. */
  public static void clearSensitiveData(Authentication authentication, Download download) {
    if (download != null
        && isUserNotAuthorizedInContext(authentication, download.getRequest().getCreator())) {
      download.getRequest().setNotificationAddresses(null);
      download.getRequest().setCreator(null);
    }
  }

  /** Remove data that shouldn't be publicly exposed. */
  public static void clearSensitiveData(
      Authentication authentication, Collection<DatasetOccurrenceDownloadUsage> downloadUsages) {
    for (DatasetOccurrenceDownloadUsage downloadUsage : downloadUsages) {
      clearSensitiveData(authentication, downloadUsage.getDownload());
    }
  }
}
