package org.gbif.ws.security;

import org.gbif.ws.server.RequestObject;

public interface GbifAuthService {

  boolean isValidRequest(RequestObject request);

  RequestObject signRequest(String username, RequestObject request);
}
