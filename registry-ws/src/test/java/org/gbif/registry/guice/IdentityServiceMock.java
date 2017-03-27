package org.gbif.registry.guice;

import org.gbif.api.model.common.User;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.identity.model.Session;
import org.gbif.identity.model.UserCreationResult;

import java.util.Date;
import java.util.Map;
import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Class that implements an in-memory users store.
 * This class is intended to be use in IT and unit tests ONLY.
 */
public class IdentityServiceMock implements IdentityService {

  private static final Map<String, User> USERS = Maps.newHashMap();
  static{
    USERS.put("admin", mockUser(1, "admin", UserRole.REGISTRY_ADMIN));
    USERS.put("user", mockUser(2, "user", UserRole.USER));
    USERS.put("editor", mockUser(3, "editor", UserRole.REGISTRY_EDITOR));
  }

  public static final String EMAIL_AT_GIBF = "%s@gbif.test.org";

  @Nullable
  @Override
  public User getByKey(int id) {
    return USERS.entrySet().stream()
            .map(Map.Entry::getValue)
            .filter(user -> user.getKey().equals(id))
            .findFirst()
            .map( user -> copyUserAfterLogin(user))
            .orElse(null);
  }

  /**
   * Authenticates the user by a simple match of username and pwd.
   */
  @Override
  public User authenticate(String username, String passwordHash) {
    if (USERS.containsKey(username) && USERS.get(username).getPasswordHash().equals(passwordHash)) {
      return copyUserAfterLogin(USERS.get(username));
    }
    return null;
  }

  @Override
  public UserCreationResult create(User user, String password) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void delete(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void update(User user) {
    throw new UnsupportedOperationException();
  }

  @Override
  public User get(String username) {
    if (USERS.containsKey(username)) {
      return copyUserAfterLogin(USERS.get(username));
    }
    return null;
  }

  @Override
  public PagingResponse<User> list(@Nullable Pageable pageable) {
    // TODO: Write implementation
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * The session key must be equals to the user name.
   */
  @Override
  public User getBySession(String session) {
    return mockUser(1, session, null);
  }

  @Override
  public PagingResponse<User> search(String query, @Nullable Pageable page) {
    // TODO: Write implementation
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public Session createSession(String username) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void terminateSession(String session) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void terminateAllSessions(String username) {
    throw new UnsupportedOperationException();
  }

  /**
   * Return a copy of the user with the lastLogin date set to now.
   * @param user
   * @return
   */
  private static User copyUserAfterLogin(User user) {
    User userCopy = new User();
    userCopy.setUserName(user.getUserName());
    userCopy.setPasswordHash(user.getPasswordHash());
    userCopy.setLastLogin(new Date());
    userCopy.setEmail(user.getEmail());
    userCopy.setFirstName(user.getFirstName());
    userCopy.setLastName(user.getLastName());
    userCopy.setKey(user.getKey());
    if (user.getRoles()!=null) {
      userCopy.setRoles(Sets.newHashSet(user.getRoles()));
    }
    return userCopy;
  }

  /**
   * Creates a full user instance using the username as passwordhash
   */
  private static User mockUser(int key, String username, UserRole... roles) {
    User user = new User();
    user.setUserName(username);
    user.setPasswordHash(username);
    user.setLastLogin(new Date());
    user.setEmail(String.format(EMAIL_AT_GIBF, username));
    user.setFirstName(username);
    user.setLastName(username);
    user.setKey(key);
    if (roles!=null) {
      user.setRoles(Sets.newHashSet(roles));
    }
    return user;
  }

}
