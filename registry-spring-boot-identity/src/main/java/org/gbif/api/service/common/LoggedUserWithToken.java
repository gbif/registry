package org.gbif.api.service.common;

import com.google.common.collect.Maps;
import org.gbif.api.model.common.GbifUser;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class LoggedUserWithToken {

  private String userName;
  private String firstName;
  private String lastName;
  private String email;
  private final Map<String, String> settings = Maps.newHashMap();
  private String token;
  private Set<String> roles = new HashSet<>();
  private Set<String> editorRoleScopes = new HashSet<>();

  /**
   * Only used for json deserialization
   */
  public LoggedUserWithToken() {
  }

  private LoggedUserWithToken(GbifUser user, String token, List<UUID> editorRights) {
    this.userName = user.getUserName();
    this.firstName = user.getFirstName();
    this.lastName = user.getLastName();
    this.email = user.getEmail();
    this.settings.putAll(user.getSettings());
    this.token = token;
    Optional.ofNullable(user.getRoles()).ifPresent(userRoles -> userRoles.forEach(role -> this.roles.add(role.name())));
    Optional.ofNullable(editorRights).ifPresent(rights -> rights.forEach(v -> this.editorRoleScopes.add(v.toString())));
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
