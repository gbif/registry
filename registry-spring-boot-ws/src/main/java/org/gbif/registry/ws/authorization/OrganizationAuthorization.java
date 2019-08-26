package org.gbif.registry.ws.authorization;

import org.gbif.api.model.registry.Organization;
import org.gbif.registry.ws.security.SecurityContextCheck;
import org.springframework.security.core.Authentication;

import javax.annotation.Nullable;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;

/**
 * The purpose of this class is to centralize authorization related checks.
 */
public class OrganizationAuthorization {

  private OrganizationAuthorization() {
  }

  public static boolean isUpdateAuthorized(@Nullable Organization previousOrg, Organization organization, Authentication authentication) {
    if (previousOrg == null) {
      return false;
    }

    boolean endorsementApprovedChanged = previousOrg.isEndorsementApproved() != organization.isEndorsementApproved();
    return !endorsementApprovedChanged || SecurityContextCheck.checkUserInRole(authentication, ADMIN_ROLE);
  }
}
