package org.gbif.api.model.common;

import javax.validation.constraints.NotNull;

/**
 * {@link AbstractUser} implementation that represents a user to be created.
 */
public class UserCreation extends AbstractUser {

  private String password;

  @NotNull
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public static User toUser(UserCreation userCreation) {
    User user = new User();
    user.userName = userCreation.userName;
    user.firstName = userCreation.firstName;
    user.lastName = userCreation.lastName;
    user.email = userCreation.email;
    user.roles = userCreation.roles;
    user.settings = userCreation.settings;
    return user;
  }
}
