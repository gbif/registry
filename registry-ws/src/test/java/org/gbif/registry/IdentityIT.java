package org.gbif.registry;

import org.gbif.api.model.common.User;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.service.common.UserSession;
import org.gbif.identity.model.ModelMutationError;
import org.gbif.identity.model.UserModelMutationResult;
import org.gbif.identity.mybatis.UserMapper;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.ws.model.UserCreation;
import org.gbif.registry.ws.security.UpdateRulesManager;
import org.gbif.ws.response.GbifResponseStatus;
import org.gbif.ws.security.GbifAuthService;

import java.util.UUID;
import java.util.function.Function;
import javax.ws.rs.core.Response;

import com.google.inject.Injector;
import com.sun.jersey.api.client.ClientResponse;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Integration tests related to the identity module.
 */
public class IdentityIT extends PlainAPIBaseIT {

  private static final String RESOURCE_PATH = "user";
  private static final String USERNAME = "user_12";
  private static final String PASSWORD = "password";

  private static final String ALTERNATE_USERNAME = "user_13";

  private UserMapper userMapper;
  private IdentityService identityService;

  private GbifAuthService gbifAuthService = GbifAuthService.singleKeyAuthService(
          TestConstants.IT_APP_KEY, TestConstants.IT_APP_SECRET);

  public IdentityIT() {
    final Injector service = RegistryTestModules.identityMybatis();
    identityService = service.getInstance(IdentityService.class);
    userMapper = service.getInstance(UserMapper.class);
  }

  @Override
  protected GbifAuthService getAuthService() {
    return gbifAuthService;
  }

  @Override
  protected String getResourcePath() {
    return RESOURCE_PATH;
  }

  @Override
  protected String getUsername() {
    return USERNAME;
  }

  @Override
  protected String getPassword() {
    return PASSWORD;
  }

  @Override
  protected void onSetup() {
    //no-op
  }

  @Test
  public void testLoginNoCredentials() {
    ClientResponse cr = getPublicClient().get(wr -> wr.path("login"));
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), cr.getStatus());
  }

  @Test
  public void testLogin() {
    User user = prepareUser();
    ClientResponse cr = getAuthenticatedClient().get(wr -> wr.path("login"));
    assertEquals(Response.Status.OK.getStatusCode(), cr.getStatus());

    //try to login using the email instead of the username
    cr = generateAuthenticatedClient(user.getEmail(), getPassword()).get(wr -> wr.path("login"));
    assertEquals(Response.Status.OK.getStatusCode(), cr.getStatus());
  }

  @Test
  public void testSession() {
    prepareUser();
    ClientResponse cr = getAuthenticatedClient().get(wr -> wr.path("login"));
    assertEquals(Response.Status.OK.getStatusCode(), cr.getStatus());
    UserSession userSession = cr.getEntity(UserSession.class);

    //use the session to issue a simple get on the user
    cr = getPublicClient().getWithSessionToken(userSession.getSession());
    assertEquals(Response.Status.OK.getStatusCode(), cr.getStatus());
    assertEquals(USERNAME, userSession.getUserName());

    //logout
    cr = getPublicClient().getWithSessionToken(userSession.getSession(), wr -> wr.path("logout"));
    assertEquals(Response.Status.NO_CONTENT.getStatusCode(), cr.getStatus());

    //try to use the token again (after logout)
    cr = getPublicClient().getWithSessionToken(userSession.getSession());
    //ideally UNAUTHORIZED would be returned
    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), cr.getStatus());
  }

  @Test
  public void testCreateUser() {
    String newUserName = ALTERNATE_USERNAME;

    ClientResponse cr = postSignedRequest(newUserName, generateUser(newUserName), Function.identity());
    assertEquals(Response.Status.CREATED.getStatusCode(), cr.getStatus());

    //test we can't login (challengeCode not confirmed)
    cr = generateAuthenticatedClient(newUserName, PASSWORD).get(wr -> wr.path("login"));
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), cr.getStatus());

    User newUser = userMapper.get(newUserName);
    UUID challengeCode = userMapper.getChallengeCode(newUser.getKey());

    //generate a new request to confirm challengeCode
    cr = postSignedRequest(newUserName,
            uri -> uri.path("confirm")
                    .queryParam("challengeCode", challengeCode));
    assertEquals(Response.Status.CREATED.getStatusCode(), cr.getStatus());

    cr = generateAuthenticatedClient(newUserName, PASSWORD).get(wr -> wr.path("login"));
    assertEquals(Response.Status.OK.getStatusCode(), cr.getStatus());
  }

  @Test
  public void testUpdateUser() {
    User testUser = prepareUser();
    final String newUserFirstName = "My new first name";

    ClientResponse cr = getAuthenticatedClient().get(wr -> wr.path("login"));
    assertEquals(Response.Status.OK.getStatusCode(), cr.getStatus());
    UserSession responseData = cr.getEntity(UserSession.class);

    testUser.setFirstName(newUserFirstName);
    cr = getPublicClient().putWithSessionToken(responseData.getSession(), testUser);
    assertEquals(Response.Status.NO_CONTENT.getStatusCode(), cr.getStatus());

    //load user directly from the database
    User updatedUser = userMapper.get(testUser.getUserName());
    assertEquals(newUserFirstName, updatedUser.getFirstName());

    //create a new user
    User testUser2 = prepareUser(generateUser(ALTERNATE_USERNAME));
    cr = generateAuthenticatedClient(ALTERNATE_USERNAME, PASSWORD).get(wr -> wr.path("login"));
    assertEquals(Response.Status.OK.getStatusCode(), cr.getStatus());
    responseData = cr.getEntity(UserSession.class);

    //update user2 using email from user1
    testUser2.setEmail(testUser.getEmail());
    cr = getPublicClient().putWithSessionToken(responseData.getSession(), testUser2);
    assertEquals(GbifResponseStatus.UNPROCESSABLE_ENTITY.getStatus(), cr.getStatus());
    assertEquals(ModelMutationError.EMAIL_ALREADY_IN_USE, cr.getEntity(UserModelMutationResult.class).getError());

    testUser2.setEmail("12345@mail.com");
    cr = getPublicClient().putWithSessionToken(responseData.getSession(), testUser2);
    assertEquals(Response.Status.NO_CONTENT.getStatusCode(), cr.getStatus());
  }

  @Test
  public void testResetPassword() {
    User testUser = prepareUser();
    User createdUser = userMapper.get(testUser.getUserName());

    //ensure there is no challengeCode
    UUID challengeCode = userMapper.getChallengeCode(createdUser.getKey());
    assertNull("challengeCode shall be null" + challengeCode, challengeCode);

    ClientResponse cr = postSignedRequest(testUser.getUserName(),
            uri -> uri.path("resetPassword"));
    assertEquals(Response.Status.NO_CONTENT.getStatusCode(), cr.getStatus());

    challengeCode = userMapper.getChallengeCode(createdUser.getKey());
    assertNotNull("challengeCode shall exist" + challengeCode);

    //we should still be able to login with username/password
    cr = getAuthenticatedClient().get(wr -> wr.path("login"));
    assertEquals(Response.Status.OK.getStatusCode(), cr.getStatus());
  }

  @Test
  public void testUpdatePassword() {
    User testUser = prepareUser();

    User createdUser = userMapper.get(testUser.getUserName());
    ClientResponse cr =
            postSignedRequest(testUser.getUserName(),
                    uriBldr -> uriBldr
                            .path("updatePassword")
                            .queryParam("password", "1234")
                            .queryParam("challengeCode", UUID.randomUUID().toString()));
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), cr.getStatus());

    //ask to reset password
    cr = postSignedRequest(testUser.getUserName(),
            uri -> uri.path("resetPassword"));
    assertEquals(Response.Status.NO_CONTENT.getStatusCode(), cr.getStatus());

    UUID challengeCode = userMapper.getChallengeCode(createdUser.getKey());
    assertNotNull("challengeCode shall exist" + challengeCode);

    //ensure we can check if the challengeCode is valid for the user
    cr = getWithSignedRequest(testUser.getUserName(),
            uri -> uri.path("challengeCodeValid")
                    .queryParam("challengeCode", challengeCode));
    assertEquals(Response.Status.NO_CONTENT.getStatusCode(), cr.getStatus());

    //change password using that code
    cr = postSignedRequest(testUser.getUserName(),
            uri -> uri
                    .path("updatePassword")
                    .queryParam("password", "1234")
                    .queryParam("challengeCode", challengeCode));
    assertEquals(Response.Status.CREATED.getStatusCode(), cr.getStatus());

    //ensure we can login with the new password
    cr = generateAuthenticatedClient(testUser.getUserName(), "1234").get(wr -> wr.path("login"));
    assertEquals(Response.Status.OK.getStatusCode(), cr.getStatus());
  }

  private User prepareUser() {
    return prepareUser(generateUser());
  }

  private User prepareUser(UserCreation newTestUser) {
    User userToCreate = UpdateRulesManager.applyCreate(newTestUser);
    UserModelMutationResult userCreated = identityService.create(userToCreate,
            newTestUser.getPassword());
    assertNoErrorAfterMutation(userCreated);

    Integer key = identityService.get(newTestUser.getUserName()).getKey();
    UUID challengeCode = userMapper.getChallengeCode(key);
    assertTrue("Shall confirm challengeCode " + challengeCode,
            identityService.confirmChallengeCode(key, challengeCode));

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

  private static void assertNoErrorAfterMutation(UserModelMutationResult userModelMutationResult) {
    if (userModelMutationResult.containsError()) {
      fail("Shall not contain error. Got " + userModelMutationResult.getError() + "," +
              userModelMutationResult.getConstraintViolation());
    }
  }

  /**
   * Generates a different user on each call.
   * Thread-Safe
   *
   * @return
   */
  private static UserCreation generateUser(String username) {
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

}
