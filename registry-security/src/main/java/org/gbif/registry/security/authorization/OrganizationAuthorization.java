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
package org.gbif.registry.security.authorization;

import org.gbif.api.model.registry.Organization;
import org.gbif.registry.security.SecurityContextCheck;

import javax.annotation.Nullable;

import org.springframework.security.core.Authentication;

import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;

/** The purpose of this class is to centralize authorization related checks. */
public class OrganizationAuthorization {

  private OrganizationAuthorization() {}

  public static boolean isUpdateAuthorized(
      @Nullable Organization previousOrg,
      Organization organization,
      Authentication authentication) {
    if (previousOrg == null) {
      return false;
    }

    boolean endorsementApprovedChanged =
        previousOrg.isEndorsementApproved() != organization.isEndorsementApproved();
    return !endorsementApprovedChanged
        || SecurityContextCheck.checkUserInRole(authentication, ADMIN_ROLE);
  }
}
