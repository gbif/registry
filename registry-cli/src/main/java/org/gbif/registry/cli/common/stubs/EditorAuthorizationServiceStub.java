package org.gbif.registry.cli.common.stubs;

import org.gbif.registry.ws.security.EditorAuthorizationService;

import java.security.Principal;
import java.util.UUID;

/**
 * Stub class used to simplify Guice binding, e.g. when this class must be bound but isn't used by CLI.
 */
public class EditorAuthorizationServiceStub implements EditorAuthorizationService  {

  @Override
  public boolean allowedToModifyNamespace(Principal user, String ns) {
    return false;
  }

  @Override
  public boolean allowedToDeleteMachineTag(Principal user, int machineTagKey) {
    return false;
  }

  @Override
  public boolean allowedToModifyEntity(Principal user, UUID key) {
    return false;
  }

  @Override
  public boolean allowedToModifyDataset(Principal user, UUID datasetKey) {
    return false;
  }

  @Override
  public boolean allowedToModifyOrganization(Principal user, UUID orgKey) {
    return false;
  }

  @Override
  public boolean allowedToModifyInstallation(Principal user, UUID installationKey) {
    return false;
  }
}
