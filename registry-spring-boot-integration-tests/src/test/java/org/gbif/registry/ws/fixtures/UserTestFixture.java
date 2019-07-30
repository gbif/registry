package org.gbif.registry.ws.fixtures;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityService;
import org.gbif.registry.identity.model.UserModelMutationResult;
import org.gbif.registry.persistence.ChallengeCodeSupportMapper;
import org.gbif.registry.persistence.mapper.ChallengeCodeMapper;
import org.gbif.registry.ws.model.UserCreation;
import org.gbif.registry.ws.security.UserUpdateRulesManager;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.UUID;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.fail;

// TODO: 2019-07-03 remove?
/**
 * Fixtures related to users used for testing.
 */
public class UserTestFixture {

  public static final String USER_RESOURCE_PATH = "user";

  public static final String USERNAME = "user_12";
  public static final String ALTERNATE_USERNAME = "user_13";
  public static final String PASSWORD = "welcome";

  private IdentityService identityService;
  private ChallengeCodeMapper challengeCodeMapper;
  private ChallengeCodeSupportMapper<Integer> challengeCodeSupportMapper;

  public UserTestFixture(IdentityService identityService,
                         ChallengeCodeMapper challengeCodeMapper,
                         ChallengeCodeSupportMapper<Integer> challengeCodeSupportMapper) {
    this.identityService = identityService;
    this.challengeCodeMapper = challengeCodeMapper;
    this.challengeCodeSupportMapper = challengeCodeSupportMapper;
  }

  /**
   * Prepare a pre-defined user {@link #USERNAME}
   *
   * @return
   */
  public GbifUser prepareUser() {
    return prepareUser(generateUser());
  }

  /**
   * Utility method to prepare a user in the database.
   *
   * @param newTestUser
   * @return
   */
  public GbifUser prepareUser(UserCreation newTestUser) {
    GbifUser userToCreate = UserUpdateRulesManager.applyCreate(newTestUser);
    UserModelMutationResult userCreated = identityService.create(userToCreate,
        newTestUser.getPassword());
    assertNoErrorAfterMutation(userCreated);

    Integer key = identityService.get(newTestUser.getUserName()).getKey();
    UUID challengeCode = challengeCodeMapper.getChallengeCode(challengeCodeSupportMapper.getChallengeCodeKey(key));
    assertTrue("Shall confirm challengeCode " + challengeCode,
        identityService.confirmUser(key, challengeCode));

    //this is currently done in the web layer (UserResource) since we confirm the challengeCode
    //directly using the service we update it here
    identityService.updateLastLogin(key);
    return userToCreate;
  }

  /**
   * Generates a test user with username {@link #USERNAME}
   *
   * @return
   */
  private static UserCreation generateUser() {
    return generateUser(USERNAME);
  }

  /**
   * Generates a different user on each call.
   * Thread-Safe
   *
   * @return
   */
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
      fail("Shall not contain error. Got " + userModelMutationResult.getError() + "," +
          userModelMutationResult.getConstraintViolation());
    }
  }

  public static MultiValueMap<String, String> buildQueryParams(String key, String value) {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add(key, value);
    return queryParams;
  }

  public static MultiValueMap<String, String> buildQueryParams(String key1, String value1, String key2, String value2) {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add(key1, value1);
    queryParams.add(key2, value2);
    return queryParams;
  }
}
