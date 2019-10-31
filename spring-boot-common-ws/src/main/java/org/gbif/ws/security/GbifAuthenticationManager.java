package org.gbif.ws.security;

import org.springframework.security.authentication.AuthenticationManager;

import javax.servlet.http.HttpServletRequest;

public interface GbifAuthenticationManager extends AuthenticationManager {

  GbifAuthentication authenticate(HttpServletRequest request);
}
