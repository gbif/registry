package org.gbif.registry.ws.security;

import org.gbif.api.model.common.User;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.ws.model.UserCreation;
import org.gbif.registry.ws.model.UserUpdate;

import java.util.Set;
import javax.annotation.Nullable;

/**
 * Simple manager that only copies data from {@link UserUpdate} based on {@link UserRole}.
 */
public class UpdateRulesManager {

  /**
   * Apply updates contained in {@link UserUpdate} to {@link User}.
   *
   * @param initiatorRoles can be null if the update if not initiated by a user (e.g. another app)
   * @param user
   * @param userUpdate
   * @return
   */
  public static User applyUpdate(@Nullable Set<UserRole> initiatorRoles, User user, UserUpdate userUpdate, boolean fromTrustedApp) {

    boolean isAdmin = initiatorRoles != null && (initiatorRoles.contains(UserRole.REGISTRY_ADMIN));

    //common operations
    user.setFirstName(userUpdate.getFirstName());
    user.setLastName(userUpdate.getLastName());
    user.setEmail(userUpdate.getEmail());
    user.setSettings(userUpdate.getSettings());

    if(isAdmin) {
      user.setRoles(userUpdate.getRoles());
      user.setDeleted(userUpdate.getDeleted());
    }

    if(fromTrustedApp || isAdmin) {
      user.setSystemSettings(userUpdate.getSystemSettings());
    }

    return user;
  }

  /**
   * Create a {@link User} from a {@link UserCreation}.
   * @param userCreate
   * @return
   */
  public static User applyCreate(UserCreation userCreate) {
    User user = new User();
    user.setUserName(userCreate.getUserName());
    user.setFirstName(userCreate.getFirstName());
    user.setLastName(userCreate.getLastName());
    user.setEmail(userCreate.getEmail());
    user.setSettings(userCreate.getSettings());
    user.setSystemSettings(userCreate.getSystemSettings());
    user.getRoles().add(UserRole.USER);
    return user;
  }

}
