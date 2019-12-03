package org.gbif.registry.utils;

import org.gbif.registry.domain.ws.UserCreation;

public class Users {

  public static UserCreation prepareUserCreation(String username, String password) {
    UserCreation user = new UserCreation();
    user.setUserName(username);
    user.setFirstName("Tim");
    user.setLastName("Robertson");
    user.setPassword(password);
    user.getSettings().put("language", "en");
    user.getSettings().put("country", "dk");
    user.setEmail(username + "@gbif.org");
    return user;
  }
}
