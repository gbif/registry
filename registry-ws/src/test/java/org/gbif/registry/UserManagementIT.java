package org.gbif.registry;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.registry.ConfirmationKeyParameter;
import org.gbif.api.service.common.IdentityService;
import org.gbif.identity.model.ModelMutationError;
import org.gbif.identity.model.UserModelMutationResult;
import org.gbif.identity.mybatis.IdentitySuretyTestHelper;
import org.gbif.identity.mybatis.UserMapper;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.ws.fixtures.TestClient;
import org.gbif.registry.ws.fixtures.TestConstants;
import org.gbif.registry.ws.fixtures.UserTestFixture;
import org.gbif.registry.ws.model.AuthenticationDataParameters;
import org.gbif.registry.ws.model.UserAdminView;
import org.gbif.ws.response.GbifResponseStatus;
import org.gbif.ws.security.GbifAuthService;

import java.util.UUID;
import java.util.function.Function;
import javax.ws.rs.core.Response;

import com.google.inject.Injector;
import com.sun.jersey.api.client.ClientResponse;
import org.junit.Test;

import static org.gbif.registry.ws.fixtures.UserTestFixture.ALTERNATE_USERNAME;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.eclipse.aether.repository.AuthenticationContext.PASSWORD;
import static org.junit.Assert.assertNull;

/**
 * Integration tests related to the User Manager resource.
 */
public class UserManagementIT extends PlainAPIBaseIT {

  private static final String CHANGED_PASSWORD = "123456";

  private static final String RESOURCE_PATH = "admin/user";
  private GbifAuthService gbifAuthService = GbifAuthService.singleKeyAuthService(
          TestConstants.IT_APP_KEY, TestConstants.IT_APP_SECRET);

  private TestClient testClient;
  private IdentityService identityService;
  private UserMapper userMapper;
  private IdentitySuretyTestHelper identitySuretyTestHelper;

  private UserTestFixture userTestFixture;

  public UserManagementIT() {
    final Injector injector = RegistryTestModules.identityMybatis();
    identityService = injector.getInstance(IdentityService.class);
    userMapper = injector.getInstance(UserMapper.class);
    identitySuretyTestHelper = injector.getInstance(IdentitySuretyTestHelper.class);

    testClient = new TestClient(wsBaseUrl);
    userTestFixture = new UserTestFixture(identityService, identitySuretyTestHelper);
  }

  @Test
  public void testCreateUser() {
    String newUserName = ALTERNATE_USERNAME;

    ClientResponse cr = postSignedRequest(TestConstants.IT_APP_KEY,
            UserTestFixture.generateUser(newUserName), Function.identity());
    assertEquals(Response.Status.CREATED.getStatusCode(), cr.getStatus());

    //test we can't login (challengeCode not confirmed)
    cr = testClient.login(newUserName, PASSWORD);
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), cr.getStatus());

    GbifUser newUser = userMapper.get(newUserName);
    UUID challengeCode = identitySuretyTestHelper.getChallengeCode(newUser.getKey());

    //generate a new request to confirm challengeCode
    ConfirmationKeyParameter params = new ConfirmationKeyParameter();
    params.setConfirmationKey(challengeCode);
    cr = postSignedRequest(newUserName, params, uri -> uri.path("confirm"));
    assertEquals(Response.Status.CREATED.getStatusCode(), cr.getStatus());

    cr = testClient.login(newUserName, PASSWORD);
    assertEquals(Response.Status.OK.getStatusCode(), cr.getStatus());
  }

  @Test
  public void testCreateUserNonWhiteListAppKey() {
    String newUserName = ALTERNATE_USERNAME;

    GbifAuthService gbifAuthServiceKey2 = GbifAuthService.singleKeyAuthService(
            TestConstants.IT_APP_KEY2, TestConstants.IT_APP_SECRET2);
    ClientResponse cr = postSignedRequest(gbifAuthServiceKey2, TestConstants.IT_APP_KEY2,
            UserTestFixture.generateUser(newUserName), Function.identity());
    //it will authenticate since the appKey is valid but it won't get the APP role
    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), cr.getStatus());
  }

  @Test
  public void testResetPassword() {
    GbifUser testUser = userTestFixture.prepareUser();
    GbifUser createdUser = userMapper.get(testUser.getUserName());

    //ensure there is no challengeCode
    UUID challengeCode = identitySuretyTestHelper.getChallengeCode(createdUser.getKey());
    assertNull("challengeCode shall be null" + challengeCode, challengeCode);

    ClientResponse cr = postSignedRequest(testUser.getUserName(),
            uri -> uri.path("resetPassword"));
    assertEquals(Response.Status.NO_CONTENT.getStatusCode(), cr.getStatus());

    challengeCode = identitySuretyTestHelper.getChallengeCode(createdUser.getKey());
    assertNotNull("challengeCode shall exist" + challengeCode);

    //we should still be able to login with username/password
    cr = testClient.login(getUsername(), getPassword());
    assertEquals(Response.Status.OK.getStatusCode(), cr.getStatus());
  }

  @Test
  public void testUpdatePassword() {
    GbifUser testUser = userTestFixture.prepareUser();

    GbifUser createdUser = userMapper.get(testUser.getUserName());
    AuthenticationDataParameters params = new AuthenticationDataParameters();
    params.setPassword(CHANGED_PASSWORD);
    params.setChallengeCode(UUID.randomUUID());
    ClientResponse cr =
            postSignedRequest(testUser.getUserName(), params,
                    uriBldr -> uriBldr.path("updatePassword"));
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), cr.getStatus());

    //ask to reset password
    cr = postSignedRequest(testUser.getUserName(),
            uri -> uri.path("resetPassword"));
    assertEquals(Response.Status.NO_CONTENT.getStatusCode(), cr.getStatus());

    UUID confirmationKey = identitySuretyTestHelper.getChallengeCode(createdUser.getKey());
    assertNotNull("challengeCode shall exist" + confirmationKey);

    //ensure we can check if the challengeCode is valid for the user
    cr = getWithSignedRequest(testUser.getUserName(),
            uri -> uri.path("confirmationKeyValid")
                    .queryParam("confirmationKey", confirmationKey));
    assertEquals(Response.Status.NO_CONTENT.getStatusCode(), cr.getStatus());

    //change password using that code
    params = new AuthenticationDataParameters();
    params.setPassword(CHANGED_PASSWORD);
    params.setChallengeCode(confirmationKey);

    cr = postSignedRequest(testUser.getUserName(), params,
            uri -> uri.path("updatePassword"));
    assertEquals(Response.Status.CREATED.getStatusCode(), cr.getStatus());

    //ensure we can login with the new password
    cr = testClient.login(testUser.getUserName(), CHANGED_PASSWORD);
    assertEquals(Response.Status.OK.getStatusCode(), cr.getStatus());
  }

  @Test
  public void getUserFromAdmin() {
    GbifUser testUser = userTestFixture.prepareUser();
    GbifUser createdUser = userMapper.get(testUser.getUserName());

    ClientResponse cr = getWithSignedRequest(TestConstants.IT_APP_KEY, uriBldr -> uriBldr.path(testUser.getUserName()));
    assertEquals(Response.Status.OK.getStatusCode(), cr.getStatus());

    assertEquals(createdUser.getKey(), cr.getEntity(UserAdminView.class).getUser().getKey());
  }

  @Test
  public void testUpdateUser() {
    GbifUser testUser = userTestFixture.prepareUser();
    final String newUserFirstName = "My new first name";

    ClientResponse cr = testClient.login(getUsername(), getPassword());
    assertEquals(Response.Status.OK.getStatusCode(), cr.getStatus());
   // LoggedUser responseData = cr.getEntity(LoggedUser.class);

    testUser.setFirstName(newUserFirstName);
    cr = putWithSignedRequest(TestConstants.IT_APP_KEY, testUser, uriBldr -> uriBldr.path(testUser.getUserName()));
    assertEquals(Response.Status.NO_CONTENT.getStatusCode(), cr.getStatus());

    //load user directly from the database
    GbifUser updatedUser = userMapper.get(testUser.getUserName());
    assertEquals(newUserFirstName, updatedUser.getFirstName());

    //create a new user
    GbifUser testUser2 = userTestFixture.prepareUser(UserTestFixture.generateUser(ALTERNATE_USERNAME));
    cr = testClient.login(ALTERNATE_USERNAME, PASSWORD);
    assertEquals(Response.Status.OK.getStatusCode(), cr.getStatus());
   // responseData = cr.getEntity(LoggedUser.class);

    //update user2 using email from user1
    testUser2.setEmail(testUser.getEmail());
    cr = putWithSignedRequest(TestConstants.IT_APP_KEY, testUser2, uriBldr -> uriBldr.path(ALTERNATE_USERNAME));
    assertEquals(GbifResponseStatus.UNPROCESSABLE_ENTITY.getStatus(), cr.getStatus());
    assertEquals(ModelMutationError.EMAIL_ALREADY_IN_USE, cr.getEntity(UserModelMutationResult.class).getError());

    testUser2.setEmail("12345@mail.com");
    cr = putWithSignedRequest(TestConstants.IT_APP_KEY, testUser2, uriBldr -> uriBldr.path(ALTERNATE_USERNAME));
    assertEquals(Response.Status.NO_CONTENT.getStatusCode(), cr.getStatus());
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
    return UserTestFixture.USERNAME;
  }

  @Override
  protected String getPassword() {
    return UserTestFixture.PASSWORD;
  }

  @Override
  protected void onSetup() {
    //no-op
  }

}
