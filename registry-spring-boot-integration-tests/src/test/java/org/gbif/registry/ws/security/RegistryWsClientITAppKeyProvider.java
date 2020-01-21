package org.gbif.registry.ws.security;

import org.gbif.ws.security.AppKeyProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class RegistryWsClientITAppKeyProvider implements AppKeyProvider {

  public String get() {
    return "gbif.registry-ws-client-it";
  }
}
