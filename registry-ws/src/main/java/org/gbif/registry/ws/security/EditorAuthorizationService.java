package org.gbif.registry.ws.security;

import java.security.Principal;
import java.util.UUID;

public interface EditorAuthorizationService {

    /**
     * Checks whether a given user is allowed to modify machine tags belonging to the given namespace.
     * @param user the security context user principal
     * @param ns the namespace in question
     * @return true if the user is allowed to modify the namespace
     */
  boolean allowedToModifyNamespace(Principal user, String ns);

    /**
     * @return true if rights exist for this user to delete the tag
     */
  boolean allowedToDeleteMachineTag(Principal user, int machineTagKey);
  /**
   * Checks whether a given registry entity is explicitly part of the list of entities for an editor user.
   * @param user the security context user principal
   * @param key the entity in question
   * @return true if the passed entity is allowed to be modified
   */
  boolean allowedToModifyEntity(Principal user, UUID key);

  boolean allowedToModifyDataset(Principal user, UUID datasetKey);

  boolean allowedToModifyOrganization(Principal user, UUID orgKey);

  boolean allowedToModifyInstallation(Principal user, UUID installationKey);
}
