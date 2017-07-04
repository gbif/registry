package org.gbif.registry;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityService;
import org.gbif.identity.mybatis.IdentitySuretyTestHelper;
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
  private IdentitySuretyTestHelper identitySuretyTestHelper;

  private UserTestFixture userTestFixture;

  public IdentityIT() {
    final Injector injector = RegistryTestModules.identityMybatis();

    identityService = injector.getInstance(IdentityService.class);
    identitySuretyTestHelper = injector.getInstance(IdentitySuretyTestHelper.class);

    testClient = new TestClient(wsBaseUrl);
    userTestFixture = new UserTestFixture(identityService, identitySuretyTestHelper);
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
    GbifUser user = userTestFixture.prepareUser();
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

}
