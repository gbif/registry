/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.ws.it.security;

import org.gbif.api.vocabulary.AppRole;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.security.UserRoles;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
