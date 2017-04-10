package org.gbif.identity.service;

import org.gbif.api.model.common.User;
import org.gbif.api.model.common.UserUpdate;
import org.gbif.api.vocabulary.UserRole;

import java.util.Set;

/**
 * Simple manager that only copies data from {@link UserUpdate} based on {@link UserRole}.
 */
public class UpdateRulesManager {

  /**
   * Apply updates contained in {@link UserUpdate} to {@link User}.
   *
   * @param initiatorRoles
   * @param user
   * @param userUpdate
   * @return
   */
  public static User applyUpdate(Set<UserRole> initiatorRoles, User user, UserUpdate userUpdate) {
    //common operations
    user.setFirstName(userUpdate.getFirstName());
    user.setLastName(userUpdate.getLastName());
    user.setEmail(userUpdate.getEmail());
    user.setSettings(userUpdate.getSettings());

    if(initiatorRoles.contains(UserRole.REGISTRY_EDITOR) || initiatorRoles.contains(UserRole.REGISTRY_ADMIN)) {
      user.setRoles(userUpdate.getRoles());
    }

    return user;
  }

}
