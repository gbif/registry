package org.gbif.ws.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Extends Spring's {@link UsernamePasswordAuthenticationToken}.
 */
public class GbifAuthentication extends UsernamePasswordAuthenticationToken {

  /**
   * User information (can be either {@link GbifUserPrincipal} or {@link AppPrincipal}).
   */
  private UserDetails principal;

  /**
   * Authentication scheme (e.g. 'GBIF').
   */
  private String authenticationScheme;

  // Take into account that a create object would have 'authenticate' false because of the constructor of the superclass.
  public GbifAuthentication(
    UserDetails principal,
    Object credentials,
    String authenticationScheme) {
    super(principal, credentials);
    this.principal = principal;
    this.authenticationScheme = authenticationScheme;
  }

  public GbifAuthentication(
    UserDetails principal,
    Object credentials,
    Collection<? extends GrantedAuthority> authorities,
    String authenticationScheme) {
    super(principal, credentials, authorities);
    this.principal = principal;
    this.authenticationScheme = authenticationScheme;
  }

  public static GbifAuthentication anonymous() {
    return new GbifAuthentication(null, null, Collections.emptyList(), "");
  }

  public String getAuthenticationScheme() {
    return authenticationScheme;
  }

  @Override
  public UserDetails getPrincipal() {
    return principal;
  }
}
