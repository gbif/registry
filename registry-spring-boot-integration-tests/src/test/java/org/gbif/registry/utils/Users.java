package org.gbif.registry.utils;

import org.gbif.registry.ws.model.UserCreation;

public class Users {

  public static UserCreation prepareUserCreation() {
    UserCreation user = new UserCreation();
    user.setUserName("user_14");
    user.setFirstName("Tim");
    user.setLastName("Robertson");
    user.setPassword("welcome");
    user.getSettings().put("language", "en");
    user.getSettings().put("country", "dk");
    user.setEmail("user_14@gbif.org");
    return user;
  }
}
