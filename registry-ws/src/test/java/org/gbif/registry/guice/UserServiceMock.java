package org.gbif.registry.guice;

import org.gbif.api.model.common.User;
import org.gbif.api.service.common.UserService;
import org.gbif.api.vocabulary.UserRole;

import java.util.Date;
import java.util.EnumSet;

import com.google.common.collect.ImmutableMap;

/**
 * Class that implements an in-memory users store.
 * This class is intended to be use in IT and unit tests only.
 */
public class UserServiceMock implements UserService {

  private static final ImmutableMap<String, String> USERS = new ImmutableMap.Builder<String, String>()
    .put("admin", "admin").put("user", "user").put("editor", "editor").build();

  private static final ImmutableMap<String, EnumSet<UserRole>> USER_ROLES =
    new ImmutableMap.Builder<String, EnumSet<UserRole>>()
      .put("admin", EnumSet.of(UserRole.REGISTRY_ADMIN))
      .put("user", EnumSet.of(UserRole.USER))
      .put("editor", EnumSet.of(UserRole.REGISTRY_EDITOR))
      .build();

  public static final String EMAIL_AT_GIBF = "%s@gbif.test.org";

  /**
   * Authenticates the user by a simple match of username and pwd.
   */
  @Override
  public User authenticate(String username, String password) {
    if (USERS.containsKey(username) && USERS.get(username).equals(password)) {
      return mockUser(username, password);
    }
    return null;
  }

  /**
   * Creates a full user instance.
   */
  private User mockUser(String username, String password) {
    User user = new User();
    user.setUserName(username);
    user.setPasswordHash(password);
    user.setLastLogin(new Date());
    user.setEmail(String.format(EMAIL_AT_GIBF, username));
    user.setFirstName(username);
    user.setLastName(username);
    user.setKey(USERS.keySet().asList().indexOf(username));
    user.setRoles(USER_ROLES.get(username));
    return user;
  }


  @Override
  public User get(String username) {
    if (USERS.containsKey(username)) {
      return mockUser(username, null);
    }
    return null;
  }

  /**
   * The session key must be equals to the user name.
   */
  @Override
  public User getBySession(String session) {
    return mockUser(session, null);
  }

}
