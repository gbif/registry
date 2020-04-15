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
package org.gbif.registry.ws.fixtures;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.domain.ws.UserCreation;
import org.gbif.registry.identity.model.UserModelMutationResult;
import org.gbif.registry.identity.mybatis.IdentitySuretyTestHelper;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.persistence.mapper.UserMapper;
import org.gbif.registry.security.UserUpdateRulesManager;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;

import static org.gbif.registry.ws.fixtures.TestConstants.TEST_ADMIN;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Fixtures related to users used for testing. */
@Component
public class UserTestFixture {

  public static final String USERNAME = "test_user";
  public static final String EMAIL = "test_user@gbif.org";
  public static final String ALTERNATE_USERNAME = "alternative_test_user";
  public static final String PASSWORD = "password";

  private IdentityService identityService;
  private IdentitySuretyTestHelper identitySuretyTestHelper;
  private UserMapper userMapper;

  @Autowired
  public UserTestFixture(
      IdentityService identityService,
      IdentitySuretyTestHelper identitySuretyTestHelper,
      UserMapper userMapper) {
    this.identityService = identityService;
    this.identitySuretyTestHelper = identitySuretyTestHelper;
    this.userMapper = userMapper;
  }

  /** Prepare a pre-defined admin user {@link TestConstants#TEST_ADMIN} */
  public GbifUser prepareAdminUser() {
    UserCreation adminUserCreation = UserTestFixture.generateUser(TEST_ADMIN);
    GbifUser adminUser = prepareUser(adminUserCreation);
    adminUser.addRole(UserRole.REGISTRY_ADMIN);
    userMapper.update(adminUser);

    return adminUser;
  }

  /** Get user by username */
  public GbifUser getUser(String username) {
    return userMapper.get(username);
  }

  /** Get user's challenge code by username */
  public UUID getUserChallengeCode(String username) {
    GbifUser user = userMapper.get(username);
    return identitySuretyTestHelper.getChallengeCode(user.getKey());
  }

  /** Add system setting to the user with provided username */
  public GbifUser addSystemSettingsToUser(String username, Map<String, String> settings) {
    GbifUser createdUser = userMapper.get(username);
    Map<String, String> params = ImmutableMap.of("my.settings.key", "100_tacos=100$");
    createdUser.setSystemSettings(settings);
    userMapper.update(createdUser);
    return createdUser;
  }

  /** Prepare a pre-defined user {@link #USERNAME} */
  public GbifUser prepareUser() {
    return prepareUser(generateUser());
  }

  /** Utility method to prepare a user in the database. */
  public GbifUser prepareUser(UserCreation newTestUser) {
    GbifUser userToCreate = UserUpdateRulesManager.applyCreate(newTestUser);
    UserModelMutationResult userCreated =
        identityService.create(userToCreate, newTestUser.getPassword());
    assertNoErrorAfterMutation(userCreated);

    Integer key = identityService.get(newTestUser.getUserName()).getKey();
    UUID challengeCode = identitySuretyTestHelper.getChallengeCode(key);
    assertTrue(
        identityService.confirmUser(key, challengeCode),
        "Shall confirm challengeCode " + challengeCode);

    // this is currently done in the web layer (UserResource) since we confirm the challengeCode
    // directly using the service we update it here
    identityService.updateLastLogin(key);
    return userToCreate;
  }

  /** Generates a test user with username {@link #USERNAME} */
  private static UserCreation generateUser() {
    return generateUser(USERNAME);
  }

  /** Generates a different user on each call. Thread-Safe */
  public static UserCreation generateUser(String username) {
    UserCreation user = new UserCreation();
    user.setUserName(username);
    user.setFirstName("Tim");
    user.setLastName("Robertson");
    user.setPassword(PASSWORD);
    user.getSettings().put("language", "en");
    user.getSettings().put("country", "dk");
    user.setEmail(username + "@gbif.org");
    return user;
  }

  private static void assertNoErrorAfterMutation(UserModelMutationResult userModelMutationResult) {
    if (userModelMutationResult.containsError()) {
      fail(
          "Shall not contain error. Got "
              + userModelMutationResult.getError()
              + ","
              + userModelMutationResult.getConstraintViolation());
    }
  }
}
