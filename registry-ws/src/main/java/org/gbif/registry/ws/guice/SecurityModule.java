package org.gbif.registry.ws.guice;

import org.gbif.registry.ws.security.EditorAuthorizationService;
import org.gbif.registry.ws.security.EditorAuthorizationServiceImpl;
import org.gbif.registry.ws.security.jwt.JwtConfiguration;
import org.gbif.ws.server.guice.WsAuthModule;

import java.util.Map;
import java.util.Properties;

import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;

/**
 * Security module for the registry that extends the common GBIF authentication module with an instance of the
 * EditorAuthorizationService.
 */
public class SecurityModule extends WsAuthModule {

  private final Properties jwtProperties;

  public SecurityModule(Properties properties) {
    super(properties);
    jwtProperties = properties;
  }

  public SecurityModule(Map<String, String> keys, Properties properties) {
    super(keys);
    jwtProperties = properties;
  }

  @Override
  protected void configure() {
    super.configure();
    bind(EditorAuthorizationService.class).to(EditorAuthorizationServiceImpl.class).in(Scopes.SINGLETON);
    expose(EditorAuthorizationService.class);
    expose(JwtConfiguration.class);
  }

  @Provides
  @Singleton
  private JwtConfiguration provideJwtConfiguration() {
    return JwtConfiguration.from(jwtProperties);
  }

}
