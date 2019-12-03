package org.gbif.registry.domain.ws;

import org.gbif.api.model.common.AbstractGbifUser;
import org.gbif.api.model.common.GbifUser;

import javax.validation.constraints.NotNull;

/**
 * {@link AbstractGbifUser} concrete class that represents a user to be created.
 * We are not using {@link GbifUser} directly to avoid confusion with the password/passwordHash field.
 *
 */
public class UserCreation extends AbstractGbifUser {

  private String password;

  @NotNull
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
