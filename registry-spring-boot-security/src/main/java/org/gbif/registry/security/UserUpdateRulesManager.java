/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.security;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.domain.ws.UserCreation;
import org.gbif.registry.domain.ws.UserUpdate;

import java.util.Set;

import javax.annotation.Nullable;

/** Simple manager that only copies data from {@link UserUpdate} based on {@link UserRole}. */
public class UserUpdateRulesManager {

  private UserUpdateRulesManager() {}

  /**
   * Apply updates contained in {@link UserUpdate} to {@link GbifUser}.
   *
   * @param initiatorRoles can be null if the update if not initiated by a user (e.g. another app)
   * @param user
   * @param userUpdate
   * @param fromTrustedApp
   * @return
   */
  public static GbifUser applyUpdate(
      @Nullable Set<UserRole> initiatorRoles,
      GbifUser user,
      UserUpdate userUpdate,
      boolean fromTrustedApp) {

    boolean isAdmin = initiatorRoles != null && (initiatorRoles.contains(UserRole.REGISTRY_ADMIN));

    // common operations
    user.setFirstName(userUpdate.getFirstName());
    user.setLastName(userUpdate.getLastName());
    user.setEmail(userUpdate.getEmail());
    user.setSettings(userUpdate.getSettings());

    if (isAdmin) {
      user.setRoles(userUpdate.getRoles());
      user.setDeleted(userUpdate.getDeleted());
    }

    if (fromTrustedApp || isAdmin) {
      user.setSystemSettings(userUpdate.getSystemSettings());
    }
    return user;
  }

  /**
   * Create a {@link GbifUser} from a {@link UserCreation}.
   *
   * @param userCreate
   * @return
   */
  public static GbifUser applyCreate(UserCreation userCreate) {
    GbifUser user = new GbifUser();
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
