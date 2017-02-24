package org.gbif.registry.ws.security;

import org.gbif.identity.util.PasswordEncoder;

/**
 * Simple utils class to expose the API user role enumeration also as static strings required by the JSR 250 annotations.
 * Unit tests makes sure these are indeed the same.
 */
public class UserRoles {
  // UserRole.REGISTRY_ADMIN.name();
  public static final String ADMIN_ROLE = "REGISTRY_ADMIN";
  // UserRole.REGISTRY_EDITOR.name();
  public static final String EDITOR_ROLE = "REGISTRY_EDITOR";


  public static final String USER_ROLE = "USER";

  public static void main(String[] args) {
    System.out.println(new PasswordEncoder().encode("password"));
  }
}
