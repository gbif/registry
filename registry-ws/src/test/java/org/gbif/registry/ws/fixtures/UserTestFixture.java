package org.gbif.registry.ws.fixtures;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityService;
import org.gbif.identity.model.UserModelMutationResult;
import org.gbif.identity.mybatis.IdentitySuretyTestHelper;
import org.gbif.registry.ws.model.UserCreation;
import org.gbif.registry.ws.security.UserUpdateRulesManager;

import java.util.UUID;
import javax.ws.rs.core.MultivaluedMap;

import com.sun.jersey.core.util.MultivaluedMapImpl;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.fail;

/**
 * Fixtures related to users used for testing.
 */
public class UserTestFixture {

  public static final String IDENTITY_RESOURCE_PATH = "identity";
  public static final String IDENTITY_ADMIN_RESOURCE_PATH = "admin/identity";
  public static final String USER_RESOURCE_PATH = "user";

  public static final String RESET_PASSWORD_PATH = "resetPassword";
  public static final String UPDATE_PASSWORD_PATH = "updatePassword";

  public static final String USERNAME = "user_12";
  public static final String ALTERNATE_USERNAME = "user_13";
  public static final String PASSWORD = "password";

  private IdentityService identityService;
  private IdentitySuretyTestHelper identitySuretyTestHelper;

  public UserTestFixture(IdentityService identityService, IdentitySuretyTestHelper identitySuretyTestHelper) {
    this.identityService = identityService;
    this.identitySuretyTestHelper = identitySuretyTestHelper;
  }

  /**
   * Prepare a pre-defined user {@link #USERNAME}
   * @return
   */
  public GbifUser prepareUser() {
    return prepareUser(generateUser());
  }

  /**
   * Utility method to prepare a user in the database.
   * @param newTestUser
   * @return
   */
  public GbifUser prepareUser(UserCreation newTestUser) {
    GbifUser userToCreate = UserUpdateRulesManager.applyCreate(newTestUser);
    UserModelMutationResult userCreated = identityService.create(userToCreate,
            newTestUser.getPassword());
    assertNoErrorAfterMutation(userCreated);

    Integer key = identityService.get(newTestUser.getUserName()).getKey();
    UUID challengeCode = identitySuretyTestHelper.getChallengeCode(key);
    assertTrue("Shall confirm challengeCode " + challengeCode,
            identityService.confirmUser(key, challengeCode));

    //this is currently done in the web layer (UserResource) since we confirm the challengeCode
    //directly using the service we update it here
    identityService.updateLastLogin(key);
    return userToCreate;
  }

  /**
   * Generates a test user with username {@link #USERNAME}
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

  public static MultivaluedMap<String, String> buildQueryParams(String key, String value){
    MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    queryParams.add(key, value);
    return queryParams;
  }

  public static MultivaluedMap<String, String> buildQueryParams(String key1, String value1, String key2, String value2){
    MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    queryParams.add(key1, value1);
    queryParams.add(key2, value2);
    return queryParams;
  }
}
