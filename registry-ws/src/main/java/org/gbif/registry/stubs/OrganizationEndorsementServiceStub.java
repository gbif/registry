package org.gbif.registry.stubs;

import org.gbif.api.model.registry.Organization;
import org.gbif.registry.ws.surety.OrganizationEndorsementService;

import java.util.UUID;

/**
 * Stub class used to simplify Guice binding, e.g. when this class must be bound but isn't actually used.
 */
public class OrganizationEndorsementServiceStub implements OrganizationEndorsementService<UUID> {

  @Override
  public void onNewOrganization(Organization newOrganization) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean confirmEndorsement(UUID organizationKey, UUID confirmationObject) {
    throw new UnsupportedOperationException();
  }
}
