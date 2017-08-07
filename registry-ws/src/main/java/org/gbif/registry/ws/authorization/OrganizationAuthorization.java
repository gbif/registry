package org.gbif.registry.ws.authorization;

import org.gbif.api.model.registry.Organization;

import javax.ws.rs.core.SecurityContext;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;

/**
 * The purpose of this class is to centralize authorization related checks.
 */
public class OrganizationAuthorization {

  private OrganizationAuthorization() {}

  public static boolean isUpdateAuthorized(Organization previousOrg, Organization organization, SecurityContext securityContext) {
    boolean endorsementApprovedChanged = previousOrg.isEndorsementApproved() != organization.isEndorsementApproved();
    return !endorsementApprovedChanged || securityContext.isUserInRole(ADMIN_ROLE);
  }

}
