package org.gbif.ws.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

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

  /**
   * Http request.
   */
  private HttpServletRequest request;

  // Take into account that a create object would have 'authenticate' false because of the constructor of the superclass.
  public GbifAuthentication(
      UserDetails principal,
      Object credentials,
      String authenticationScheme,
      HttpServletRequest request) {
    super(principal, credentials);
    this.principal = principal;
    this.authenticationScheme = authenticationScheme;
    this.request = request;
  }

  public GbifAuthentication(
      UserDetails principal,
      Object credentials,
      Collection<? extends GrantedAuthority> authorities,
      String authenticationScheme,
      HttpServletRequest request) {
    super(principal, credentials, authorities);
    this.principal = principal;
    this.authenticationScheme = authenticationScheme;
    this.request = request;
  }

  public String getAuthenticationScheme() {
    return authenticationScheme;
  }

  public HttpServletRequest getRequest() {
    return request;
  }

  @Override
  public UserDetails getPrincipal() {
    return principal;
  }
}
