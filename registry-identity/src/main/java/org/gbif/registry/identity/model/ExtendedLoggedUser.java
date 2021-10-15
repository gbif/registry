/*
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
package org.gbif.registry.identity.model;

import org.gbif.api.model.common.GbifUser;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Extended {@link LoggedUser} which additionally contains token, roles and editor scopes. */
public class ExtendedLoggedUser extends LoggedUser {

  private String token;
  private Set<String> roles = new HashSet<>();
  private Set<String> editorRoleScopes = new HashSet<>();

  /** Only used for json deserialization */
  public ExtendedLoggedUser() {}

  private ExtendedLoggedUser(GbifUser user, String token, List<UUID> editorRights) {
    super(user);
    this.token = token;
    Optional.ofNullable(user.getRoles())
        .ifPresent(userRoles -> userRoles.forEach(role -> this.roles.add(role.name())));
    Optional.ofNullable(editorRights)
        .ifPresent(rights -> rights.forEach(v -> this.editorRoleScopes.add(v.toString())));
  }

  public static ExtendedLoggedUser from(GbifUser user, String token, List<UUID> editorRights) {
    if (user == null) {
      return null;
    }
    return new ExtendedLoggedUser(user, token, editorRights);
  }

  public String getToken() {
    return token;
  }

  public Set<String> getRoles() {
    return roles;
  }

  public Set<String> getEditorRoleScopes() {
    return editorRoleScopes;
  }
}
