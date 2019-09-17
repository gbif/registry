package org.gbif.registry.stubs;

import org.gbif.registry.ws.security.EditorAuthorizationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Stub class used to simplify stuff, e.g. when this class must be bound but isn't actually used.
 */
@Service
@Qualifier("editorAuthorizationServiceStub")
public class EditorAuthorizationServiceStub implements EditorAuthorizationService {

  @Override
  public boolean allowedToModifyNamespace(String user, String ns) {
    return false;
  }

  @Override
  public boolean allowedToDeleteMachineTag(String user, int machineTagKey) {
    return false;
  }

  @Override
  public boolean allowedToModifyEntity(String user, UUID key) {
    return false;
  }

  @Override
  public boolean allowedToModifyDataset(String user, UUID datasetKey) {
    return false;
  }

  @Override
  public boolean allowedToModifyOrganization(String user, UUID orgKey) {
    return false;
  }

  @Override
  public boolean allowedToModifyInstallation(String user, UUID installationKey) {
    return false;
  }
}
