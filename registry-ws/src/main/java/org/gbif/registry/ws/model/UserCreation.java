package org.gbif.registry.ws.model;

import org.gbif.api.model.common.AbstractUser;
import org.gbif.api.model.common.User;

import javax.validation.constraints.NotNull;

/**
 * {@link AbstractUser} concrete class that represents a user to be created.
 * We are not using {@link User} directly to avoid confusion with the password/passwordHash field.
 *
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

}
