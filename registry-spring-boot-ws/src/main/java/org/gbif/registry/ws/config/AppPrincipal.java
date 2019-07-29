package org.gbif.registry.ws.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Objects;

public class AppPrincipal implements UserDetails {

  @NotNull
  private final String appKey;
  private final String appRole;

  public AppPrincipal(String appKey, String appRole) {
    Objects.requireNonNull(appKey, "appKey shall be provided");
    this.appKey = appKey;
    this.appRole = appRole;
  }

  // TODO: 2019-07-29 implement methods below
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return null;
  }

  @Override
  public String getPassword() {
    return null;
  }

  @Override
  public String getUsername() {
    return null;
  }

  @Override
  public boolean isAccountNonExpired() {
    return false;
  }

  @Override
  public boolean isAccountNonLocked() {
    return false;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return false;
  }

  @Override
  public boolean isEnabled() {
    return false;
  }
}
