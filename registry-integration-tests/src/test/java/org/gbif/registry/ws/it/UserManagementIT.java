/*
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
package org.gbif.registry.ws.it;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.ConfirmationKeyParameter;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.domain.ws.AuthenticationDataParameters;
import org.gbif.registry.domain.ws.EmailChangeRequest;
import org.gbif.registry.domain.ws.UserAdminView;
import org.gbif.registry.domain.ws.UserCreation;
import org.gbif.registry.identity.model.ModelMutationError;
import org.gbif.registry.identity.model.UserModelMutationResult;
import org.gbif.registry.identity.mybatis.IdentitySuretyTestHelper;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.security.UserRoles;
import org.gbif.registry.ws.it.fixtures.RequestTestFixture;
import org.gbif.registry.ws.it.fixtures.UserTestFixture;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;

import static org.gbif.registry.ws.it.fixtures.TestConstants.IT_APP_KEY;
import static org.gbif.registry.ws.it.fixtures.TestConstants.IT_APP_KEY2;
import static org.gbif.registry.ws.it.fixtures.TestConstants.TEST_ADMIN;
import static org.gbif.registry.ws.it.fixtures.UserTestFixture.ALTERNATE_USERNAME;
import static org.gbif.registry.ws.it.fixtures.UserTestFixture.ALTERNATIVE_EMAIL;
import static org.gbif.registry.ws.it.fixtures.UserTestFixture.PASSWORD;
import static org.gbif.registry.ws.it.fixtures.UserTestFixture.USERNAME;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests related to the User Manager resource (the service itself is tested in the
 * registry-identity module). Due to the fact that all user management operations are not available
 * in the Java ws client, the tests use a direct HTTP client.
 */
public class UserManagementIT extends BaseItTest {

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule =
      new TestCaseDatabaseInitializer(
          "public.user", "editor_rights", "country_rights", "namespace_rights");

  private static final String CHANGED_PASSWORD = "123456";

  private final IdentitySuretyTestHelper identitySuretyTestHelper;
  private final UserTestFixture userTestFixture;
  private final RequestTestFixture requestTestFixture;

  @Autowired
  public UserManagementIT(
      UserTestFixture userTestFixture,
      RequestTestFixture requestTestFixture,
      IdentitySuretyTestHelper identitySuretyTestHelper,
      SimplePrincipalProvider simplePrincipalProvider,
      EsManageServer esServer) {
    super(simplePrincipalProvider, esServer);
    this.identitySuretyTestHelper = identitySuretyTestHelper;
    this.userTestFixture = userTestFixture;
    this.requestTestFixture = requestTestFixture;
  }

  @Test
  public void testCreateUser() throws Exception {
    // create a new user
    UserCreation userCreation = UserTestFixture.generateUser(ALTERNATE_USERNAME);

    requestTestFixture
        .postSignedRequest(IT_APP_KEY, userCreation, "/admin/user")
        .andExpect(status().isCreated());

    // test we can't log in (challengeCode not confirmed)
    requestTestFixture
        .getRequest(ALTERNATE_USERNAME, PASSWORD, "/user/login")
        .andExpect(status().isUnauthorized());

    // generate a new request to confirm challengeCode
    ConfirmationKeyParameter params = new ConfirmationKeyParameter();
    params.setConfirmationKey(userTestFixture.getUserChallengeCode(ALTERNATE_USERNAME));

    requestTestFixture
        .postSignedRequest(ALTERNATE_USERNAME, params, "/admin/user/confirm")
        .andExpect(status().isCreated());

    // test we can log in afterwards
    requestTestFixture
        .getRequest(ALTERNATE_USERNAME, PASSWORD, "/user/login")
        .andExpect(status().isOk());

    // Test search by rights on entity
    userTestFixture.prepareAdminUser();
    ResultActions result =
        requestTestFixture
            .getSignedRequest(
                TEST_ADMIN,
                "/admin/user/search",
                ImmutableMap.<String, String>builder()
                    .put("role", UserRole.USER.name())
                    .put("1", TEST_ADMIN)
                    .build())
            .andExpect(status().isOk());
    PagingResponse<UserAdminView> adminUsers =
        requestTestFixture.extractJsonResponse(
            result, new TypeReference<PagingResponse<UserAdminView>>() {});
    assertEquals(
        1L,
        adminUsers.getResults().stream()
            .filter(u -> u.getUser().getUserName().equals(ALTERNATE_USERNAME))
            .count());
  }

  @Test
  public void testCreateUserNonWhiteListAppKey() throws Exception {
    // create a new user using an app key which is not present in the white list
    UserCreation userCreation = UserTestFixture.generateUser(ALTERNATE_USERNAME);

    // it will authenticate since the appKey is valid, but it won't get the APP role
    requestTestFixture
        .postSignedRequest(IT_APP_KEY2, IT_APP_KEY2, userCreation, "/admin/user")
        .andExpect(status().isForbidden());
  }

  @Test
  public void testResetPassword() throws Exception {
    GbifUser testUser = userTestFixture.prepareUser();
    GbifUser createdUser = userTestFixture.getUser(testUser.getUserName());

    // ensure there is no challengeCode
    UUID challengeCode = identitySuretyTestHelper.getChallengeCode(createdUser.getKey());
    assertNull(challengeCode, "challengeCode shall be null");

    // reset password
    requestTestFixture
        .postSignedRequest(USERNAME, "/admin/user/resetPassword")
        .andExpect(status().isNoContent());

    challengeCode = identitySuretyTestHelper.getChallengeCode(createdUser.getKey());
    assertNotNull(challengeCode, "challengeCode shall exist");

    // we should still be able to login with username/password
    requestTestFixture.getRequest(USERNAME, PASSWORD, "/user/login").andExpect(status().isOk());
  }

  @Test
  public void testUpdatePassword() throws Exception {
    GbifUser testUser = userTestFixture.prepareUser();

    GbifUser createdUser = userTestFixture.getUser(testUser.getUserName());
    AuthenticationDataParameters params = new AuthenticationDataParameters();
    params.setPassword(CHANGED_PASSWORD);
    params.setChallengeCode(UUID.randomUUID());

    requestTestFixture
        .postSignedRequest(USERNAME, params, "/admin/user/updatePassword")
        .andExpect(status().isUnauthorized());

    // ask to reset password
    requestTestFixture
        .postSignedRequest(USERNAME, "/admin/user/resetPassword")
        .andExpect(status().isNoContent());

    UUID confirmationKey = identitySuretyTestHelper.getChallengeCode(createdUser.getKey());
    assertNotNull(confirmationKey, "challengeCode shall exist");

    // ensure we can check if the challengeCode is valid for the user
    Map<String, String> queryParams =
        ImmutableMap.of("confirmationKey", confirmationKey.toString());
    requestTestFixture
        .getSignedRequest(USERNAME, "/admin/user/confirmationKeyValid", queryParams)
        .andExpect(status().isNoContent());

    // change password using that code
    params = new AuthenticationDataParameters();
    params.setPassword(CHANGED_PASSWORD);
    params.setChallengeCode(confirmationKey);

    requestTestFixture
        .postSignedRequest(USERNAME, params, "/admin/user/updatePassword")
        .andExpect(status().isCreated());

    // ensure we can log in with the new password
    requestTestFixture
        .getRequest(USERNAME, CHANGED_PASSWORD, "/user/login")
        .andExpect(status().isOk());
  }

  @Test
  public void getUserFromAdmin() throws Exception {
    GbifUser testUser = userTestFixture.prepareUser();
    GbifUser createdUser = userTestFixture.getUser(testUser.getUserName());

    ResultActions actions =
        requestTestFixture
            .getSignedRequest(IT_APP_KEY, "/admin/user/" + USERNAME)
            .andExpect(status().isOk());

    UserAdminView actualUserAdminView =
        requestTestFixture.extractJsonResponse(actions, UserAdminView.class);

    assertEquals(createdUser.getKey(), actualUserAdminView.getUser().getKey());

    // Test search user role
    userTestFixture.prepareAdminUser();
    ResultActions result =
        requestTestFixture
            .getSignedRequest(
                TEST_ADMIN,
                "/admin/user/search",
                ImmutableMap.<String, String>builder().put("role", UserRole.USER.name()).build())
            .andExpect(status().isOk());
    PagingResponse<UserAdminView> adminUsers =
        requestTestFixture.extractJsonResponse(
            result, new TypeReference<PagingResponse<UserAdminView>>() {});
    assertEquals(
        1L,
        adminUsers.getResults().stream()
            .filter(u -> u.getUser().getUserName().equals(createdUser.getUserName()))
            .count());
  }

  @Test
  public void getUserBySystemSettings() throws Exception {
    userTestFixture.prepareUser();
    Map<String, String> params = ImmutableMap.of("my.settings.key", "100_tacos=100$");
    GbifUser createdUser = userTestFixture.addSystemSettingsToUser(USERNAME, params);

    ResultActions actions =
        requestTestFixture
            .getSignedRequest(IT_APP_KEY, "/admin/user/find", params)
            .andExpect(status().isOk());

    UserAdminView actualUserAdminView =
        requestTestFixture.extractJsonResponse(actions, UserAdminView.class);

    assertEquals(createdUser.getKey(), actualUserAdminView.getUser().getKey());
  }

  @Test
  public void getUserByWrongSystemSettings() throws Exception {
    userTestFixture.prepareUser();
    Map<String, String> params = ImmutableMap.of("my.settings.key", "100_tacos=100$");

    // user does not have these system settings
    requestTestFixture
        .getSignedRequest(IT_APP_KEY, "/admin/user/find", params)
        .andExpect(status().isNoContent());
  }

  @Test
  public void testUpdateUser() throws Exception {
    GbifUser testUser = userTestFixture.prepareUser();
    final String newUserFirstName = "My new first name";
    final Map<String, String> newSystemSettings = ImmutableMap.of("some.setting", "value");

    requestTestFixture.getRequest(USERNAME, PASSWORD, "/user/login").andExpect(status().isOk());

    testUser.setFirstName(newUserFirstName);
    testUser.setSystemSettings(newSystemSettings);

    requestTestFixture
        .putSignedRequest(IT_APP_KEY, testUser, "/admin/user/" + USERNAME)
        .andExpect(status().isNoContent());

    // load user directly from the database
    GbifUser updatedUser = userTestFixture.getUser(testUser.getUserName());
    assertEquals(newUserFirstName, updatedUser.getFirstName());
    assertEquals(newSystemSettings, updatedUser.getSystemSettings());

    // create a new user
    GbifUser testUser2 =
        userTestFixture.prepareUser(UserTestFixture.generateUser(ALTERNATE_USERNAME));
    requestTestFixture
        .getRequest(ALTERNATE_USERNAME, PASSWORD, "/user/login")
        .andExpect(status().isOk());

    // update user2 using email from user1
    testUser2.setEmail(testUser.getEmail());
    ResultActions actions =
        requestTestFixture
            .putSignedRequest(IT_APP_KEY, testUser2, "/admin/user/" + ALTERNATE_USERNAME)
            .andExpect(status().isUnprocessableEntity());

    UserModelMutationResult actualUserModelMutationResult =
        requestTestFixture.extractJsonResponse(actions, UserModelMutationResult.class);

    assertEquals(ModelMutationError.EMAIL_ALREADY_IN_USE, actualUserModelMutationResult.getError());

    testUser2.setEmail("12345@mail.com");
    requestTestFixture
        .putSignedRequest(IT_APP_KEY, testUser2, "/admin/user/" + ALTERNATE_USERNAME)
        .andExpect(status().isNoContent());

    // email can't be updated directly
    updatedUser = userTestFixture.getUser(testUser.getUserName());
    assertEquals(testUser.getEmail(), updatedUser.getEmail());
  }

  @Test
  public void testUserEditorRights() throws Exception {
    // Create a first admin user; this can't be done through the API
    userTestFixture.prepareAdminUser();
    userTestFixture.prepareUser();
    UUID key = UUID.randomUUID();

    // Admin add right
    requestTestFixture
        .postSignedRequestPlainText(TEST_ADMIN, key, "/admin/user/" + USERNAME + "/editorRight")
        .andExpect(status().isCreated());

    // Admin see rights
    requestTestFixture
        .getSignedRequest(TEST_ADMIN, "/admin/user/" + USERNAME + "/editorRight")
        .andExpect(status().isOk());

    // See own rights
    requestTestFixture
        .getSignedRequest(USERNAME, "/admin/user/" + USERNAME + "/editorRight")
        .andExpect(status().isOk());

    // Test search by rights on entity
    ResultActions rightsSearchResult =
        requestTestFixture
            .getSignedRequest(
                TEST_ADMIN,
                "/admin/user/search",
                ImmutableMap.<String, String>builder()
                    .put("editorRightsOn", key.toString())
                    .put("role", UserRoles.USER_ROLE)
                    .put("q", USERNAME)
                    .build())
            .andExpect(status().isOk());
    PagingResponse<UserAdminView> editorUsers =
        requestTestFixture.extractJsonResponse(
            rightsSearchResult, new TypeReference<PagingResponse<UserAdminView>>() {});
    assertTrue(editorUsers.getCount() == 1);

    // Admin delete right
    requestTestFixture
        .deleteSignedRequest(TEST_ADMIN, "/admin/user/" + USERNAME + "/editorRight/" + key)
        .andExpect(status().isNoContent());
  }

  @Test
  public void testUserEditorRightsErrors() throws Exception {
    // Create a first admin user; this can't be done through the API
    userTestFixture.prepareAdminUser();
    userTestFixture.prepareUser();
    UUID key = UUID.randomUUID();

    // User doesn't exist
    requestTestFixture
        .postSignedRequestPlainText(TEST_ADMIN, key, "/admin/user/someOtherUser/editorRight")
        .andExpect(status().isNotFound());

    // Not an admin user
    requestTestFixture
        .postSignedRequestPlainText(USERNAME, key, "/admin/user/" + USERNAME + "/editorRight")
        .andExpect(status().isForbidden());

    // Right already exists
    requestTestFixture
        .postSignedRequestPlainText(TEST_ADMIN, key, "/admin/user/" + USERNAME + "/editorRight")
        .andExpect(status().isCreated());
    requestTestFixture
        .postSignedRequestPlainText(TEST_ADMIN, key, "/admin/user/" + USERNAME + "/editorRight")
        .andExpect(status().isConflict());

    // Right doesn't exist
    UUID randomKey = UUID.randomUUID();
    requestTestFixture
        .deleteSignedRequest(TEST_ADMIN, "/admin/user/" + USERNAME + "/editorRight/" + randomKey)
        .andExpect(status().isNotFound());
  }

  @Test
  public void testChangeEmail() throws Exception {
    GbifUser testUser = userTestFixture.prepareUser();

    GbifUser updatedUser = new GbifUser(testUser);
    updatedUser.setEmail(ALTERNATIVE_EMAIL);

    // perform user update including email
    requestTestFixture
        .putSignedRequest(IT_APP_KEY, updatedUser, "/admin/user/" + USERNAME)
        .andExpect(status().isNoContent());

    // ensure email was NOT updated (shall be confirmed first)
    updatedUser = userTestFixture.getUser(testUser.getUserName());
    assertEquals(testUser.getEmail(), updatedUser.getEmail());

    // perform email change using a arbitrary wrong confirmation key
    EmailChangeRequest request = new EmailChangeRequest();
    request.setEmail(ALTERNATIVE_EMAIL);
    request.setChallengeCode(UUID.randomUUID());
    requestTestFixture
        .putSignedRequest(USERNAME, request, "/admin/user/changeEmail")
        .andExpect(status().isUnprocessableEntity());

    // extract a valid confirmation key
    UUID confirmationKey = identitySuretyTestHelper.getChallengeCode(updatedUser.getKey());
    assertNotNull(confirmationKey, "challengeCode shall exist");

    // perform email change using a current email (the same)
    request = new EmailChangeRequest();
    request.setEmail(testUser.getEmail());
    request.setChallengeCode(confirmationKey);
    requestTestFixture
        .putSignedRequest(USERNAME, request, "/admin/user/changeEmail")
        .andExpect(status().isUnprocessableEntity());

    // perform email change correctly
    request = new EmailChangeRequest();
    request.setEmail(ALTERNATIVE_EMAIL);
    request.setChallengeCode(confirmationKey);
    requestTestFixture
        .putSignedRequest(USERNAME, request, "/admin/user/changeEmail")
        .andExpect(status().isNoContent());

    // ensure email was updated
    updatedUser = userTestFixture.getUser(testUser.getUserName());
    assertEquals(ALTERNATIVE_EMAIL, updatedUser.getEmail());

    // try to login using the old email
    requestTestFixture
        .getRequest(testUser.getEmail(), PASSWORD, "/user/login")
        .andExpect(status().isUnauthorized());

    // try with the new email
    requestTestFixture
        .getRequest(ALTERNATIVE_EMAIL, PASSWORD, "/user/login")
        .andExpect(status().isOk());
  }

  @Test
  public void testUserNamespaceRights() throws Exception {
    // Create a first admin user; this can't be done through the API
    userTestFixture.prepareAdminUser();
    userTestFixture.prepareUser();
    String namespace = "ns.test.com";

    // Admin add right
    requestTestFixture
        .postSignedRequestPlainText(
            TEST_ADMIN, namespace, "/admin/user/" + USERNAME + "/namespaceRight")
        .andExpect(status().isCreated());

    // Admin see rights
    requestTestFixture
        .getSignedRequest(TEST_ADMIN, "/admin/user/" + USERNAME + "/namespaceRight")
        .andExpect(status().isOk());

    // See own rights
    requestTestFixture
        .getSignedRequest(USERNAME, "/admin/user/" + USERNAME + "/namespaceRight")
        .andExpect(status().isOk());

    // Test search by rights on entity
    ResultActions rightsSearchResult =
        requestTestFixture
            .getSignedRequest(
                TEST_ADMIN,
                "/admin/user/search",
                ImmutableMap.<String, String>builder()
                    .put("namespaceRightsOn", namespace)
                    .put("role", UserRoles.USER_ROLE)
                    .put("q", USERNAME)
                    .build())
            .andExpect(status().isOk());
    PagingResponse<UserAdminView> editorUsers =
        requestTestFixture.extractJsonResponse(
            rightsSearchResult, new TypeReference<PagingResponse<UserAdminView>>() {});
    assertTrue(editorUsers.getCount() == 1);

    // Admin delete right
    requestTestFixture
        .deleteSignedRequest(TEST_ADMIN, "/admin/user/" + USERNAME + "/namespaceRight/" + namespace)
        .andExpect(status().isNoContent());
  }

  @Test
  public void testUserNamespaceRightsErrors() throws Exception {
    // Create a first admin user; this can't be done through the API
    userTestFixture.prepareAdminUser();
    userTestFixture.prepareUser();
    String namespace = "ns2.test.com";

    // User doesn't exist
    requestTestFixture
        .postSignedRequestPlainText(
            TEST_ADMIN, namespace, "/admin/user/someOtherUser/namespaceRight")
        .andExpect(status().isNotFound());

    // Not an admin user
    requestTestFixture
        .postSignedRequestPlainText(
            USERNAME, namespace, "/admin/user/" + USERNAME + "/namespaceRight")
        .andExpect(status().isForbidden());

    // Right already exists
    requestTestFixture
        .postSignedRequestPlainText(
            TEST_ADMIN, namespace, "/admin/user/" + USERNAME + "/namespaceRight")
        .andExpect(status().isCreated());
    requestTestFixture
        .postSignedRequestPlainText(
            TEST_ADMIN, namespace, "/admin/user/" + USERNAME + "/namespaceRight")
        .andExpect(status().isConflict());

    // Right doesn't exist
    String randomNamespace = "foo";
    requestTestFixture
        .deleteSignedRequest(
            TEST_ADMIN, "/admin/user/" + USERNAME + "/namespaceRight/" + randomNamespace)
        .andExpect(status().isNotFound());
  }

  @Test
  public void testUserCountryRights() throws Exception {
    // Create a first admin user; this can't be done through the API
    userTestFixture.prepareAdminUser();
    userTestFixture.prepareUser();
    String country = Country.SPAIN.getIso2LetterCode();

    // Admin add right
    requestTestFixture
        .postSignedRequestPlainText(
            TEST_ADMIN, country, "/admin/user/" + USERNAME + "/countryRight")
        .andExpect(status().isCreated());

    // Admin see rights
    requestTestFixture
        .getSignedRequest(TEST_ADMIN, "/admin/user/" + USERNAME + "/countryRight")
        .andExpect(status().isOk());

    // See own rights
    requestTestFixture
        .getSignedRequest(USERNAME, "/admin/user/" + USERNAME + "/countryRight")
        .andExpect(status().isOk());

    // Test search by rights on entity
    ResultActions rightsSearchResult =
        requestTestFixture
            .getSignedRequest(
                TEST_ADMIN,
                "/admin/user/search",
                ImmutableMap.<String, String>builder()
                    .put("countryRightsOn", country)
                    .put("role", UserRoles.USER_ROLE)
                    .put("q", USERNAME)
                    .build())
            .andExpect(status().isOk());
    PagingResponse<UserAdminView> editorUsers =
        requestTestFixture.extractJsonResponse(
            rightsSearchResult, new TypeReference<PagingResponse<UserAdminView>>() {});
    assertTrue(editorUsers.getCount() == 1);

    // Admin delete right
    requestTestFixture
        .deleteSignedRequest(TEST_ADMIN, "/admin/user/" + USERNAME + "/countryRight/" + country)
        .andExpect(status().isNoContent());
  }

  @Test
  public void testUserCountryRightsErrors() throws Exception {
    // Create a first admin user; this can't be done through the API
    userTestFixture.prepareAdminUser();
    userTestFixture.prepareUser();
    String country = Country.AFGHANISTAN.getIso2LetterCode();

    // User doesn't exist
    requestTestFixture
        .postSignedRequestPlainText(TEST_ADMIN, country, "/admin/user/someOtherUser/countryRight")
        .andExpect(status().isNotFound());

    // Not an admin user
    requestTestFixture
        .postSignedRequestPlainText(USERNAME, country, "/admin/user/" + USERNAME + "/countryRight")
        .andExpect(status().isForbidden());

    // Right already exists
    requestTestFixture
        .postSignedRequestPlainText(
            TEST_ADMIN, country, "/admin/user/" + USERNAME + "/countryRight")
        .andExpect(status().isCreated());
    requestTestFixture
        .postSignedRequestPlainText(
            TEST_ADMIN, country, "/admin/user/" + USERNAME + "/countryRight")
        .andExpect(status().isConflict());

    // Right doesn't exist
    String randomCountry = "FO";
    requestTestFixture
        .deleteSignedRequest(
            TEST_ADMIN, "/admin/user/" + USERNAME + "/countryRight/" + randomCountry)
        .andExpect(status().isNotFound());
  }

    @Test
  public void testUserDeletionCleansUpRights() throws Exception {
    // Create a first admin user; this can't be done through the API
    userTestFixture.prepareAdminUser();
    userTestFixture.prepareUser();
    
    UUID testKey = UUID.randomUUID();
    String testNamespace = "test.namespace";
    String testCountry = Country.SPAIN.getIso2LetterCode();

    // Admin add rights to the user
    requestTestFixture
        .postSignedRequestPlainText(TEST_ADMIN, testKey, "/admin/user/" + USERNAME + "/editorRight")
        .andExpect(status().isCreated());

    requestTestFixture
        .postSignedRequestPlainText(TEST_ADMIN, testNamespace, "/admin/user/" + USERNAME + "/namespaceRight")
        .andExpect(status().isCreated());

    requestTestFixture
        .postSignedRequestPlainText(TEST_ADMIN, testCountry, "/admin/user/" + USERNAME + "/countryRight")
        .andExpect(status().isCreated());

    // Verify rights exist
    requestTestFixture
        .getSignedRequest(TEST_ADMIN, "/admin/user/" + USERNAME + "/editorRight")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0]").value(testKey.toString()));

    requestTestFixture
        .getSignedRequest(TEST_ADMIN, "/admin/user/" + USERNAME + "/namespaceRight")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0]").value(testNamespace));

    requestTestFixture
        .getSignedRequest(TEST_ADMIN, "/admin/user/" + USERNAME + "/countryRight")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0]").value(testCountry));

    // Delete the user
    requestTestFixture
        .deleteSignedRequest(TEST_ADMIN, "/admin/user/" + USERNAME)
        .andExpect(status().isNoContent());

    // Create a new user with the same username but different email
    UserCreation newUser = new UserCreation();
    newUser.setUserName(USERNAME);
    newUser.setEmail("different.email@example.com");
    newUser.setPassword(PASSWORD);
    newUser.setFirstName("New");
    newUser.setLastName("User");

    requestTestFixture
        .postSignedRequest(IT_APP_KEY, newUser, "/admin/user")
        .andExpect(status().isCreated());

    // Confirm the new user
    ConfirmationKeyParameter params = new ConfirmationKeyParameter();
    params.setConfirmationKey(userTestFixture.getUserChallengeCode(USERNAME));
    requestTestFixture
        .postSignedRequest(USERNAME, params, "/admin/user/confirm")
        .andExpect(status().isCreated());

    // Verify that the new user has NO rights (rights were cleaned up)
    requestTestFixture
        .getSignedRequest(TEST_ADMIN, "/admin/user/" + USERNAME + "/editorRight")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));

    requestTestFixture
        .getSignedRequest(TEST_ADMIN, "/admin/user/" + USERNAME + "/namespaceRight")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));

    requestTestFixture
        .getSignedRequest(TEST_ADMIN, "/admin/user/" + USERNAME + "/countryRight")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }
}
