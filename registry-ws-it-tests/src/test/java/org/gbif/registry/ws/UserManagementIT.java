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
package org.gbif.registry.ws;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.registry.ConfirmationKeyParameter;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.identity.mybatis.IdentitySuretyTestHelper;
import org.gbif.registry.domain.ws.AuthenticationDataParameters;
import org.gbif.registry.domain.ws.UserAdminView;
import org.gbif.registry.domain.ws.UserCreation;
import org.gbif.registry.identity.model.ModelMutationError;
import org.gbif.registry.identity.model.UserModelMutationResult;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.persistence.mapper.UserMapper;
import org.gbif.registry.ws.fixtures.TestClient;
import org.gbif.registry.ws.fixtures.TestConstants;
import org.gbif.registry.ws.fixtures.UserTestFixture;
import org.gbif.ws.security.JerseyGbifAuthService;

import java.util.UUID;
import java.util.function.Function;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import com.google.common.collect.ImmutableMap;
import com.sun.jersey.api.client.ClientResponse;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.gbif.registry.ws.fixtures.UserTestFixture.ALTERNATE_USERNAME;
import static org.gbif.registry.ws.util.AssertHttpResponse.assertResponse;
import static org.junit.Assert.assertNull;

/**
 * Integration tests related to the User Manager resource (the service itself is tested in the
 * registry-identity module). Due to the fact that all user management operations are not available
 * in the Java ws client, the tests use a direct HTTP client.
 */
public class UserManagementIT extends PlainAPIBaseIT {

  private static final String CHANGED_PASSWORD = "123456";
  private static final String RESET_PASSWORD_PATH = "resetPassword";
  private static final String UPDATE_PASSWORD_PATH = "updatePassword";
  private static final String WRONG_PASSWORD = "WRONGPassword";

  private static final String RESOURCE_PATH = "admin/user";
  private JerseyGbifAuthService gbifAuthService;

  private TestClient testClient;
  private UserMapper userMapper;
  private IdentitySuretyTestHelper identitySuretyTestHelper;

  private UserTestFixture userTestFixture;

  @Autowired
  public UserManagementIT(
      IdentityService identityService,
      UserMapper userMapper,
      IdentitySuretyTestHelper identitySuretyTestHelper,
      JerseyGbifAuthService gbifAuthService) {
    this.userMapper = userMapper;
    this.identitySuretyTestHelper = identitySuretyTestHelper;
    this.gbifAuthService = gbifAuthService;

    testClient = new TestClient(wsBaseUrl);
    userTestFixture = new UserTestFixture(identityService, identitySuretyTestHelper);
  }

  @Test
  public void testCreateUser() {
    String newUserName = ALTERNATE_USERNAME;

    ClientResponse cr =
        postSignedRequest(
            TestConstants.IT_APP_KEY,
            UserTestFixture.generateUser(newUserName),
            Function.identity());
    assertResponse(Response.Status.CREATED, cr);

    // test we can't login (challengeCode not confirmed)
    cr = testClient.login(newUserName, WRONG_PASSWORD);
    assertResponse(Response.Status.UNAUTHORIZED, cr);

    GbifUser newUser = userMapper.get(newUserName);
    UUID challengeCode = identitySuretyTestHelper.getChallengeCode(newUser.getKey());

    // generate a new request to confirm challengeCode
    ConfirmationKeyParameter params = new ConfirmationKeyParameter();
    params.setConfirmationKey(challengeCode);
    cr = postSignedRequest(newUserName, params, uri -> uri.path("confirm"));
    assertResponse(Response.Status.CREATED, cr);

    cr = testClient.login(newUserName, "password");
    assertResponse(Response.Status.OK, cr);
  }

  @Test
  public void testCreateUserNonWhiteListAppKey() {
    String newUserName = ALTERNATE_USERNAME;

    // TODO
    /*   GbifAuthServiceImpl authService = new GbifAuthServiceImpl(new AppKeySigningService(KeyStore))
    GbifAuthService gbifAuthServiceKey2 = GbifAuthService.singleKeyAuthService(
            TestConstants.IT_APP_KEY2, TestConstants.IT_APP_SECRET2);*/
    ClientResponse cr =
        postSignedRequest(
            gbifAuthService,
            TestConstants.IT_APP_KEY2,
            UserTestFixture.generateUser(newUserName),
            Function.identity());
    // it will authenticate since the appKey is valid but it won't get the APP role
    assertResponse(Response.Status.FORBIDDEN, cr);
  }

  @Test
  public void testResetPassword() {
    GbifUser testUser = userTestFixture.prepareUser();
    GbifUser createdUser = userMapper.get(testUser.getUserName());

    // ensure there is no challengeCode
    UUID challengeCode = identitySuretyTestHelper.getChallengeCode(createdUser.getKey());
    assertNull("challengeCode shall be null" + challengeCode, challengeCode);

    ClientResponse cr =
        postSignedRequest(testUser.getUserName(), uri -> uri.path(RESET_PASSWORD_PATH));
    assertResponse(Response.Status.NO_CONTENT, cr);

    challengeCode = identitySuretyTestHelper.getChallengeCode(createdUser.getKey());
    assertNotNull("challengeCode shall exist" + challengeCode);

    // we should still be able to login with username/password
    cr = testClient.login(getUsername(), getPassword());
    assertResponse(Response.Status.OK, cr);
  }

  @Test
  public void testUpdatePassword() {
    GbifUser testUser = userTestFixture.prepareUser();

    GbifUser createdUser = userMapper.get(testUser.getUserName());
    AuthenticationDataParameters params = new AuthenticationDataParameters();
    params.setPassword(CHANGED_PASSWORD);
    params.setChallengeCode(UUID.randomUUID());
    ClientResponse cr =
        postSignedRequest(
            testUser.getUserName(), params, uriBldr -> uriBldr.path(UPDATE_PASSWORD_PATH));
    assertResponse(Response.Status.UNAUTHORIZED, cr);

    // ask to reset password
    cr = postSignedRequest(testUser.getUserName(), uri -> uri.path(RESET_PASSWORD_PATH));
    assertResponse(Response.Status.NO_CONTENT, cr);

    UUID confirmationKey = identitySuretyTestHelper.getChallengeCode(createdUser.getKey());
    assertNotNull("challengeCode shall exist" + confirmationKey);

    // ensure we can check if the challengeCode is valid for the user
    cr =
        getWithSignedRequest(
            testUser.getUserName(),
            uri -> uri.path("confirmationKeyValid").queryParam("confirmationKey", confirmationKey));
    assertResponse(Response.Status.NO_CONTENT, cr);

    // change password using that code
    params = new AuthenticationDataParameters();
    params.setPassword(CHANGED_PASSWORD);
    params.setChallengeCode(confirmationKey);

    cr = postSignedRequest(testUser.getUserName(), params, uri -> uri.path(UPDATE_PASSWORD_PATH));
    assertResponse(Response.Status.CREATED, cr);

    // ensure we can login with the new password
    cr = testClient.login(testUser.getUserName(), CHANGED_PASSWORD);
    assertResponse(Response.Status.OK, cr);
  }

  @Test
  public void getUserFromAdmin() {
    GbifUser testUser = userTestFixture.prepareUser();
    GbifUser createdUser = userMapper.get(testUser.getUserName());

    ClientResponse cr =
        getWithSignedRequest(
            TestConstants.IT_APP_KEY, uriBldr -> uriBldr.path(testUser.getUserName()));
    assertResponse(Response.Status.OK, cr);

    assertEquals(createdUser.getKey(), cr.getEntity(UserAdminView.class).getUser().getKey());
  }

  @Test
  public void getUserBySystemSettings() {
    GbifUser testUser = userTestFixture.prepareUser();
    GbifUser createdUser = userMapper.get(testUser.getUserName());
    createdUser.setSystemSettings(ImmutableMap.of("my.settings.key", "100_tacos=100$"));
    userMapper.update(createdUser);

    ClientResponse cr =
        getWithSignedRequest(
            TestConstants.IT_APP_KEY,
            uriBldr -> uriBldr.path("find"),
            ImmutableMap.of("my.settings.key", "100_tacos=100$"));
    assertResponse(Response.Status.OK, cr);
    assertEquals(createdUser.getKey(), cr.getEntity(UserAdminView.class).getUser().getKey());
  }

  @Test
  public void testUpdateUser() {
    GbifUser testUser = userTestFixture.prepareUser();
    final String newUserFirstName = "My new first name";

    ClientResponse cr = testClient.login(getUsername(), getPassword());
    assertResponse(Response.Status.OK, cr);

    testUser.setFirstName(newUserFirstName);
    cr =
        putWithSignedRequest(
            TestConstants.IT_APP_KEY, testUser, uriBldr -> uriBldr.path(testUser.getUserName()));
    assertResponse(Response.Status.NO_CONTENT, cr);

    // load user directly from the database
    GbifUser updatedUser = userMapper.get(testUser.getUserName());
    assertEquals(newUserFirstName, updatedUser.getFirstName());

    // create a new user
    GbifUser testUser2 =
        userTestFixture.prepareUser(UserTestFixture.generateUser(ALTERNATE_USERNAME));
    cr = testClient.login(ALTERNATE_USERNAME, "password");
    assertResponse(Response.Status.OK, cr);

    // update user2 using email from user1
    testUser2.setEmail(testUser.getEmail());
    cr =
        putWithSignedRequest(
            TestConstants.IT_APP_KEY, testUser2, uriBldr -> uriBldr.path(ALTERNATE_USERNAME));

    // TODO: remove GbifResponseStatus
    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, cr.getStatus());
    assertEquals(
        ModelMutationError.EMAIL_ALREADY_IN_USE,
        cr.getEntity(UserModelMutationResult.class).getError());

    testUser2.setEmail("12345@mail.com");
    cr =
        putWithSignedRequest(
            TestConstants.IT_APP_KEY, testUser2, uriBldr -> uriBldr.path(ALTERNATE_USERNAME));
    assertResponse(Response.Status.NO_CONTENT, cr);
  }

  @Test
  public void testUserEditorRights() {
    /* Create a first admin user; this can't be done through the API. */
    UserCreation adminUserCreation = UserTestFixture.generateUser(TestConstants.TEST_ADMIN);
    GbifUser adminUser = userTestFixture.prepareUser(adminUserCreation);
    adminUser.addRole(UserRole.REGISTRY_ADMIN);
    userMapper.update(adminUser);

    GbifUser testUser = userTestFixture.prepareUser();
    UUID key = UUID.randomUUID();

    // Admin add right
    ClientResponse cr =
        postSignedRequest(
            TestConstants.TEST_ADMIN,
            key,
            uriBldr -> uriBldr.path(testUser.getUserName() + "/editorRight"));
    assertResponse(Response.Status.CREATED, cr);

    // Admin see rights
    cr =
        getWithSignedRequest(
            TestConstants.TEST_ADMIN,
            uriBldr -> uriBldr.path(testUser.getUserName() + "/editorRight"));
    assertResponse(Response.Status.OK, cr);

    // See own rights
    cr =
        getWithSignedRequest(
            testUser.getUserName(),
            uriBldr -> uriBldr.path(testUser.getUserName() + "/editorRight"));
    assertResponse(Response.Status.OK, cr);

    // Admin delete right
    cr =
        deleteWithSignedRequest(
            TestConstants.TEST_ADMIN,
            uriBldr -> uriBldr.path(testUser.getUserName() + "/editorRight/" + key));
    assertResponse(Response.Status.NO_CONTENT, cr);
  }

  @Test
  public void testUserEditorRightsErrors() {
    /* Create a first admin user; this can't be done through the API. */
    UserCreation adminUserCreation = UserTestFixture.generateUser(TestConstants.TEST_ADMIN);
    GbifUser adminUser = userTestFixture.prepareUser(adminUserCreation);
    adminUser.addRole(UserRole.REGISTRY_ADMIN);
    userMapper.update(adminUser);

    GbifUser testUser = userTestFixture.prepareUser();
    UUID key = UUID.randomUUID();

    // User doesn't exist
    ClientResponse cr =
        postSignedRequest(
            TestConstants.TEST_ADMIN,
            key,
            uriBldr -> uriBldr.path("someOtherUser" + "/editorRight"));
    assertResponse(Response.Status.NOT_FOUND, cr);

    // Not an admin user
    cr =
        postSignedRequest(
            testUser.getUserName(),
            key,
            uriBldr -> uriBldr.path(testUser.getUserName() + "/editorRight"));
    assertResponse(Response.Status.FORBIDDEN, cr);
    System.err.println(cr);

    // Right already exists
    cr =
        postSignedRequest(
            TestConstants.TEST_ADMIN,
            key,
            uriBldr -> uriBldr.path(testUser.getUserName() + "/editorRight"));
    assertResponse(Response.Status.CREATED, cr);
    cr =
        postSignedRequest(
            TestConstants.TEST_ADMIN,
            key,
            uriBldr -> uriBldr.path(testUser.getUserName() + "/editorRight"));
    assertResponse(Response.Status.CONFLICT, cr);

    // Right doesn't exist
    cr =
        deleteWithSignedRequest(
            TestConstants.TEST_ADMIN,
            uriBldr -> uriBldr.path(testUser.getUserName() + "/editorRight/" + UUID.randomUUID()));
    assertResponse(Response.Status.NOT_FOUND, cr);
    System.err.println(cr);
  }

  @Override
  protected JerseyGbifAuthService getAuthService() {
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
    // no-op
  }
}
