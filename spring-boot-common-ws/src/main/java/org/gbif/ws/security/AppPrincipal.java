package org.gbif.ws.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * @see org.gbif.api.model.common.AppPrincipal
 */
public class AppPrincipal implements UserDetails {

  @NotNull
  private final String appKey;

  private List<? extends GrantedAuthority> authorities;

  public AppPrincipal(String appKey, List<? extends GrantedAuthority> authorities) {
    Objects.requireNonNull(appKey, "appKey shall be provided");
    this.appKey = appKey;
    this.authorities = authorities;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    throw new UnsupportedOperationException("There is no password for the AppPrincipal");
  }

  @Override
  public String getUsername() {
    return appKey;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
