package org.gbif.ws.security;

import org.springframework.security.core.Authentication;

public interface GbifAuthentication extends Authentication {

  String getAuthenticationScheme();
}
