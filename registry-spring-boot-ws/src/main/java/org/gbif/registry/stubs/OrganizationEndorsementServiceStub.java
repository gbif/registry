package org.gbif.registry.stubs;

import org.gbif.api.model.registry.Organization;
import org.gbif.registry.ws.surety.OrganizationEndorsementService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Stub class used to simplify stuff, e.g. when this class must be bound but isn't actually used.
 */
@Service
@Qualifier("organizationEndorsementServiceStub")
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
