package org.gbif.registry.grizzly;

import org.gbif.identity.guice.IdentityTestModule;
import org.gbif.registry.TestConstants;
import org.gbif.registry.guice.TestRegistryWsServletListener;
import org.gbif.registry.ws.guice.SecurityModule;

import java.io.IOException;
import java.util.Properties;

import com.google.inject.Module;

/**
 * This Registry Server uses a nested ServletListener which uses the real IdentityService
 * see {@link InnerRegistryWsServletListener#getIdentityModule(Properties)} and a {@link SecurityModule}
 * with in-memory/predefined app keys.
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
      return new IdentityTestModule(props);
    }

    @Override
    protected Module getSecurityModule(Properties props) {
      return new SecurityModule(TestConstants.getIntegrationTestAppKeys());
    }
  }

}
