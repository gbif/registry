package org.gbif.registry.ws.security;

import org.gbif.api.vocabulary.AppRole;
import org.gbif.api.vocabulary.UserRole;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UserRolesTest {
  @Test
  public void assertRoleNamesAreIdentical() {
    assertEquals(UserRoles.ADMIN_ROLE, UserRole.REGISTRY_ADMIN.name());
    assertEquals(UserRoles.EDITOR_ROLE, UserRole.REGISTRY_EDITOR.name());
    assertEquals(UserRoles.GRSCICOLL_ADMIN_ROLE, UserRole.GRSCICOLL_ADMIN.name());
    assertEquals(UserRoles.GRSCICOLL_EDITOR_ROLE, UserRole.GRSCICOLL_EDITOR.name());

    assertEquals(UserRoles.APP_ROLE, AppRole.APP.name());
  }
}
