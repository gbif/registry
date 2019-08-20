package org.gbif.registry.ws.security;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

public interface EditorAuthorizationService {

  /**
   * Checks whether a given user is allowed to modify machine tags belonging to the given namespace.
   * @param user the security context user principal
   * @param ns the namespace in question
   * @return true if the user is allowed to modify the namespace
   */
  boolean allowedToModifyNamespace(UserDetails user, String ns);

  /**
   * @return true if rights exist for this user to delete the tag
   */
  boolean allowedToDeleteMachineTag(UserDetails user, int machineTagKey);
  /**
   * Checks whether a given registry entity is explicitly part of the list of entities for an editor user.
   * @param user the security context user principal
   * @param key the entity in question
   * @return true if the passed entity is allowed to be modified
   */
  boolean allowedToModifyEntity(UserDetails user, UUID key);

  boolean allowedToModifyDataset(UserDetails user, UUID datasetKey);

  boolean allowedToModifyOrganization(UserDetails user, UUID orgKey);

  boolean allowedToModifyInstallation(UserDetails user, UUID installationKey);
}
