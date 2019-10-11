package org.gbif.ws.security;

import org.springframework.stereotype.Component;

// TODO: 11/10/2019 move to the registry-ws or registry-client?
@Component
public class RegistryWsClientITAppKeyProvider implements AppKeyProvider {

  // TODO: 11/10/2019 implement this. Should be more providers for the rest of apps in the future.
  // todo Must be configurable (local, dev, uat, prod etc)
  public String get() {
    return "gbif.registry-ws-client-it";
  }
}
