package org.gbif.registry.ws.guice;

import org.gbif.registry.ws.security.EditorAuthorizationService;
import org.gbif.registry.ws.security.EditorAuthorizationServiceImpl;
import org.gbif.ws.server.guice.WsAuthModule;

import java.util.Map;
import java.util.Properties;

import com.google.inject.Scopes;

/**
 * Security module for the registry that extends the common GBIF authentication module with an instance of the
 * EditorAuthorizationService.
 */
public class SecurityModule extends WsAuthModule {

  public SecurityModule(Properties properties) {
    super(properties);
  }

  public SecurityModule(Map<String, String> keys) {
    super(keys);
  }

  @Override
  protected void configure() {
    super.configure();
    bind(EditorAuthorizationService.class).to(EditorAuthorizationServiceImpl.class).in(Scopes.SINGLETON);
    expose(EditorAuthorizationService.class);
  }

}
