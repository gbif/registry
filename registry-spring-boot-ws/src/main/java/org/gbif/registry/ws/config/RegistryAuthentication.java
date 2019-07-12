package org.gbif.registry.ws.config;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class RegistryAuthentication extends UsernamePasswordAuthenticationToken {

  // TODO: 2019-07-11 mb add some more fields?
  private String authenticationScheme;

  public RegistryAuthentication(Object principal, Object credentials, String authenticationScheme) {
    super(principal, credentials);
    this.authenticationScheme = authenticationScheme;
  }

  public RegistryAuthentication(Object principal, Object credentials,
                                Collection<? extends GrantedAuthority> authorities, String authenticationScheme) {
    super(principal, credentials, authorities);
    this.authenticationScheme = authenticationScheme;
  }

  public RegistryAuthentication(Authentication authentication, String authenticationScheme) {
    this(authentication.getPrincipal(), authentication.getCredentials(), authentication.getAuthorities(), authenticationScheme);
  }

  public String getAuthenticationScheme() {
    return authenticationScheme;
  }
}
