package org.gbif.identity.guice;

import org.gbif.identity.email.IdentityEmailManager;
import org.gbif.identity.email.IdentityEmailManagerMock;

import java.util.Properties;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;

/**
 * Wrapper of {@link InternalIdentityMyBatisModule} for testing purpose.
 * The main target is to bind {@link IdentityEmailManager} to a mock object
 */
public class IdentityTestModule implements Module {

  private final Properties properties;
  public IdentityTestModule(Properties properties) {
    this.properties = properties;
  }

  @Override
  public void configure(Binder binder) {
    IdentityServiceModule mod = new IdentityServiceModule(properties);

    binder.install(mod);
    binder.bind(IdentityEmailManager.class).to(IdentityEmailManagerMock.class).in(Scopes.SINGLETON);
  }
}
