package org.gbif.registry.ws.config;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

// TODO: 2019-07-26 comments, tests
public class RegistryAuthentication extends UsernamePasswordAuthenticationToken {

  private UserDetails principal;
  private String authenticationScheme;
  // TODO: 2019-07-26 is it ok to store request?
  private HttpServletRequest request;

  // TODO: 2019-07-29 use a custom object (class or interface) instead of a spring one UserDetails
  public RegistryAuthentication(UserDetails principal, Object credentials, String authenticationScheme, HttpServletRequest request) {
    super(principal, credentials);
    this.principal = principal;
    this.authenticationScheme = authenticationScheme;
    this.request = request;
  }

  public RegistryAuthentication(UserDetails principal, Object credentials,
                                Collection<? extends GrantedAuthority> authorities, String authenticationScheme, HttpServletRequest request) {
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
  public GbifUserPrincipal getPrincipal() {
    return ((GbifUserPrincipal) principal);
  }
}
