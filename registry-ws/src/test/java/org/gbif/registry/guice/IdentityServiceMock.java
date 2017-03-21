package org.gbif.registry.guice;

import org.gbif.api.model.common.User;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.service.common.UserService;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.identity.model.Session;

import java.util.Date;
import java.util.EnumSet;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Class that implements an in-memory users store.
 * This class is intended to be use in IT and unit tests only.
 */
public class IdentityServiceMock implements IdentityService {

  private static final Map<Integer, User> USERS = Maps.newHashMap();
  USERS.put(1, mockUser(1, "trobertson", "abc", UserRole.USER, UserRole.REGISTRY_EDITOR, UserRole.REGISTRY_ADMIN));
  USERS.put(2, mockUser(2, "mdoering", "efg", UserRole.USER, UserRole.REGISTRY_EDITOR, UserRole.REGISTRY_ADMIN));
  USERS.put(3, mockUser(3, "jblogs", "abc", UserRole.USER));


  public static final String EMAIL_AT_GIBF = "%s@gbif.test.org";

  @Nullable
  @Override
  public User getByKey(int id) {


    // TODO: Write implementation
    throw new UnsupportedOperationException("Not implemented yet");
  }

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


  @Override
  public String create(User user) {
    // TODO: Write implementation
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void delete(String s) {
    // TODO: Write implementation
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public User get(String username) {
    if (USERS.containsKey(username)) {
      return mockUser(username, null);
    }
    return null;
  }

  @Override
  public PagingResponse<User> list(@Nullable Pageable pageable) {
    // TODO: Write implementation
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void update(User user) {
    // TODO: Write implementation
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * The session key must be equals to the user name.
   */
  @Override
  public User getBySession(String session) {
    return mockUser(session, null);
  }

  @Override
  public PagingResponse<User> search(
    String query, @Nullable Pageable page
  ) {
    // TODO: Write implementation
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public Session createSession(String username) {
    // TODO: Write implementation
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void terminateSession(String session) {
    // TODO: Write implementation
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void terminateAllSessions(String username) {
    // TODO: Write implementation
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * Creates a full user instance.
   */
  private static User mockUser(int key, String username, String passwordHash, UserRole... roles) {
    User user = new User();
    user.setUserName(username);
    user.setPasswordHash(passwordHash);
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
