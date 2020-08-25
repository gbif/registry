/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.test.mocks;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.identity.model.UserModelMutationResult;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.ws.it.fixtures.TestConstants;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Class that implements an in-memory users store. This class is intended to be use in IT and unit
 * tests ONLY and only provides methods to authenticate.
 */
public class IdentityServiceMock implements IdentityService {

  private static final Map<String, GbifUser> USERS = Maps.newHashMap();

  static {
    USERS.put(
        TestConstants.TEST_ADMIN,
        mockUser(
            1, TestConstants.TEST_ADMIN, TestConstants.getTestUserRole(TestConstants.TEST_ADMIN)));
    USERS.put(
        TestConstants.TEST_EDITOR,
        mockUser(
            2,
            TestConstants.TEST_EDITOR,
            TestConstants.getTestUserRole(TestConstants.TEST_EDITOR)));
    USERS.put(
        TestConstants.TEST_USER,
        mockUser(
            3, TestConstants.TEST_USER, TestConstants.getTestUserRole(TestConstants.TEST_USER)));
    USERS.put(
        TestConstants.TEST_GRSCICOLL_ADMIN,
        mockUser(
            4,
            TestConstants.TEST_GRSCICOLL_ADMIN,
            TestConstants.getTestUserRole(TestConstants.TEST_GRSCICOLL_ADMIN)));
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

  @Nullable
  @Override
  public GbifUser getBySystemSetting(String key, String value) {
    return null;
  }

  /** Authenticates the user by a simple match of username and pwd. */
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
  public void delete(GbifUser userBefore, GbifUser user, List<Download> downloads) {
    throw new UnsupportedOperationException();
  }

  @Override
  public UserModelMutationResult update(GbifUser user) {
    throw new UnsupportedOperationException();
  }

  @Override
  public GbifUser get(String identifier) {
    if (USERS.containsKey(identifier)) {
      return copyUserAfterLogin(USERS.get(identifier));
    }
    return getByEmail(identifier);
  }

  @Nullable
  public GbifUser getByEmail(String email) {
    return USERS.entrySet().stream()
        .map(Map.Entry::getValue)
        .filter(user -> user.getEmail().equals(email))
        .findFirst()
        .map(IdentityServiceMock::copyUserAfterLogin)
        .orElse(null);
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
  public boolean confirmUser(int userKey, UUID challengeCode, boolean emailEnabled) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasPendingConfirmation(int userKey) {
    return true;
  }

  @Override
  public UserModelMutationResult updatePassword(
      int userKey, String newPassword, UUID challengeCode) {
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

  @Override
  public List<UUID> listEditorRights(String userName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addEditorRight(String userName, UUID key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteEditorRight(String userName, UUID key) {
    throw new UnsupportedOperationException();
  }

  /**
   * Return a copy of the user with the lastLogin date set to now.
   *
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
    if (user.getRoles() != null) {
      userCopy.setRoles(Sets.newHashSet(user.getRoles()));
    }
    return userCopy;
  }

  /** Creates a full user instance using the username as passwordhash */
  private static GbifUser mockUser(int key, String username, UserRole... roles) {
    GbifUser user = new GbifUser();
    user.setUserName(username);
    user.setPasswordHash(username);
    user.setLastLogin(new Date());
    user.setEmail(String.format(EMAIL_AT_GIBF, username));
    user.setFirstName(username);
    user.setLastName(username);
    user.setKey(key);
    if (roles != null) {
      user.setRoles(Sets.newHashSet(roles));
    }
    return user;
  }
}
