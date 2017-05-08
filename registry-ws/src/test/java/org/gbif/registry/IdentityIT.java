package org.gbif.registry;

import org.gbif.api.model.common.User;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.service.common.UserSession;
import org.gbif.identity.mybatis.UserMapper;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.ws.fixtures.TestClient;
import org.gbif.registry.ws.fixtures.UserTestFixture;
import org.gbif.registry.ws.model.AuthenticationDataParameters;
import org.gbif.ws.security.GbifAuthService;

import javax.ws.rs.core.Response;

import com.google.inject.Injector;
import com.sun.jersey.api.client.ClientResponse;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * Integration tests related to the identity module.
 */
public class IdentityIT extends PlainAPIBaseIT {

  private TestClient testClient;
  private IdentityService identityService;
  private UserMapper userMapper;

  private UserTestFixture userTestFixture;

  public IdentityIT() {
    final Injector service = RegistryTestModules.identityMybatis();
    userMapper = service.getInstance(UserMapper.class);
    identityService = service.getInstance(IdentityService.class);

    testClient = new TestClient(wsBaseUrl);
    userTestFixture = new UserTestFixture(identityService, userMapper);
  }

  @Override
  protected GbifAuthService getAuthService() {
    //should not be used here
    return null;
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
    ClientResponse cr = getPublicClient().get(wr -> wr.path("login"));
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), cr.getStatus());
  }

  @Test
  public void testLogin() {
    User user = userTestFixture.prepareUser();
    ClientResponse cr = getAuthenticatedClient().get(wr -> wr.path("login"));
    assertEquals(Response.Status.OK.getStatusCode(), cr.getStatus());

    //try to login using the email instead of the username
    cr = testClient.login(user.getEmail(), getPassword());
    assertEquals(Response.Status.OK.getStatusCode(), cr.getStatus());
  }

  @Test
  public void testChangePassword() {
    userTestFixture.prepareUser();

    AuthenticationDataParameters params = new AuthenticationDataParameters();
    params.setPassword("123456");
    ClientResponse cr = getAuthenticatedClient()
            .put(uri -> uri.path("changePassword"), params);
    assertEquals(Response.Status.NO_CONTENT.getStatusCode(), cr.getStatus());

    //try to login using the previous password
    cr = getAuthenticatedClient().get(wr -> wr.path("login"));
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), cr.getStatus());

    //try with the new password
    cr = testClient.login(UserTestFixture.USERNAME, "123456");
    assertEquals(Response.Status.OK.getStatusCode(), cr.getStatus());
  }

  @Test
  public void testSession() {
    userTestFixture.prepareUser();
    ClientResponse cr = getAuthenticatedClient().get(wr -> wr.path("login"));
    assertEquals(Response.Status.OK.getStatusCode(), cr.getStatus());
    UserSession userSession = cr.getEntity(UserSession.class);

    //use the session to issue a simple get on the user
    cr = getPublicClient().getWithSessionToken(userSession.getSession());
    assertEquals(Response.Status.OK.getStatusCode(), cr.getStatus());
    assertEquals(UserTestFixture.USERNAME, userSession.getUserName());

    //logout
    cr = getPublicClient().getWithSessionToken(userSession.getSession(), wr -> wr.path("logout"));
    assertEquals(Response.Status.NO_CONTENT.getStatusCode(), cr.getStatus());

    //try to use the token again (after logout)
    cr = getPublicClient().getWithSessionToken(userSession.getSession());
    //ideally UNAUTHORIZED would be returned
    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), cr.getStatus());
  }

}
