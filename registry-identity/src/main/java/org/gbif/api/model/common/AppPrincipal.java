package org.gbif.api.model.common;

import javax.security.auth.Subject;

/**
 * A wrapper class for an application represented by an appKey
 * that exposes the unique account name as its appKey.
 */
public class AppPrincipal implements ExtendedPrincipal {

  private final String appKey;
  private final String appRole;

  public AppPrincipal(String appKey, String appRole) {
    this.appKey = appKey;
    this.appRole = appRole;
  }

  @Override
  public String getName() {
    return appKey;
  }

  @Override
  public boolean implies(Subject subject) {
    return false;
  }

  @Override
  public boolean hasRole(String role) {
    return appRole != null && appRole.equalsIgnoreCase(role);
  }
}
