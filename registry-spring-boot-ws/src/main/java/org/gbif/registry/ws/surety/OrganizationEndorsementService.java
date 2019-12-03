package org.gbif.registry.ws.surety;

import org.gbif.api.model.registry.Organization;

import java.util.UUID;

/**
 * Service responsible to handle the business logic of creating and confirming {@link Organization}.
 */
public interface OrganizationEndorsementService<T> {

  /**
   * Handles the logic on new organization.
   *
   * @param newOrganization new organization
   */
  void onNewOrganization(Organization newOrganization);

  /**
   * Confirm the endorsement of an organization using confirmation object.
   *
   * @param organizationKey    organization key
   * @param confirmationObject object used to handle confirmation by the implementation.
   * @return the organization endorsement was approved or not
   */
  boolean confirmEndorsement(UUID organizationKey, T confirmationObject);
}
