package org.gbif.registry.ws.surety;

import org.gbif.api.model.registry.Organization;

import java.util.UUID;

/**
 * Service responsible to handle the business logic of creating and confirming {@link Organization}
 */
public interface OrganizationEndorsementService<T> {

  void onNewOrganization(Organization newOrganization);

  /**
   *
   * @param organizationKey
   * @param confirmationObject object used to handle confirmation by the implementation.
   * @return
   */
  boolean confirmOrganization(UUID organizationKey, T confirmationObject);

}
