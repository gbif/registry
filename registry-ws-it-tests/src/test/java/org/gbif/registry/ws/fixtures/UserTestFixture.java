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
import org.gbif.registry.domain.ws.UserCreation;
import org.gbif.registry.identity.model.UserModelMutationResult;
import org.gbif.registry.identity.mybatis.IdentitySuretyTestHelper;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.security.UserUpdateRulesManager;

import java.util.UUID;

import javax.ws.rs.core.MultivaluedMap;

import com.sun.jersey.core.util.MultivaluedMapImpl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Fixtures related to users used for testing. */
public class UserTestFixture {

  public static final String USER_RESOURCE_PATH = "user";

  public static final String USERNAME = "user_12";
  public static final String ALTERNATE_USERNAME = "user_13";
  public static final String APP_KEY = "gbif.app.it";
  public static final String PASSWORD = "password";

  private IdentityService identityService;
  private IdentitySuretyTestHelper identitySuretyTestHelper;

  public UserTestFixture(
      IdentityService identityService, IdentitySuretyTestHelper identitySuretyTestHelper) {
    this.identityService = identityService;
    this.identitySuretyTestHelper = identitySuretyTestHelper;
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
        "Shall confirm challengeCode " + challengeCode,
        identityService.confirmUser(key, challengeCode));

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

  public static MultivaluedMap<String, String> buildQueryParams(String key, String value) {
    MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    queryParams.add(key, value);
    return queryParams;
  }

  public static MultivaluedMap<String, String> buildQueryParams(
      String key1, String value1, String key2, String value2) {
    MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    queryParams.add(key1, value1);
    queryParams.add(key2, value2);
    return queryParams;
  }
}
