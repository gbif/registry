package org.gbif.registry;

import org.gbif.api.model.common.User;
import org.gbif.api.model.common.UserCreation;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.service.common.UserSession;
import org.gbif.identity.model.UserCreationResult;
import org.gbif.identity.mybatis.UserMapper;
import org.gbif.registry.guice.RegistryTestModules;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * Integration tests related to the identity module.
 */
public class IdentityIT extends PlainAPIBaseIT {

  private static final String RESOURCE_PATH = "user";
  private static final String USERNAME = "user_12";
  private static final String PASSWORD = "password";

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
    String newUserName = "user_13";

    ClientResponse cr = postSignedRequest(newUserName, generateUser(newUserName), Function.identity());
    assertEquals(Response.Status.CREATED.getStatusCode(), cr.getStatus());

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
  }

  @Test
  public void testResetPassword() {
    User testUser = prepareUser();
    User createdUser = userMapper.get(testUser.getUserName());

    UUID challengeCode = userMapper.getChallengeCode(createdUser.getKey());
    assertNull("challengeCode shall be null" + challengeCode, challengeCode);

    ClientResponse cr = postSignedRequest(testUser.getUserName(),
            uri -> uri.path("resetPassword"));
    assertEquals(Response.Status.NO_CONTENT.getStatusCode(), cr.getStatus());

    challengeCode = userMapper.getChallengeCode(createdUser.getKey());
    assertNotNull("challengeCode shall exist" + challengeCode);
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
    UserCreation newTestUser = generateUser();
    UserCreationResult userCreated = identityService.create(newTestUser);
    assertFalse("Shall not contain error -> " + userCreated.getError(), userCreated.containsError());

    Integer key = identityService.get(newTestUser.getUserName()).getKey();
    UUID challengeCode = userMapper.getChallengeCode(key);
    assertTrue("Shall confirm challengeCode " + challengeCode,
            identityService.confirmChallengeCode(key, challengeCode));

    return UserCreation.toUser(newTestUser);
  }

  private static UserCreation generateUser() {
    return generateUser(USERNAME);
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
