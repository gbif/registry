package org.gbif.registry.stubs;

import org.gbif.registry.ws.security.EditorAuthorizationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Stub class used to simplify stuff, e.g. when this class must be bound but isn't actually used.
 */
@Service
@Qualifier("editorAuthorizationServiceStub")
public class EditorAuthorizationServiceStub implements EditorAuthorizationService {

  @Override
  public boolean allowedToModifyNamespace(UserDetails user, String ns) {
    return false;
  }

  @Override
  public boolean allowedToDeleteMachineTag(UserDetails user, int machineTagKey) {
    return false;
  }

  @Override
  public boolean allowedToModifyEntity(UserDetails user, UUID key) {
    return false;
  }

  @Override
  public boolean allowedToModifyDataset(UserDetails user, UUID datasetKey) {
    return false;
  }

  @Override
  public boolean allowedToModifyOrganization(UserDetails user, UUID orgKey) {
    return false;
  }

  @Override
  public boolean allowedToModifyInstallation(UserDetails user, UUID installationKey) {
    return false;
  }
}
