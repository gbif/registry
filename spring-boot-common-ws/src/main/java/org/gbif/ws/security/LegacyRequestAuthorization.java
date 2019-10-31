package org.gbif.ws.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * Class providing temporary authorization for legacy web service requests (GBRDS/IPT).
 */
public class LegacyRequestAuthorization implements Authentication {

  private static final Logger LOG = LoggerFactory.getLogger(LegacyRequestAuthorization.class);

  private boolean authenticated = false;
  private final UUID userKey;
  private final UUID organizationKey;

  public LegacyRequestAuthorization(UUID userKey, UUID organizationKey) {
    this.userKey = userKey;
    this.organizationKey = organizationKey;
    setAuthenticated(true);
  }

  public UUID getUserKey() {
    return userKey;
  }

  public UUID getOrganizationKey() {
    return organizationKey;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.emptyList();
  }

  @Override
  public Object getCredentials() {
    LOG.warn("LegacyRequestAuthorization#getCredentials is not used");
    return null;
  }

  @Override
  public Object getDetails() {
    LOG.warn("LegacyRequestAuthorization#getDetails is not used");
    return null;
  }

  @Override
  public Object getPrincipal() {
    return new BasicUserPrincipal(userKey.toString());
  }

  @Override
  public boolean isAuthenticated() {
    return authenticated;
  }

  @Override
  public void setAuthenticated(boolean isAuthenticated) {
    this.authenticated = isAuthenticated;
  }

  @Override
  public String getName() {
    return userKey.toString();
  }
}
