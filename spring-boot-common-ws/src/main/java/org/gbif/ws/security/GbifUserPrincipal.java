package org.gbif.ws.security;

import org.gbif.api.model.common.GbifUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @see org.gbif.api.model.common.GbifUserPrincipal
 */
public class GbifUserPrincipal implements UserDetails {

  @NotNull
  private final GbifUser user;

  public GbifUserPrincipal(GbifUser user) {
    Objects.requireNonNull(user, "user shall be provided");
    this.user = user;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return user.getRoles()
        .stream()
        .map(p -> new SimpleGrantedAuthority(p.toString()))
        .collect(Collectors.toList());
  }

  @Override
  public String getPassword() {
    return user.getPasswordHash();
  }

  @Override
  public String getUsername() {
    return user.getUserName();
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
