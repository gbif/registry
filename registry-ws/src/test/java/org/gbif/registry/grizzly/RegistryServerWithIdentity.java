package org.gbif.registry.grizzly;

import org.gbif.identity.inject.IdentityServiceTestModule;
import org.gbif.registry.guice.TestRegistryWsServletListener;
import org.gbif.registry.ws.fixtures.TestConstants;
import org.gbif.registry.ws.guice.SecurityModule;

import java.io.IOException;
import java.util.Properties;

import com.google.inject.Module;

import static org.gbif.ws.server.filter.AppIdentityFilter.APPKEYS_WHITELIST;

/**
 * This Registry Server uses a nested ServletListener which uses the real IdentityService
 * see {@link InnerRegistryWsServletListener#getIdentityModule(Properties)} and a {@link SecurityModule}
 * with in-memory/predefined app keys.
 * It accesses the test database for user authentication.
 */
public class RegistryServerWithIdentity extends AbstractRegistryServer {

  public static final RegistryServerWithIdentity INSTANCE = new RegistryServerWithIdentity();

  protected RegistryServerWithIdentity() {
    super(InnerRegistryWsServletListener.class);
  }

  @Override
  public AbstractRegistryServer getNewInstance() {
    return new RegistryServerWithIdentity();
  }

  /**
   * This nested class needs to be public to be loaded by Grizzly.
   */
  public static class InnerRegistryWsServletListener extends TestRegistryWsServletListener {

    public InnerRegistryWsServletListener() throws IOException {
    }

    @Override
    protected Module getIdentityModule(Properties props) {
      props.setProperty(APPKEYS_WHITELIST, TestConstants.IT_APP_KEY);
      return new IdentityServiceTestModule(props);
    }

    @Override
    protected Module getSecurityModule(Properties props) {
      return new SecurityModule(TestConstants.getIntegrationTestAppKeys());
    }
  }

}
