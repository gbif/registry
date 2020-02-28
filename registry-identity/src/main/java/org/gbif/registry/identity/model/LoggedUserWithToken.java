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
package org.gbif.registry.identity.model;

import org.gbif.api.model.common.GbifUser;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Maps;

public class LoggedUserWithToken {

  private String userName;
  private String firstName;
  private String lastName;
  private String email;
  private final Map<String, String> settings = Maps.newHashMap();
  private String token;
  private Set<String> roles = new HashSet<>();
  private Set<String> editorRoleScopes = new HashSet<>();

  /** Only used for json deserialization */
  public LoggedUserWithToken() {}

  private LoggedUserWithToken(GbifUser user, String token, List<UUID> editorRights) {
    this.userName = user.getUserName();
    this.firstName = user.getFirstName();
    this.lastName = user.getLastName();
    this.email = user.getEmail();
    this.settings.putAll(user.getSettings());
    this.token = token;
    Optional.ofNullable(user.getRoles())
        .ifPresent(userRoles -> userRoles.forEach(role -> this.roles.add(role.name())));
    Optional.ofNullable(editorRights)
        .ifPresent(rights -> rights.forEach(v -> this.editorRoleScopes.add(v.toString())));
  }

  public static LoggedUserWithToken from(GbifUser user, String token, List<UUID> editorRights) {
    if (user == null) {
      return null;
    }
    return new LoggedUserWithToken(user, token, editorRights);
  }

  public String getUserName() {
    return userName;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getEmail() {
    return email;
  }

  public Map<String, String> getSettings() {
    return settings;
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
