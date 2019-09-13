package org.gbif.registry.ws.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * Class providing temporary authorization for legacy web service requests (GBRDS/IPT).
 */
public class LegacyRequestAuthorization implements Authentication {

  private final UUID userKey;
  private final UUID organizationKey;

  public LegacyRequestAuthorization(UUID userKey, UUID organizationKey) {
    this.userKey = userKey;
    this.organizationKey = organizationKey;
  }

  public UUID getUserKey() {
    return userKey;
  }

  public UUID getOrganizationKey() {
    return organizationKey;
  }

  // TODO: 06/09/2019 test these implementations are ok
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.emptyList();
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public Object getDetails() {
    return null;
  }

  @Override
  public Object getPrincipal() {
    return null;
  }

  @Override
  public boolean isAuthenticated() {
    return true;
  }

  @Override
  public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getName() {
    return userKey.toString();
  }
}

