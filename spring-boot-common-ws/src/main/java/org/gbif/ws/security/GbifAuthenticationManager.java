package org.gbif.ws.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;

public interface GbifAuthenticationManager extends AuthenticationManager {

  Authentication authenticate(HttpServletRequest request);
}
