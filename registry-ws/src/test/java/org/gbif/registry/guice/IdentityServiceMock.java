package org.gbif.registry.guice;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.identity.model.UserModelMutationResult;
import org.gbif.registry.ws.fixtures.TestConstants;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;

/**
 * Class that implements an in-memory users store.
 * This class is intended to be use in IT and unit tests ONLY and only provides methods to authenticate.
 */
public class IdentityServiceMock implements IdentityService {

  private static final Map<String, GbifUser> USERS = Maps.newHashMap();
  static{
    USERS.put(TestConstants.TEST_ADMIN, mockUser(1, TestConstants.TEST_ADMIN,
            TestConstants.getTestUserRole(TestConstants.TEST_ADMIN)));
    USERS.put(TestConstants.TEST_EDITOR, mockUser(2, TestConstants.TEST_EDITOR,
            TestConstants.getTestUserRole(TestConstants.TEST_EDITOR)));
    USERS.put(TestConstants.TEST_USER, mockUser(3, TestConstants.TEST_USER,
            TestConstants.getTestUserRole(TestConstants.TEST_USER)));
  }

  private static final String EMAIL_AT_GIBF = "%s@gbif.test.org";

  @Nullable
  @Override
  public GbifUser getByKey(int id) {
    return USERS.entrySet().stream()
            .map(Map.Entry::getValue)
            .filter(user -> user.getKey().equals(id))
            .findFirst()
            .map(IdentityServiceMock::copyUserAfterLogin)
            .orElse(null);
  }

  /**
   * Authenticates the user by a simple match of username and pwd.
   */
  @Override
  public GbifUser authenticate(String username, String passwordHash) {
    if (USERS.containsKey(username) && USERS.get(username).getPasswordHash().equals(passwordHash)) {
      return copyUserAfterLogin(USERS.get(username));
    }
    return null;
  }

  @Override
  public void delete(int key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public UserModelMutationResult update(GbifUser user) {
    throw new UnsupportedOperationException();
  }

  @Override
  public GbifUser get(String username) {
    if (USERS.containsKey(username)) {
      return copyUserAfterLogin(USERS.get(username));
    }
    return null;
  }

  @Nullable
  @Override
  public GbifUser getByEmail(String email) {
    return USERS.entrySet().stream()
            .map(Map.Entry::getValue)
            .filter(user -> user.getEmail().equals(email))
            .findFirst()
            .map(IdentityServiceMock::copyUserAfterLogin)
            .orElse(null);
  }

  @Nullable
  @Override
  public GbifUser getByIdentifier(String identifier) {
    return StringUtils.contains(identifier, "@") ?
            getByEmail(identifier) :get(identifier);
  }

  @Override
  public PagingResponse<GbifUser> list(@Nullable Pageable pageable) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PagingResponse<GbifUser> search(String query, @Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public UserModelMutationResult create(GbifUser user, String password) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void updateLastLogin(int userKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isConfirmationKeyValid(int userKey, UUID challengeCode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean confirmUser(int userKey, UUID challengeCode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasPendingConfirmation(int userKey) {
    return true;
  }

  @Override
  public UserModelMutationResult updatePassword(int userKey, String newPassword, UUID challengeCode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public UserModelMutationResult updatePassword(int userKey, String newPassword) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void resetPassword(int userKey) {
    throw new UnsupportedOperationException();
  }

  /**
   * Return a copy of the user with the lastLogin date set to now.
   * @param user
   * @return
   */
  private static GbifUser copyUserAfterLogin(GbifUser user) {
    GbifUser userCopy = new GbifUser();
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
  private static GbifUser mockUser(int key, String username, UserRole... roles) {
    GbifUser user = new GbifUser();
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
