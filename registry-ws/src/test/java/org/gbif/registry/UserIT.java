package org.gbif.registry;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityService;
import org.gbif.identity.mybatis.IdentitySuretyTestHelper;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.ws.fixtures.TestClient;
import org.gbif.registry.ws.fixtures.TestConstants;
import org.gbif.registry.ws.fixtures.UserTestFixture;
import org.gbif.registry.ws.model.AuthenticationDataParameters;
import org.gbif.registry.ws.security.jwt.JwtConfiguration;
import org.gbif.ws.security.GbifAuthService;

import java.io.IOException;
import java.util.function.Function;
import javax.ws.rs.core.Response;

import com.google.inject.Injector;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.assertj.core.util.Strings;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import static org.gbif.registry.ws.util.AssertHttpResponse.assertResponse;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests related to the user (identity) module representing actions a user can initiate by himself.
 *
 * Due to the fact that login and changePassword are not directly available in the Java ws client,
 * most of the tests use a direct HTTP client.
 */
public class UserIT extends PlainAPIBaseIT {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private TestClient testClient;
  private IdentityService identityService;
  private IdentitySuretyTestHelper identitySuretyTestHelper;

  private GbifAuthService gbifAuthService = GbifAuthService.singleKeyAuthService(
          TestConstants.IT_APP_KEY, TestConstants.IT_APP_SECRET);

  private static final Function<WebResource, WebResource> LOGIN_RESOURCE_FCT = (wr) -> wr.path("login");

  private UserTestFixture userTestFixture;

  public UserIT() {
    final Injector injector = RegistryTestModules.identityMybatis();

    identityService = injector.getInstance(IdentityService.class);
    identitySuretyTestHelper = injector.getInstance(IdentitySuretyTestHelper.class);

    testClient = new TestClient(wsBaseUrl);
    userTestFixture = new UserTestFixture(identityService, identitySuretyTestHelper);
  }

  /**
   * Only used to make sure we can NOT use the appkey
   * @return
   */
  @Override
  protected GbifAuthService getAuthService() {
    return gbifAuthService;
  }

  @Override
  protected String getResourcePath() {
    return UserTestFixture.USER_RESOURCE_PATH;
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

  @Test
  public void testLoginNoCredentials() {
    // GET login
    ClientResponse cr = getPublicClient().get(LOGIN_RESOURCE_FCT);
    assertResponse(Response.Status.UNAUTHORIZED, cr);

    // POST login
    cr = getPublicClient().post(LOGIN_RESOURCE_FCT, null);
    assertResponse(Response.Status.UNAUTHORIZED, cr);
  }

  @Test
  public void testLoginGet() throws IOException {
    GbifUser user = userTestFixture.prepareUser();
    ClientResponse cr = getAuthenticatedClient().get(LOGIN_RESOURCE_FCT);
    assertResponse(Response.Status.OK, cr);

    // check jwt token
    String body = cr.getEntity(String.class);
    String token = OBJECT_MAPPER.readTree(body).get(JwtConfiguration.TOKEN_FIELD_RESPONSE).asText();
    assertTrue(!Strings.isNullOrEmpty(token));

    //try to login using the email instead of the username
    cr = testClient.login(user.getEmail(), getPassword());
    assertResponse(Response.Status.OK, cr);
  }

  @Test
  public void testLoginPost() throws IOException {
    GbifUser user = userTestFixture.prepareUser();
    ClientResponse cr = getAuthenticatedClient().post(LOGIN_RESOURCE_FCT, null);
    assertResponse(Response.Status.CREATED, cr);

    // check jwt token
    String body = cr.getEntity(String.class);
    String token = OBJECT_MAPPER.readTree(body).get(JwtConfiguration.TOKEN_FIELD_RESPONSE).asText();
    assertTrue(!Strings.isNullOrEmpty(token));

    //try to login using the email instead of the username
    cr = testClient.loginPost(user.getEmail(), getPassword());
    assertResponse(Response.Status.CREATED, cr);
  }

  @Test
  public void testChangePassword() {
    userTestFixture.prepareUser();

    final String newPassword = "123456";
    AuthenticationDataParameters params = new AuthenticationDataParameters();
    params.setPassword(newPassword);
    ClientResponse cr = getAuthenticatedClient()
            .put(uri -> uri.path("changePassword"), params);
    assertResponse(Response.Status.NO_CONTENT, cr);

    //try to login using the previous password
    cr = getAuthenticatedClient().get(LOGIN_RESOURCE_FCT);
    assertResponse(Response.Status.UNAUTHORIZED, cr);

    //try with the new password
    cr = testClient.login(UserTestFixture.USERNAME, newPassword);
    assertResponse(Response.Status.OK, cr);
  }

  /**
   * The login endpoint only accepts HTTP Basic request.
   * Application that uses appkeys are trusted.
   */
  @Test
  public void testLoginWithAppKeys() {
    GbifUser user = userTestFixture.prepareUser();
    ClientResponse cr = getWithSignedRequest(user.getUserName(), (uriBuilder -> uriBuilder.path("login")));
    assertResponse(Response.Status.FORBIDDEN, cr);
  }

}
